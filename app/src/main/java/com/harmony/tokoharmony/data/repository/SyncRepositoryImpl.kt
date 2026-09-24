package com.harmony.tokoharmony.data.repository

import androidx.room.withTransaction
import com.harmony.tokoharmony.core.database.AppDatabase
import com.harmony.tokoharmony.core.database.dao.CategoryDao
import com.harmony.tokoharmony.core.database.dao.DigitalTransactionDao
import com.harmony.tokoharmony.core.database.dao.PriceHistoryDao
import com.harmony.tokoharmony.core.database.dao.ProductDao
import com.harmony.tokoharmony.core.database.dao.StockAdjustmentDao
import com.harmony.tokoharmony.core.database.dao.StockInDao
import com.harmony.tokoharmony.core.database.dao.StockMovementDao
import com.harmony.tokoharmony.core.database.dao.SyncQueueDao
import com.harmony.tokoharmony.core.database.dao.TransactionDao
import com.harmony.tokoharmony.core.database.dao.TransactionItemDao
import com.harmony.tokoharmony.core.database.dao.UserDao
import com.harmony.tokoharmony.core.network.HttpSyncClient
import com.harmony.tokoharmony.core.network.SyncConfig
import com.harmony.tokoharmony.core.network.SyncJsonMapper
import com.harmony.tokoharmony.data.local.mapper.toDomain
import com.harmony.tokoharmony.data.local.mapper.toEntity
import com.harmony.tokoharmony.domain.model.BootstrapData
import com.harmony.tokoharmony.domain.model.SyncBatchResult
import com.harmony.tokoharmony.domain.model.SyncQueueItem
import com.harmony.tokoharmony.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val syncQueueDao: SyncQueueDao,
    private val httpSyncClient: HttpSyncClient,
    private val syncConfig: SyncConfig,
    private val userDao: UserDao,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val transactionDao: TransactionDao,
    private val transactionItemDao: TransactionItemDao,
    private val digitalTransactionDao: DigitalTransactionDao,
    private val stockInDao: StockInDao,
    private val stockAdjustmentDao: StockAdjustmentDao,
    private val stockMovementDao: StockMovementDao
) : SyncRepository {

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 5
    }

    override suspend fun enqueue(item: SyncQueueItem): Result<Unit> {
        return try {
            syncQueueDao.insertSyncQueueItem(item.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun enqueueAll(items: List<SyncQueueItem>): Result<Unit> {
        return try {
            syncQueueDao.insertSyncQueueItems(items.map { it.toEntity() })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingItems(limit: Int): List<SyncQueueItem> {
        return syncQueueDao.getPendingItems(limit).map { it.toDomain() }
    }

    override fun getPendingCount(): Flow<Int> {
        return syncQueueDao.getPendingCount()
    }

    override fun getFailedCount(): Flow<Int> {
        return syncQueueDao.getFailedCount()
    }

    override fun getLastSyncTime(): Flow<Long?> {
        return syncQueueDao.getLastSyncTime()
    }

    override fun getAllQueueItems(): Flow<List<SyncQueueItem>> {
        return syncQueueDao.getAllQueueItems().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun resetStaleSyncing(staleThresholdMs: Long): Int {
        val staleThreshold = System.currentTimeMillis() - staleThresholdMs
        return syncQueueDao.resetStaleSyncing(staleThreshold)
    }

    override suspend fun syncPendingBatch(limit: Int): SyncBatchResult {
        if (!syncConfig.isConfigured()) {
            return SyncBatchResult(
                totalProcessed = 0,
                successCount = 0,
                failureCount = 0,
                isNetworkError = false,
                errorMessage = "Google Sheets Web App URL belum dikonfigurasi."
            )
        }

        val pendingEntities = syncQueueDao.getPendingItems(limit)
        if (pendingEntities.isEmpty()) {
            return SyncBatchResult(
                totalProcessed = 0,
                successCount = 0,
                failureCount = 0
            )
        }

        val queueIds = pendingEntities.map { it.queueId }
        // 1. Mark batch as SYNCING in Room
        syncQueueDao.markSyncing(queueIds)

        val pendingItems = pendingEntities.map { it.toDomain() }
        val batchId = UUID.randomUUID().toString()
        val requestJson = SyncJsonMapper.buildBatchRequestJson(
            syncId = batchId,
            deviceId = "android-device",
            items = pendingItems,
            syncToken = syncConfig.syncToken
        )

        // 2. Transmit batch to Apps Script Web App
        val response = httpSyncClient.postJson(syncConfig.endpointUrl, requestJson)

        if (!response.isSuccessful) {
            // Network or HTTP failure -> Revert to PENDING with incremented retry or mark FAILED
            val isRetryableHttp = response.statusCode == -1 || response.statusCode >= 500 || response.statusCode == 429
            val errorMsg = if (response.statusCode == -1) "Koneksi jaringan terputus / timeout" else "HTTP Server Error (${response.statusCode})"

            for (entity in pendingEntities) {
                val nextRetry = entity.retryCount + 1
                val nextStatus = if (isRetryableHttp && nextRetry < MAX_RETRY_ATTEMPTS) "PENDING" else "FAILED"
                syncQueueDao.markFailed(
                    queueId = entity.queueId,
                    error = errorMsg,
                    retryCount = nextRetry,
                    status = nextStatus
                )
            }

            return SyncBatchResult(
                totalProcessed = pendingEntities.size,
                successCount = 0,
                failureCount = pendingEntities.size,
                isNetworkError = true,
                errorMessage = errorMsg
            )
        }

        // 3. Process remote response
        val batchResult = SyncJsonMapper.parseBatchResponse(response.body)

        // If batch response contains per-item results
        val resultMap = batchResult.itemResults.associateBy { it.queueId }
        val now = System.currentTimeMillis()

        for (entity in pendingEntities) {
            val itemResult = resultMap[entity.queueId]
            if (itemResult != null) {
                if (itemResult.status == "SUCCESS") {
                    syncQueueDao.markSynced(entity.queueId, now)
                } else {
                    val nextRetry = entity.retryCount + 1
                    val nextStatus = if (itemResult.retryable && nextRetry < MAX_RETRY_ATTEMPTS) "PENDING" else "FAILED"
                    syncQueueDao.markFailed(
                        queueId = entity.queueId,
                        error = itemResult.error ?: "Gagal diproses remote",
                        retryCount = nextRetry,
                        status = nextStatus
                    )
                }
            } else {
                // If remote returned partial/malformed item results, mark item as PENDING with retry
                val nextRetry = entity.retryCount + 1
                val nextStatus = if (nextRetry < MAX_RETRY_ATTEMPTS) "PENDING" else "FAILED"
                syncQueueDao.markFailed(
                    queueId = entity.queueId,
                    error = "Item tidak tertera di respon remote",
                    retryCount = nextRetry,
                    status = nextStatus
                )
            }
        }

        if (batchResult.successCount > 0) {
            syncConfig.lastSyncTimestamp = now
        }

        return batchResult
    }

    override suspend fun fetchBootstrapData(): Result<BootstrapData> {
        if (!syncConfig.isConfigured()) {
            return Result.failure(IllegalStateException("Google Sheets Web App URL belum dikonfigurasi."))
        }

        val tokenParam = if (syncConfig.syncToken.isNotBlank()) "&token=${java.net.URLEncoder.encode(syncConfig.syncToken, "UTF-8")}" else ""
        val url = if (syncConfig.endpointUrl.contains("?")) {
            "${syncConfig.endpointUrl}&action=bootstrap$tokenParam"
        } else {
            "${syncConfig.endpointUrl}?action=bootstrap$tokenParam"
        }

        val response = httpSyncClient.getJson(url)
        if (!response.isSuccessful) {
            return Result.failure(Exception("Gagal mengambil backup data dari server (${response.statusCode}): ${response.body}"))
        }

        return try {
            val data = SyncJsonMapper.parseBootstrapResponse(response.body)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreBootstrapData(data: BootstrapData): Result<Unit> {
        return try {
            appDatabase.withTransaction {
                // Dependency order insertion with foreign-key integrity validation
                // 1. Users
                if (data.users.isNotEmpty()) {
                    userDao.insertUsers(data.users.map { it.toEntity() })
                }
                val availableUserIds = data.users.map { it.userId }.toSet()

                // 2. Categories
                if (data.categories.isNotEmpty()) {
                    categoryDao.insertCategories(data.categories.map { it.toEntity() })
                }
                val availableCategoryIds = data.categories.map { it.categoryId }.toSet()

                // 3. Products (must reference valid category)
                val validProducts = data.products.filter { availableCategoryIds.contains(it.categoryId) }
                if (validProducts.isNotEmpty()) {
                    productDao.insertProducts(validProducts.map { it.toEntity() })
                }
                val availableProductIds = validProducts.map { it.productId }.toSet()

                // 4. Price History (must reference valid product and user)
                val validPriceHistories = data.priceHistories.filter {
                    availableProductIds.contains(it.productId) && (it.changedBy.isBlank() || availableUserIds.contains(it.changedBy))
                }
                if (validPriceHistories.isNotEmpty()) {
                    priceHistoryDao.insertPriceHistories(validPriceHistories.map { it.toEntity() })
                }

                // 5. Transactions (must reference valid created_by and cancelled_by)
                val validTransactions = data.transactions.filter {
                    availableUserIds.contains(it.createdBy) && (it.cancelledBy.isNullOrBlank() || availableUserIds.contains(it.cancelledBy))
                }
                if (validTransactions.isNotEmpty()) {
                    transactionDao.insertTransactions(validTransactions.map { it.toEntity() })
                }
                val availableTransactionIds = validTransactions.map { it.transactionId }.toSet()

                // 6. Transaction Items (must reference valid transaction and product)
                val validTransactionItems = data.transactionItems.filter {
                    availableTransactionIds.contains(it.transactionId) && availableProductIds.contains(it.productId)
                }
                if (validTransactionItems.isNotEmpty()) {
                    transactionItemDao.insertTransactionItems(validTransactionItems.map { it.toEntity() })
                }
                val availableTransactionItemIds = validTransactionItems.map { it.itemId }.toSet()

                // 6b. Digital Transactions (must reference valid transaction item)
                val validDigitalTransactions = data.digitalTransactions.filter {
                    availableTransactionItemIds.contains(it.transactionItemId)
                }
                if (validDigitalTransactions.isNotEmpty()) {
                    digitalTransactionDao.insertDigitalTransactions(validDigitalTransactions.map { it.toEntity() })
                }

                // 7. Stock Ins (must reference valid product and user)
                val validStockIns = data.stockIns.filter {
                    availableProductIds.contains(it.productId) && availableUserIds.contains(it.createdBy)
                }
                if (validStockIns.isNotEmpty()) {
                    stockInDao.insertStockIns(validStockIns.map { it.toEntity() })
                }

                // 8. Stock Adjustments (must reference valid product and user)
                val validStockAdjustments = data.stockAdjustments.filter {
                    availableProductIds.contains(it.productId) && availableUserIds.contains(it.createdBy)
                }
                if (validStockAdjustments.isNotEmpty()) {
                    stockAdjustmentDao.insertStockAdjustments(validStockAdjustments.map { it.toEntity() })
                }

                // 9. Stock Movements (must reference valid product and user)
                val validStockMovements = data.stockMovements.filter {
                    availableProductIds.contains(it.productId) && availableUserIds.contains(it.createdBy)
                }
                if (validStockMovements.isNotEmpty()) {
                    stockMovementDao.insertStockMovements(validStockMovements.map { it.toEntity() })
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
