package com.harmony.tokoharmony

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.harmony.tokoharmony.core.database.AppDatabase
import com.harmony.tokoharmony.core.database.entity.CategoryEntity
import com.harmony.tokoharmony.core.database.entity.DigitalTransactionEntity
import com.harmony.tokoharmony.core.database.entity.ProductEntity
import com.harmony.tokoharmony.core.database.entity.StockMovementEntity
import com.harmony.tokoharmony.core.database.entity.SyncQueueEntity
import com.harmony.tokoharmony.core.database.entity.TransactionEntity
import com.harmony.tokoharmony.core.database.entity.TransactionItemEntity
import com.harmony.tokoharmony.core.database.entity.UserEntity
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
import com.harmony.tokoharmony.domain.model.SyncStatus
import com.harmony.tokoharmony.domain.model.TransactionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Phase7VerificationInstrumentedTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testPhase7_DigitalIsolation_MixedCheckout_Cancellation_OnDevice() = runBlocking {
        val now = System.currentTimeMillis()

        // 1. Setup User and Category
        val admin = UserEntity(
            userId = "admin-1",
            displayName = "Admin Toko",
            role = Role.ADMIN.name,
            pinHash = null,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        db.userDao().insertUser(admin)

        val catFisik = CategoryEntity(
            categoryId = "cat-fisik",
            name = "Makanan & Minuman",
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        val catDigital = CategoryEntity(
            categoryId = "cat-digital",
            name = "Produk Digital",
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        db.categoryDao().insertCategory(catFisik)
        db.categoryDao().insertCategory(catDigital)

        // 2. Setup Products: 1 Physical, 1 Digital
        val prodFisik = ProductEntity(
            productId = "prod-mie",
            categoryId = "cat-fisik",
            name = "Mie Instan",
            barcode = "89912345678",
            productKind = ProductKind.PHYSICAL.name,
            pricingMethod = PricingMethod.PER_UNIT.name,
            quantityType = QuantityType.COUNT.name,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            purchaseUnit = null,
            purchaseConversionFactor = null,
            currentPrice = 3500L,
            minimumStock = 10L,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        val prodDigital = ProductEntity(
            productId = "prod-pulsa-50k",
            categoryId = "cat-digital",
            name = "Pulsa Telkomsel 50k",
            barcode = null,
            productKind = ProductKind.DIGITAL.name,
            pricingMethod = PricingMethod.PER_UNIT.name,
            quantityType = QuantityType.COUNT.name,
            sellingUnit = "layanan",
            stockUnit = "layanan",
            purchaseUnit = null,
            purchaseConversionFactor = null,
            currentPrice = 52000L,
            minimumStock = null,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        db.productDao().insertProduct(prodFisik)
        db.productDao().insertProduct(prodDigital)

        // Physical product initial stock: 50 pcs
        val initMovement = StockMovementEntity(
            movementId = "mov-init-1",
            productId = "prod-mie",
            movementType = MovementType.INITIAL_STOCK.name,
            quantityDelta = 50L,
            referenceType = null,
            referenceId = "init-1",
            reason = "Stok awal",
            createdAt = now,
            createdBy = "admin-1"
        )
        db.stockMovementDao().insertStockMovement(initMovement)

        // Verify initial stock
        assertEquals(50L, db.stockMovementDao().getCurrentStock("prod-mie"))
        // Digital stock MUST be 0 (no movements)
        assertEquals(0L, db.stockMovementDao().getCurrentStock("prod-pulsa-50k"))

        // 3. Mixed Checkout: 2x Mie Instan (Physical) + 1x Pulsa 50k (Digital)
        val txId = "tx-mixed-1"
        val tx = TransactionEntity(
            transactionId = txId,
            transactionNumber = "TRX-20260924-001",
            transactionStatus = TransactionStatus.COMPLETED.name,
            paymentStatus = PaymentStatus.PAID.name,
            paymentMethod = PaymentMethod.CASH.name,
            totalAmount = 59000L, // 2 * 3500 + 52000 = 59000
            amountReceived = 60000L,
            changeAmount = 1000L,
            createdAt = now,
            completedAt = now,
            cancelledAt = null,
            createdBy = "admin-1"
        )
        db.transactionDao().insertTransaction(tx)

        val itemPhysical = TransactionItemEntity(
            itemId = "item-fisik-1",
            transactionId = txId,
            productId = "prod-mie",
            productNameSnapshot = "Mie Instan",
            quantity = 2L,
            unitPrice = 3500L,
            subtotal = 7000L,
            sellingUnit = "pcs"
        )
        val itemDigital = TransactionItemEntity(
            itemId = "item-digital-1",
            transactionId = txId,
            productId = "prod-pulsa-50k",
            productNameSnapshot = "Pulsa Telkomsel 50k",
            quantity = 1L,
            unitPrice = 52000L,
            subtotal = 52000L,
            sellingUnit = "layanan"
        )
        db.transactionItemDao().insertTransactionItems(listOf(itemPhysical, itemDigital))

        // Physical item records SALE movement: -2
        val salePhysical = StockMovementEntity(
            movementId = "mov-sale-1",
            productId = "prod-mie",
            movementType = MovementType.SALE.name,
            quantityDelta = -2L,
            referenceType = ReferenceType.TRANSACTION.name,
            referenceId = txId,
            reason = "Penjualan TRX-20260924-001",
            createdAt = now,
            createdBy = "admin-1"
        )
        db.stockMovementDao().insertStockMovement(salePhysical)

        // Digital transaction record: nominal 50k, customer number 08123456789
        val dtEntity = DigitalTransactionEntity(
            digitalTransactionId = "dt-1",
            transactionItemId = "item-digital-1",
            serviceType = "PULSA",
            customerNumber = "08123456789",
            nominal = 50000L,
            providerReference = "SN-20260924-9988",
            status = "SUCCESS",
            createdAt = now
        )
        db.digitalTransactionDao().insertDigitalTransaction(dtEntity)

        // Enqueue sync queue for digital transaction
        val queueItem = SyncQueueEntity(
            queueId = "q-dt-1",
            entityType = SyncEntityType.DIGITAL_TRANSACTION.name,
            entityId = "dt-1",
            operation = SyncOperation.CREATE.name,
            payload = "{\"digitalTransactionId\":\"dt-1\"}",
            status = SyncStatus.PENDING.name,
            retryCount = 0,
            createdAt = now
        )
        db.syncQueueDao().insertSyncQueueItem(queueItem)

        // 4. Invariant Verification: Digital product isolation
        // Physical stock became 50 - 2 = 48
        assertEquals(48L, db.stockMovementDao().getCurrentStock("prod-mie"))
        // Digital stock MUST STRICTLY BE 0
        assertEquals(0L, db.stockMovementDao().getCurrentStock("prod-pulsa-50k"))
        // Digital product has 0 movements in ledger
        val digitalMovements = db.stockMovementDao().getMovementsForProduct("prod-pulsa-50k").first()
        assertTrue(digitalMovements.isEmpty())

        // Verify digital transaction was persisted and can be queried by transaction
        val dtsForTx = db.digitalTransactionDao().getDigitalTransactionsForTransaction(txId)
        assertEquals(1, dtsForTx.size)
        assertEquals("08123456789", dtsForTx[0].customerNumber)
        assertEquals(50000L, dtsForTx[0].nominal)
        assertEquals("SUCCESS", dtsForTx[0].status)

        // 5. Cancellation Behavior:
        // Physical item gets SALE_REVERSAL movement (+2)
        // Digital item gets NO SALE_REVERSAL movement (0 movements)
        val cancelReversalPhysical = StockMovementEntity(
            movementId = "mov-reversal-1",
            productId = "prod-mie",
            movementType = MovementType.SALE_REVERSAL.name,
            quantityDelta = 2L,
            referenceType = ReferenceType.TRANSACTION.name,
            referenceId = txId,
            reason = "Pembatalan TRX-20260924-001",
            createdAt = now + 1000,
            createdBy = "admin-1"
        )
        db.stockMovementDao().insertStockMovement(cancelReversalPhysical)

        // Physical stock restored to 48 + 2 = 50
        assertEquals(50L, db.stockMovementDao().getCurrentStock("prod-mie"))
        // Digital product still has ZERO movements!
        val digitalMovementsAfterCancel = db.stockMovementDao().getMovementsForProduct("prod-pulsa-50k").first()
        assertTrue(digitalMovementsAfterCancel.isEmpty())

        // 6. Low-Level Database Referential Integrity Test (SQLite Cascade Constraint):
        // Note: Production transactions are NEVER deleted (business cancellation uses CANCELLED + SALE_REVERSAL).
        // This test solely verifies that the database foreign-key CASCADE constraint correctly prevents orphaned rows.
        db.transactionDao().deleteTransaction(txId)
        assertNull(db.digitalTransactionDao().getDigitalTransactionById("dt-1"))
    }
}
