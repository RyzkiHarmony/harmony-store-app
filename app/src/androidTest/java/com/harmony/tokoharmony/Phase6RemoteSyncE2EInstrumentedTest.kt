package com.harmony.tokoharmony

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.harmony.tokoharmony.core.database.AppDatabase
import com.harmony.tokoharmony.core.database.entity.CategoryEntity
import com.harmony.tokoharmony.core.database.entity.PriceHistoryEntity
import com.harmony.tokoharmony.core.database.entity.ProductEntity
import com.harmony.tokoharmony.core.database.entity.StockMovementEntity
import com.harmony.tokoharmony.core.database.entity.SyncQueueEntity
import com.harmony.tokoharmony.core.database.entity.TransactionEntity
import com.harmony.tokoharmony.core.database.entity.TransactionItemEntity
import com.harmony.tokoharmony.core.database.entity.UserEntity
import com.harmony.tokoharmony.core.network.HttpSyncClient
import com.harmony.tokoharmony.core.network.SyncConfig
import com.harmony.tokoharmony.core.network.SyncJsonMapper
import com.harmony.tokoharmony.data.repository.SyncRepositoryImpl
import com.harmony.tokoharmony.domain.model.MovementType
import com.harmony.tokoharmony.domain.model.PaymentMethod
import com.harmony.tokoharmony.domain.model.PaymentStatus
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.ReferenceType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.SyncEntityType
import com.harmony.tokoharmony.domain.model.SyncOperation
import com.harmony.tokoharmony.domain.model.SyncQueueItem
import com.harmony.tokoharmony.domain.model.SyncStatus
import com.harmony.tokoharmony.domain.model.TransactionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Phase6RemoteSyncE2EInstrumentedTest {

    private lateinit var context: Context
    private lateinit var syncConfig: SyncConfig
    private lateinit var httpSyncClient: HttpSyncClient
    private lateinit var db: AppDatabase
    private lateinit var syncRepository: SyncRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        syncConfig = SyncConfig(context).apply {
            val args = InstrumentationRegistry.getArguments()
            val customUrl = args.getString("SYNC_ENDPOINT_URL")?.takeIf { it.isNotBlank() }
            val customToken = args.getString("SYNC_TOKEN")?.takeIf { it.isNotBlank() }
            if (customUrl != null) endpointUrl = customUrl
            if (customToken != null) syncToken = customToken
        }
        assumeTrue(
            "Remote sync endpoint URL must be configured (via device Settings or -e SYNC_ENDPOINT_URL)",
            syncConfig.isConfigured()
        )
        httpSyncClient = HttpSyncClient(syncConfig)

        // Create isolated test database on real device
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        syncRepository = SyncRepositoryImpl(
            appDatabase = db,
            syncQueueDao = db.syncQueueDao(),
            httpSyncClient = httpSyncClient,
            syncConfig = syncConfig,
            userDao = db.userDao(),
            categoryDao = db.categoryDao(),
            productDao = db.productDao(),
            priceHistoryDao = db.priceHistoryDao(),
            transactionDao = db.transactionDao(),
            transactionItemDao = db.transactionItemDao(),
            digitalTransactionDao = db.digitalTransactionDao(),
            stockInDao = db.stockInDao(),
            stockAdjustmentDao = db.stockAdjustmentDao(),
            stockMovementDao = db.stockMovementDao()
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun test01_verifyRemoteConnectivity() = runBlocking {
        assertTrue("SyncConfig endpointUrl must be configured", syncConfig.isConfigured())
        assertTrue("SyncConfig syncToken must be configured", syncConfig.syncToken.isNotBlank())

        val pingUrl = "${syncConfig.endpointUrl}?action=ping&token=${syncConfig.syncToken}"
        val response = httpSyncClient.getJson(pingUrl)

        assertTrue("Ping to Google Apps Script Web App must be successful. Status: ${response.statusCode}", response.isSuccessful)
        assertTrue("Ping response body must contain SUCCESS", response.body.contains("SUCCESS"))
        assertTrue("Ping response body must indicate gateway is running", response.body.contains("Toko Harmony POS Sync Gateway is running"))
    }

    @Test
    fun test02_createE2E_and_idempotency_and_update() = runBlocking {
        val uniqueSuffix = System.currentTimeMillis().toString().takeLast(6)
        val catId = "cat-e2e-$uniqueSuffix"
        val prodId = "prod-e2e-$uniqueSuffix"

        // 1. Create Category and Product locally
        val categoryPayload = "{\"categoryId\":\"$catId\",\"name\":\"E2E Category $uniqueSuffix\",\"isActive\":true,\"createdAt\":1700000000000,\"updatedAt\":1700000000000}"
        val productPayload = "{\"productId\":\"$prodId\",\"categoryId\":\"$catId\",\"name\":\"E2E Product $uniqueSuffix\",\"barcode\":\"899$uniqueSuffix\",\"productKind\":\"PHYSICAL\",\"pricingMethod\":\"PER_UNIT\",\"quantityType\":\"COUNT\",\"sellingUnit\":\"pcs\",\"stockUnit\":\"pcs\",\"purchaseUnit\":null,\"purchaseConversionFactor\":null,\"currentPrice\":50000,\"minimumStock\":5,\"isActive\":true,\"createdAt\":1700000000000,\"updatedAt\":1700000000000}"

        val q1 = SyncQueueItem(
            queueId = "q-cat-$uniqueSuffix",
            entityType = SyncEntityType.CATEGORY,
            entityId = catId,
            operation = SyncOperation.CREATE,
            payload = categoryPayload
        )
        val q2 = SyncQueueItem(
            queueId = "q-prod-$uniqueSuffix",
            entityType = SyncEntityType.PRODUCT,
            entityId = prodId,
            operation = SyncOperation.CREATE,
            payload = productPayload
        )

        syncRepository.enqueue(q1)
        syncRepository.enqueue(q2)

        val pendingBefore = db.syncQueueDao().getPendingItems(10)
        assertEquals(2, pendingBefore.size)

        // 2. Perform Batch Sync to Google Sheets
        val syncResult = syncRepository.syncPendingBatch(limit = 10)
        assertEquals(2, syncResult.totalProcessed)
        assertEquals(2, syncResult.successCount)
        assertEquals(0, syncResult.failureCount)

        // Verify SyncQueue updated to SYNCED
        val pendingAfter = db.syncQueueDao().getPendingItems(10)
        assertEquals(0, pendingAfter.size)

        // 3. Verify in Google Sheets via Remote Bootstrap
        val bootstrapResult1 = syncRepository.fetchBootstrapData()
        assertTrue("Bootstrap fetch must succeed", bootstrapResult1.isSuccess)
        val remoteData1 = bootstrapResult1.getOrThrow()

        val remoteProd1 = remoteData1.products.find { it.productId == prodId }
        assertNotNull("Product must exist in Google Sheets", remoteProd1)
        assertEquals("E2E Product $uniqueSuffix", remoteProd1?.name)
        assertEquals(50000L, remoteProd1?.currentPrice)

        // Count occurrences in remote products to verify exactly 1 row
        val countProd1 = remoteData1.products.count { it.productId == prodId }
        assertEquals("Exactly one row must exist remotely for CREATE", 1, countProd1)

        // 4. Test IDEMPOTENCY: Re-send the exact same CREATE operations
        val q1Retry = q1.copy(queueId = "q-cat-retry-$uniqueSuffix")
        val q2Retry = q2.copy(queueId = "q-prod-retry-$uniqueSuffix")
        syncRepository.enqueue(q1Retry)
        syncRepository.enqueue(q2Retry)

        val retryResult = syncRepository.syncPendingBatch(limit = 10)
        assertEquals(2, retryResult.successCount)

        val bootstrapResult2 = syncRepository.fetchBootstrapData()
        val remoteData2 = bootstrapResult2.getOrThrow()
        val countProd2 = remoteData2.products.count { it.productId == prodId }
        assertEquals("Resending duplicate CREATE must NOT create duplicate row in Google Sheets", 1, countProd2)

        // 5. Test UPDATE: Modify entity and sync UPDATE
        val updatedPayload = "{\"productId\":\"$prodId\",\"categoryId\":\"$catId\",\"name\":\"E2E Product Updated $uniqueSuffix\",\"barcode\":\"899$uniqueSuffix\",\"productKind\":\"PHYSICAL\",\"pricingMethod\":\"PER_UNIT\",\"quantityType\":\"COUNT\",\"sellingUnit\":\"pcs\",\"stockUnit\":\"pcs\",\"purchaseUnit\":null,\"purchaseConversionFactor\":null,\"currentPrice\":75000,\"minimumStock\":10,\"isActive\":true,\"createdAt\":1700000000000,\"updatedAt\":1700000050000}"
        val qUpdate = SyncQueueItem(
            queueId = "q-prod-upd-$uniqueSuffix",
            entityType = SyncEntityType.PRODUCT,
            entityId = prodId,
            operation = SyncOperation.UPDATE,
            payload = updatedPayload
        )
        syncRepository.enqueue(qUpdate)

        val updateResult = syncRepository.syncPendingBatch(limit = 10)
        assertEquals(1, updateResult.successCount)

        val bootstrapResult3 = syncRepository.fetchBootstrapData()
        val remoteData3 = bootstrapResult3.getOrThrow()
        val remoteProdUpdated = remoteData3.products.find { it.productId == prodId }
        assertNotNull("Updated product must exist", remoteProdUpdated)
        assertEquals("E2E Product Updated $uniqueSuffix", remoteProdUpdated?.name)
        assertEquals(75000L, remoteProdUpdated?.currentPrice)

        val countProdUpdated = remoteData3.products.count { it.productId == prodId }
        assertEquals("UPDATE must not create a duplicate row", 1, countProdUpdated)
    }

    @Test
    fun test03_historicalEntities_idempotency() = runBlocking {
        val uniqueSuffix = System.currentTimeMillis().toString().takeLast(6)
        val histId = "hist-e2e-$uniqueSuffix"
        val movId = "mov-e2e-$uniqueSuffix"
        val itemId = "item-e2e-$uniqueSuffix"

        val phPayload = "{\"historyId\":\"$histId\",\"productId\":\"prod-$uniqueSuffix\",\"oldPrice\":50000,\"newPrice\":75000,\"changedAt\":1700000000000,\"changedBy\":\"admin-test\"}"
        val movPayload = "{\"movementId\":\"$movId\",\"productId\":\"prod-$uniqueSuffix\",\"movementType\":\"STOCK_IN\",\"quantityDelta\":10,\"referenceType\":null,\"referenceId\":null,\"reason\":\"Restock E2E\",\"createdAt\":1700000000000,\"createdBy\":\"admin-test\"}"
        val itemPayload = "{\"itemId\":\"$itemId\",\"transactionId\":\"tx-$uniqueSuffix\",\"productId\":\"prod-$uniqueSuffix\",\"productNameSnapshot\":\"Snapshot E2E\",\"quantity\":2,\"unitPrice\":75000,\"subtotal\":150000,\"sellingUnit\":\"pcs\"}"

        syncRepository.enqueue(SyncQueueItem("q-ph-$uniqueSuffix", SyncEntityType.PRICE_HISTORY, histId, SyncOperation.CREATE, phPayload))
        syncRepository.enqueue(SyncQueueItem("q-mov-$uniqueSuffix", SyncEntityType.STOCK_MOVEMENT, movId, SyncOperation.CREATE, movPayload))
        syncRepository.enqueue(SyncQueueItem("q-item-$uniqueSuffix", SyncEntityType.TRANSACTION_ITEM, itemId, SyncOperation.CREATE, itemPayload))

        val syncRes = syncRepository.syncPendingBatch(limit = 10)
        assertEquals(3, syncRes.successCount)

        // Resend duplicate historical entities
        syncRepository.enqueue(SyncQueueItem("q-ph-dup-$uniqueSuffix", SyncEntityType.PRICE_HISTORY, histId, SyncOperation.CREATE, phPayload))
        syncRepository.enqueue(SyncQueueItem("q-mov-dup-$uniqueSuffix", SyncEntityType.STOCK_MOVEMENT, movId, SyncOperation.CREATE, movPayload))
        syncRepository.enqueue(SyncQueueItem("q-item-dup-$uniqueSuffix", SyncEntityType.TRANSACTION_ITEM, itemId, SyncOperation.CREATE, itemPayload))

        val dupRes = syncRepository.syncPendingBatch(limit = 10)
        assertEquals(3, dupRes.successCount)

        // Verify remote counts
        val remoteData = syncRepository.fetchBootstrapData().getOrThrow()
        assertEquals(1, remoteData.priceHistories.count { it.historyId == histId })
        assertEquals(1, remoteData.stockMovements.count { it.movementId == movId })
        assertEquals(1, remoteData.transactionItems.count { it.itemId == itemId })
    }

    @Test
    fun test04_transaction_completion_and_cancellation_e2e() = runBlocking {
        val uniqueSuffix = System.currentTimeMillis().toString().takeLast(6)
        val txId = "tx-e2e-$uniqueSuffix"
        val itemId = "item-tx-$uniqueSuffix"
        val saleMovId = "mov-sale-$uniqueSuffix"
        val reversalMovId = "mov-rev-$uniqueSuffix"

        // 1. Transaction COMPLETED sync
        val txPayload = "{\"transactionId\":\"$txId\",\"transactionNumber\":\"TRX-$uniqueSuffix\",\"transactionStatus\":\"COMPLETED\",\"paymentStatus\":\"PAID\",\"paymentMethod\":\"CASH\",\"totalAmount\":100000,\"amountReceived\":100000,\"changeAmount\":0,\"createdAt\":1700000000000,\"completedAt\":1700000000000,\"cancelledAt\":null,\"createdBy\":\"cashier-1\",\"cancelledBy\":null,\"cancellationReason\":null}"
        val itemPayload = "{\"itemId\":\"$itemId\",\"transactionId\":\"$txId\",\"productId\":\"prod-1\",\"productNameSnapshot\":\"Test Item\",\"quantity\":1,\"unitPrice\":100000,\"subtotal\":100000,\"sellingUnit\":\"pcs\"}"
        val saleMovPayload = "{\"movementId\":\"$saleMovId\",\"productId\":\"prod-1\",\"movementType\":\"SALE\",\"quantityDelta\":-1,\"referenceType\":\"TRANSACTION\",\"referenceId\":\"$txId\",\"reason\":\"Penjualan TRX-$uniqueSuffix\",\"createdAt\":1700000000000,\"createdBy\":\"cashier-1\"}"

        syncRepository.enqueue(SyncQueueItem("q-tx-$uniqueSuffix", SyncEntityType.TRANSACTION, txId, SyncOperation.CREATE, txPayload))
        syncRepository.enqueue(SyncQueueItem("q-txi-$uniqueSuffix", SyncEntityType.TRANSACTION_ITEM, itemId, SyncOperation.CREATE, itemPayload))
        syncRepository.enqueue(SyncQueueItem("q-mov-$uniqueSuffix", SyncEntityType.STOCK_MOVEMENT, saleMovId, SyncOperation.CREATE, saleMovPayload))

        val sync1 = syncRepository.syncPendingBatch(limit = 10)
        assertEquals(3, sync1.successCount)

        // Verify remote completed transaction
        val data1 = syncRepository.fetchBootstrapData().getOrThrow()
        val remoteTx1 = data1.transactions.find { it.transactionId == txId }
        assertNotNull(remoteTx1)
        assertEquals(TransactionStatus.COMPLETED, remoteTx1?.transactionStatus)
        assertEquals(1, data1.transactionItems.count { it.itemId == itemId })
        assertEquals(1, data1.stockMovements.count { it.movementId == saleMovId })

        // 2. Transaction CANCELLED sync
        val cancelTxPayload = "{\"transactionId\":\"$txId\",\"transactionNumber\":\"TRX-$uniqueSuffix\",\"transactionStatus\":\"CANCELLED\",\"paymentStatus\":\"PAID\",\"paymentMethod\":\"CASH\",\"totalAmount\":100000,\"amountReceived\":100000,\"changeAmount\":0,\"createdAt\":1700000000000,\"completedAt\":1700000000000,\"cancelledAt\":1700000060000,\"createdBy\":\"cashier-1\",\"cancelledBy\":\"admin-1\",\"cancellationReason\":\"Salah input kasir\"}"
        val reversalMovPayload = "{\"movementId\":\"$reversalMovId\",\"productId\":\"prod-1\",\"movementType\":\"SALE_REVERSAL\",\"quantityDelta\":1,\"referenceType\":\"TRANSACTION\",\"referenceId\":\"$txId\",\"reason\":\"Pembatalan TRX-$uniqueSuffix\",\"createdAt\":1700000060000,\"createdBy\":\"admin-1\"}"

        syncRepository.enqueue(SyncQueueItem("q-tx-cancel-$uniqueSuffix", SyncEntityType.TRANSACTION, txId, SyncOperation.CANCEL, cancelTxPayload))
        syncRepository.enqueue(SyncQueueItem("q-mov-rev-$uniqueSuffix", SyncEntityType.STOCK_MOVEMENT, reversalMovId, SyncOperation.CREATE, reversalMovPayload))

        val sync2 = syncRepository.syncPendingBatch(limit = 10)
        assertEquals(2, sync2.successCount)

        // Verify remote cancellation
        val data2 = syncRepository.fetchBootstrapData().getOrThrow()
        val remoteTxCancelled = data2.transactions.find { it.transactionId == txId }
        assertNotNull(remoteTxCancelled)
        assertEquals(TransactionStatus.CANCELLED, remoteTxCancelled?.transactionStatus)
        assertEquals("Salah input kasir", remoteTxCancelled?.cancellationReason)

        // Verify original SALE movement still intact AND SALE_REVERSAL present
        assertEquals(1, data2.stockMovements.count { it.movementId == saleMovId })
        val revMov = data2.stockMovements.find { it.movementId == reversalMovId }
        assertNotNull(revMov)
        assertEquals(MovementType.SALE_REVERSAL, revMov?.movementType)
        assertEquals(1L, revMov?.quantityDelta)
    }

    @Test
    fun test05_partialBatchFailure_and_staleSyncingRecovery() = runBlocking {
        val uniqueSuffix = System.currentTimeMillis().toString().takeLast(6)
        val validId = "cat-valid-$uniqueSuffix"
        val invalidId = "inv-$uniqueSuffix"

        // 1. Partial Batch: 1 valid category, 1 invalid entity payload to trigger validation error
        val validPayload = "{\"categoryId\":\"$validId\",\"name\":\"Valid Cat $uniqueSuffix\",\"isActive\":true,\"createdAt\":1700000000000,\"updatedAt\":1700000000000}"
        
        // Entity with invalid type
        val opValid = SyncQueueItem("q-v-$uniqueSuffix", SyncEntityType.CATEGORY, validId, SyncOperation.CREATE, validPayload)
        val opInvalid = SyncQueueItem("q-inv-$uniqueSuffix", SyncEntityType.CATEGORY, invalidId, SyncOperation.CREATE, "not-a-json-object")

        syncRepository.enqueue(opValid)
        syncRepository.enqueue(opInvalid)

        val batchResult = syncRepository.syncPendingBatch(limit = 10)
        assertEquals(2, batchResult.totalProcessed)
        assertEquals(1, batchResult.successCount)
        assertEquals(1, batchResult.failureCount)

        val validItemResult = batchResult.itemResults.find { it.queueId == "q-v-$uniqueSuffix" }
        assertEquals("SUCCESS", validItemResult?.status)

        val invalidItemResult = batchResult.itemResults.find { it.queueId == "q-inv-$uniqueSuffix" }
        assertEquals("FAILED", invalidItemResult?.status)
        assertFalse("Validation error must be non-retryable", invalidItemResult?.retryable ?: true)

        // 2. Stale SYNCING recovery
        val staleQueueId = "q-stale-$uniqueSuffix"
        val staleTimestamp = System.currentTimeMillis() - 400_000 // > 5 minutes ago
        db.syncQueueDao().insertSyncQueueItem(
            SyncQueueEntity(
                queueId = staleQueueId,
                entityType = "CATEGORY",
                entityId = "cat-stale-$uniqueSuffix",
                operation = "CREATE",
                payload = validPayload,
                status = "SYNCING",
                createdAt = staleTimestamp
            )
        )

        val recoveredCount = syncRepository.resetStaleSyncing(staleThresholdMs = 300_000)
        assertTrue("Stale record older than 5m must be reset", recoveredCount >= 1)

        val resetItem = db.syncQueueDao().getPendingItems(50).find { it.queueId == staleQueueId }
        assertNotNull("Stale item must be returned to PENDING", resetItem)
        assertEquals("PENDING", resetItem?.status)
    }

    @Test
    fun test06_bootstrapRestore_intoCleanDatabase() = runBlocking {
        // Fetch actual live snapshot from Google Sheets
        val bootstrapResult = syncRepository.fetchBootstrapData()
        assertTrue("Fetch bootstrap from real Google Sheets must succeed", bootstrapResult.isSuccess)
        val remoteData = bootstrapResult.getOrThrow()

        // Create a completely clean Room database
        val cleanDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val cleanSyncRepo = SyncRepositoryImpl(
            appDatabase = cleanDb,
            syncQueueDao = cleanDb.syncQueueDao(),
            httpSyncClient = httpSyncClient,
            syncConfig = syncConfig,
            userDao = cleanDb.userDao(),
            categoryDao = cleanDb.categoryDao(),
            productDao = cleanDb.productDao(),
            priceHistoryDao = cleanDb.priceHistoryDao(),
            transactionDao = cleanDb.transactionDao(),
            transactionItemDao = cleanDb.transactionItemDao(),
            digitalTransactionDao = cleanDb.digitalTransactionDao(),
            stockInDao = cleanDb.stockInDao(),
            stockAdjustmentDao = cleanDb.stockAdjustmentDao(),
            stockMovementDao = cleanDb.stockMovementDao()
        )

        // Restore snapshot into clean database
        val restoreResult = cleanSyncRepo.restoreBootstrapData(remoteData)
        assertTrue("Restoring remote snapshot into clean database must succeed", restoreResult.isSuccess)

        // Verify foreign key integrity and entity preservation
        val availableCategoryIds = remoteData.categories.map { it.categoryId }.toSet()
        val expectedProductCount = remoteData.products.count { it.categoryId in availableCategoryIds }
        val restoredCount = cleanDb.productDao().getAllProducts().first().size
        assertEquals(expectedProductCount, restoredCount)

        cleanDb.close()
    }

    @Test
    fun test07_offlineQueuing_and_reconnectionRecovery() = runBlocking {
        val uniqueSuffix = System.currentTimeMillis().toString().takeLast(6)
        val catId = "cat-offline-$uniqueSuffix"
        val payload = "{\"categoryId\":\"$catId\",\"name\":\"Offline Cat $uniqueSuffix\",\"isActive\":true,\"createdAt\":1700000000000,\"updatedAt\":1700000000000}"

        // 1. Enqueue item while in simulated offline condition (unreachable endpoint)
        val originalUrl = syncConfig.endpointUrl
        syncConfig.endpointUrl = "http://127.0.0.1:54321/offline-mock-unreachable"

        val queueId = "q-off-$uniqueSuffix"
        val queueItem = SyncQueueItem(
            queueId = queueId,
            entityType = SyncEntityType.CATEGORY,
            entityId = catId,
            operation = SyncOperation.CREATE,
            payload = payload
        )
        syncRepository.enqueue(queueItem)

        // Verify locally persisted as PENDING
        val localPendingBefore = db.syncQueueDao().getPendingItems(10).find { it.queueId == queueId }
        assertNotNull("Item must be locally persisted in Room even when offline", localPendingBefore)
        assertEquals("PENDING", localPendingBefore?.status)

        // Attempt sync while offline
        val offlineResult = syncRepository.syncPendingBatch(limit = 10)
        assertTrue("Sync while offline must report isNetworkError", offlineResult.isNetworkError)
        assertEquals(0, offlineResult.successCount)

        // Verify item is still PENDING (not discarded or lost) with retryCount incremented
        val localPendingAfterFailed = db.syncQueueDao().getPendingItems(10).find { it.queueId == queueId }
        assertNotNull("Failed network sync must preserve item in PENDING", localPendingAfterFailed)
        assertEquals("PENDING", localPendingAfterFailed?.status)
        assertEquals(1, localPendingAfterFailed?.retryCount)

        // 2. Restore network connectivity (real live Google Apps Script endpoint)
        syncConfig.endpointUrl = originalUrl

        // Perform sync after network restored
        val onlineResult = syncRepository.syncPendingBatch(limit = 10)
        assertFalse("Restored network sync must not have network error", onlineResult.isNetworkError)
        assertTrue("Pending item must be synced successfully", onlineResult.successCount >= 1)

        // Verify item status in local database is now SYNCED
        val syncedItem = db.syncQueueDao().getQueueItemById(queueId)
        assertNotNull("Synced item must exist", syncedItem)
        assertEquals("SYNCED", syncedItem?.status)

        // 3. Confirm record is present in live remote Google Sheets via bootstrap query
        val bootstrapResult = syncRepository.fetchBootstrapData()
        assertTrue("Live fetch must succeed", bootstrapResult.isSuccess)
        val remoteCategories = bootstrapResult.getOrThrow().categories
        assertTrue("Newly synced category must exist in remote sheet", remoteCategories.any { it.categoryId == catId })
    }
}
