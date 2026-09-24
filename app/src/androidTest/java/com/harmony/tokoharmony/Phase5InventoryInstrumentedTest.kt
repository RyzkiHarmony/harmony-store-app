package com.harmony.tokoharmony

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.harmony.tokoharmony.core.database.AppDatabase
import com.harmony.tokoharmony.core.database.entity.CategoryEntity
import com.harmony.tokoharmony.core.database.entity.ProductEntity
import com.harmony.tokoharmony.core.database.entity.StockAdjustmentEntity
import com.harmony.tokoharmony.core.database.entity.StockInEntity
import com.harmony.tokoharmony.core.database.entity.StockMovementEntity
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
import com.harmony.tokoharmony.domain.model.TransactionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Phase5InventoryInstrumentedTest {

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
    fun testInitialStock_StockIn_Opname_Cancellation_OnDevice() = runBlocking {
        val now = System.currentTimeMillis()

        // 1. Insert Users & Category for FK constraints
        val adminUser = UserEntity(
            userId = "admin-1",
            displayName = "Admin User",
            role = Role.ADMIN.name,
            pinHash = "hash123",
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        val cashierUser = UserEntity(
            userId = "cashier-1",
            displayName = "Cashier User",
            role = Role.CASHIER.name,
            pinHash = null,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        db.userDao().insertUser(adminUser)
        db.userDao().insertUser(cashierUser)

        val category = CategoryEntity(
            categoryId = "cat-1",
            name = "Sembako",
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        db.categoryDao().insertCategory(category)

        // 2. Insert physical product
        val product = ProductEntity(
            productId = "prod-device-1",
            categoryId = "cat-1",
            name = "Minyak Goreng 2L",
            barcode = "8999888777",
            productKind = ProductKind.PHYSICAL.name,
            pricingMethod = PricingMethod.PER_UNIT.name,
            quantityType = QuantityType.COUNT.name,
            sellingUnit = "pouch",
            stockUnit = "pouch",
            purchaseUnit = "Karton",
            purchaseConversionFactor = 6L,
            currentPrice = 34000L,
            minimumStock = 5L,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        db.productDao().insertProduct(product)

        // 3. Initial Stock = 12 pouch (2 karton)
        val initialMovement = StockMovementEntity(
            movementId = "mov-init-1",
            productId = product.productId,
            movementType = MovementType.INITIAL_STOCK.name,
            quantityDelta = 12L,
            referenceType = null,
            referenceId = null,
            reason = "Stok Awal",
            createdAt = now,
            createdBy = "admin-1"
        )
        db.stockMovementDao().insertStockMovement(initialMovement)
        assertEquals(12L, db.stockMovementDao().getCurrentStock(product.productId))
        assertTrue(db.stockMovementDao().hasInitialStock(product.productId))

        // 4. Stock In = 3 Karton x 6 pouch = +18 pouch
        val stockInEntity = StockInEntity(
            stockInId = "si-dev-1",
            productId = product.productId,
            purchaseQuantity = 3L,
            purchaseUnit = "Karton",
            conversionFactor = 6L,
            stockQuantity = 18L,
            note = "Beli dari Grosir",
            createdAt = now + 1000L,
            createdBy = "admin-1"
        )
        val stockInMovement = StockMovementEntity(
            movementId = "mov-si-1",
            productId = product.productId,
            movementType = MovementType.STOCK_IN.name,
            quantityDelta = 18L,
            referenceType = ReferenceType.STOCK_IN.name,
            referenceId = stockInEntity.stockInId,
            reason = stockInEntity.note,
            createdAt = now + 1000L,
            createdBy = "admin-1"
        )
        db.stockInDao().insertStockIn(stockInEntity)
        db.stockMovementDao().insertStockMovement(stockInMovement)
        assertEquals(30L, db.stockMovementDao().getCurrentStock(product.productId))

        // 5. Sale = Sold 4 pouch (-4)
        val txId = "tx-dev-1"
        val txEntity = TransactionEntity(
            transactionId = txId,
            transactionNumber = "TRX-20260924-DEV",
            transactionStatus = TransactionStatus.COMPLETED.name,
            paymentStatus = PaymentStatus.PAID.name,
            paymentMethod = PaymentMethod.CASH.name,
            totalAmount = 136000L,
            amountReceived = 150000L,
            changeAmount = 14000L,
            createdAt = now + 2000L,
            completedAt = now + 2000L,
            createdBy = "cashier-1"
        )
        val itemEntity = TransactionItemEntity(
            itemId = "item-dev-1",
            transactionId = txId,
            productId = product.productId,
            productNameSnapshot = product.name,
            quantity = 4L,
            unitPrice = 34000L,
            subtotal = 136000L,
            sellingUnit = "pouch"
        )
        val saleMovement = StockMovementEntity(
            movementId = "mov-sale-1",
            productId = product.productId,
            movementType = MovementType.SALE.name,
            quantityDelta = -4L,
            referenceType = ReferenceType.TRANSACTION.name,
            referenceId = txId,
            reason = "Penjualan TRX-20260924-DEV",
            createdAt = now + 2000L,
            createdBy = "cashier-1"
        )
        db.transactionDao().insertTransaction(txEntity)
        db.transactionItemDao().insertItems(listOf(itemEntity))
        db.stockMovementDao().insertStockMovement(saleMovement)
        assertEquals(26L, db.stockMovementDao().getCurrentStock(product.productId))

        // 6. Stock Opname: System = 26, Physical = 25 (1 rusak) -> diff = -1
        val adjustmentEntity = StockAdjustmentEntity(
            adjustmentId = "adj-dev-1",
            productId = product.productId,
            systemQuantity = 26L,
            physicalQuantity = 25L,
            difference = -1L,
            reason = "Barang rusak",
            note = "1 pouch bocor",
            createdAt = now + 3000L,
            createdBy = "admin-1"
        )
        val adjustmentMovement = StockMovementEntity(
            movementId = "mov-adj-1",
            productId = product.productId,
            movementType = MovementType.ADJUSTMENT.name,
            quantityDelta = -1L,
            referenceType = ReferenceType.STOCK_ADJUSTMENT.name,
            referenceId = adjustmentEntity.adjustmentId,
            reason = "Barang rusak",
            createdAt = now + 3000L,
            createdBy = "admin-1"
        )
        db.stockAdjustmentDao().insertStockAdjustment(adjustmentEntity)
        db.stockMovementDao().insertStockMovement(adjustmentMovement)
        assertEquals(25L, db.stockMovementDao().getCurrentStock(product.productId))

        // 7. Sale Reversal: Transaction tx-dev-1 is cancelled -> restore +4
        db.transactionDao().updateTransaction(
            txEntity.copy(
                transactionStatus = TransactionStatus.CANCELLED.name,
                cancelledAt = now + 4000L,
                cancelledBy = "admin-1",
                cancellationReason = "Pelanggan retur barang"
            )
        )
        val reversalMovement = StockMovementEntity(
            movementId = "mov-rev-1",
            productId = product.productId,
            movementType = MovementType.SALE_REVERSAL.name,
            quantityDelta = 4L,
            referenceType = ReferenceType.TRANSACTION.name,
            referenceId = txId,
            reason = "Pembatalan TRX-20260924-DEV: Pelanggan retur barang",
            createdAt = now + 4000L,
            createdBy = "admin-1"
        )
        db.stockMovementDao().insertStockMovement(reversalMovement)

        // 8. Verification of Authoritative Formula:
        // 12 (INITIAL) + 18 (STOCK_IN) - 4 (SALE) - 1 (ADJUSTMENT) + 4 (SALE_REVERSAL) = 29
        assertEquals(29L, db.stockMovementDao().getCurrentStock(product.productId))

        val allMovements = db.stockMovementDao().getAllMovements().first()
        assertEquals(5, allMovements.size)

        val allStockIns = db.stockInDao().getAllStockIns().first()
        assertEquals(1, allStockIns.size)
        assertEquals(18L, allStockIns[0].stockQuantity)

        val allAdjustments = db.stockAdjustmentDao().getAllStockAdjustments().first()
        assertEquals(1, allAdjustments.size)
        assertEquals(-1L, allAdjustments[0].difference)
    }
}
