package com.harmony.tokoharmony.domain.usecase.inventory

import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.MovementType
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.ReferenceType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.StockAdjustment
import com.harmony.tokoharmony.domain.model.StockIn
import com.harmony.tokoharmony.domain.model.StockMovement
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StockLedgerCalculationTest {

    private lateinit var stockRepository: FakeStockLedgerRepository
    private lateinit var getCurrentStockUseCase: GetCurrentStockUseCase

    private val adminUser = User(
        userId = "admin-1",
        displayName = "Admin",
        role = Role.ADMIN,
        pinHash = "hash",
        isActive = true
    )

    private val countProduct = Product(
        productId = "prod-count-1",
        categoryId = "cat-1",
        name = "Indomie Goreng",
        barcode = "899123456",
        productKind = ProductKind.PHYSICAL,
        pricingMethod = PricingMethod.PER_UNIT,
        quantityType = QuantityType.COUNT,
        sellingUnit = "pcs",
        stockUnit = "pcs",
        currentPrice = 3500L,
        minimumStock = 10L,
        isActive = true
    )

    private val digitalProduct = Product(
        productId = "prod-digital-1",
        categoryId = "cat-2",
        name = "Pulsa Telkomsel 10k",
        barcode = null,
        productKind = ProductKind.DIGITAL,
        pricingMethod = PricingMethod.PER_UNIT,
        quantityType = QuantityType.COUNT,
        sellingUnit = "voucher",
        stockUnit = "voucher",
        currentPrice = 12000L,
        minimumStock = null,
        isActive = true
    )

    @Before
    fun setup() {
        stockRepository = FakeStockLedgerRepository()
        getCurrentStockUseCase = GetCurrentStockUseCase(stockRepository)
    }

    @Test
    fun `stock ledger calculates correct sum for INITIAL + IN - SALE + REVERSAL + ADJUSTMENT`() = runTest {
        val pId = countProduct.productId

        // 1. Initial stock = 100
        stockRepository.addMovement(
            StockMovement(
                movementId = "m-1",
                productId = pId,
                movementType = MovementType.INITIAL_STOCK,
                quantityDelta = 100L,
                referenceType = null,
                referenceId = null,
                reason = "Initial inventory",
                createdAt = 1000L,
                createdBy = adminUser.userId
            )
        )
        assertEquals(100L, getCurrentStockUseCase(pId))

        // 2. Stock In = +50 (e.g. 1 dus x 50 pcs)
        stockRepository.addMovement(
            StockMovement(
                movementId = "m-2",
                productId = pId,
                movementType = MovementType.STOCK_IN,
                quantityDelta = 50L,
                referenceType = ReferenceType.STOCK_IN,
                referenceId = "si-1",
                reason = null,
                createdAt = 2000L,
                createdBy = adminUser.userId
            )
        )
        assertEquals(150L, getCurrentStockUseCase(pId))

        // 3. Sale = -20
        stockRepository.addMovement(
            StockMovement(
                movementId = "m-3",
                productId = pId,
                movementType = MovementType.SALE,
                quantityDelta = -20L,
                referenceType = ReferenceType.TRANSACTION,
                referenceId = "tx-1",
                reason = null,
                createdAt = 3000L,
                createdBy = "cashier-1"
            )
        )
        assertEquals(130L, getCurrentStockUseCase(pId))

        // 4. Sale Reversal (Cancelled transaction tx-1) = +20
        stockRepository.addMovement(
            StockMovement(
                movementId = "m-4",
                productId = pId,
                movementType = MovementType.SALE_REVERSAL,
                quantityDelta = 20L,
                referenceType = ReferenceType.TRANSACTION,
                referenceId = "tx-1",
                reason = "Pembatalan transaksi",
                createdAt = 4000L,
                createdBy = adminUser.userId
            )
        )
        assertEquals(150L, getCurrentStockUseCase(pId))

        // 5. Another Sale = -15
        stockRepository.addMovement(
            StockMovement(
                movementId = "m-5",
                productId = pId,
                movementType = MovementType.SALE,
                quantityDelta = -15L,
                referenceType = ReferenceType.TRANSACTION,
                referenceId = "tx-2",
                reason = null,
                createdAt = 5000L,
                createdBy = "cashier-1"
            )
        )
        assertEquals(135L, getCurrentStockUseCase(pId))

        // 6. Stock Opname / Adjustment: system was 135, physical is 130, delta = -5 (damaged goods)
        stockRepository.addMovement(
            StockMovement(
                movementId = "m-6",
                productId = pId,
                movementType = MovementType.ADJUSTMENT,
                quantityDelta = -5L,
                referenceType = ReferenceType.STOCK_ADJUSTMENT,
                referenceId = "adj-1",
                reason = "Barang rusak",
                createdAt = 6000L,
                createdBy = adminUser.userId
            )
        )
        // Expected: 100 + 50 - 20 + 20 - 15 + (-5) = 130
        assertEquals(130L, getCurrentStockUseCase(pId))
    }

    @Test
    fun `product with no movements has zero stock`() = runTest {
        assertEquals(0L, getCurrentStockUseCase("unknown-product"))
    }

    @Test
    fun `stock ledger maintains product isolation across different products`() = runTest {
        // Product 1
        stockRepository.addMovement(
            StockMovement(
                movementId = "m-1",
                productId = "prod-1",
                movementType = MovementType.INITIAL_STOCK,
                quantityDelta = 50L,
                referenceType = null,
                referenceId = null,
                reason = "Initial",
                createdAt = 1000L,
                createdBy = adminUser.userId
            )
        )

        // Product 2
        stockRepository.addMovement(
            StockMovement(
                movementId = "m-2",
                productId = "prod-2",
                movementType = MovementType.INITIAL_STOCK,
                quantityDelta = 200L,
                referenceType = null,
                referenceId = null,
                reason = "Initial",
                createdAt = 1000L,
                createdBy = adminUser.userId
            )
        )

        // Stock in for Product 1 only
        stockRepository.addMovement(
            StockMovement(
                movementId = "m-3",
                productId = "prod-1",
                movementType = MovementType.STOCK_IN,
                quantityDelta = 25L,
                referenceType = ReferenceType.STOCK_IN,
                referenceId = "si-1",
                reason = null,
                createdAt = 2000L,
                createdBy = adminUser.userId
            )
        )

        assertEquals(75L, getCurrentStockUseCase("prod-1"))
        assertEquals(200L, getCurrentStockUseCase("prod-2"))
    }

    private class FakeStockLedgerRepository : StockRepository {
        private val movements = mutableListOf<StockMovement>()
        private val movementsFlow = MutableStateFlow<List<StockMovement>>(emptyList())

        fun addMovement(movement: StockMovement) {
            movements.add(movement)
            movementsFlow.value = movements.toList()
        }

        override suspend fun getCurrentStock(productId: String): Long {
            return movements.filter { it.productId == productId }.sumOf { it.quantityDelta }
        }

        override suspend fun insertStockMovement(movement: StockMovement): Result<Unit> {
            addMovement(movement)
            return Result.Success(Unit)
        }

        override suspend fun insertStockMovements(movements: List<StockMovement>): Result<Unit> {
            movements.forEach { addMovement(it) }
            return Result.Success(Unit)
        }

        override fun getMovementsForProduct(productId: String): Flow<List<StockMovement>> {
            return movementsFlow.map { list -> list.filter { it.productId == productId } }
        }

        override fun getAllStockMovements(): Flow<List<StockMovement>> {
            return movementsFlow
        }

        override suspend fun hasInitialStock(productId: String): Boolean {
            return movements.any { it.productId == productId && it.movementType == MovementType.INITIAL_STOCK }
        }

        override suspend fun recordInitialStock(
            productId: String,
            initialStock: Long,
            userId: String
        ): Result<Unit> {
            addMovement(
                StockMovement(
                    movementId = "init-$productId",
                    productId = productId,
                    movementType = MovementType.INITIAL_STOCK,
                    quantityDelta = initialStock,
                    reason = "Stok Awal",
                    createdAt = System.currentTimeMillis(),
                    createdBy = userId
                )
            )
            return Result.Success(Unit)
        }

        override suspend fun recordStockIn(
            productId: String,
            purchaseQuantity: Long,
            purchaseUnit: String,
            conversionFactor: Long,
            note: String?,
            userId: String
        ): Result<StockIn> {
            val stockQty = purchaseQuantity * conversionFactor
            val item = StockIn(
                stockInId = "si-1",
                productId = productId,
                purchaseQuantity = purchaseQuantity,
                purchaseUnit = purchaseUnit,
                conversionFactor = conversionFactor,
                stockQuantity = stockQty,
                note = note,
                createdAt = System.currentTimeMillis(),
                createdBy = userId
            )
            addMovement(
                StockMovement(
                    movementId = "mov-si-1",
                    productId = productId,
                    movementType = MovementType.STOCK_IN,
                    quantityDelta = stockQty,
                    referenceType = ReferenceType.STOCK_IN,
                    referenceId = item.stockInId,
                    reason = note,
                    createdAt = System.currentTimeMillis(),
                    createdBy = userId
                )
            )
            return Result.Success(item)
        }

        override suspend fun recordStockAdjustment(
            productId: String,
            physicalQuantity: Long,
            reason: String,
            note: String?,
            userId: String
        ): Result<StockAdjustment> {
            val current = getCurrentStock(productId)
            val diff = physicalQuantity - current
            val item = StockAdjustment(
                adjustmentId = "adj-1",
                productId = productId,
                systemQuantity = current,
                physicalQuantity = physicalQuantity,
                difference = diff,
                reason = reason,
                note = note,
                createdAt = System.currentTimeMillis(),
                createdBy = userId
            )
            addMovement(
                StockMovement(
                    movementId = "mov-adj-1",
                    productId = productId,
                    movementType = MovementType.ADJUSTMENT,
                    quantityDelta = diff,
                    referenceType = ReferenceType.STOCK_ADJUSTMENT,
                    referenceId = item.adjustmentId,
                    reason = reason,
                    createdAt = System.currentTimeMillis(),
                    createdBy = userId
                )
            )
            return Result.Success(item)
        }

        override fun getAllStockIns(): Flow<List<StockIn>> = flowOf(emptyList())
        override fun getAllStockAdjustments(): Flow<List<StockAdjustment>> = flowOf(emptyList())
        override fun getStockInsForProduct(productId: String): Flow<List<StockIn>> = flowOf(emptyList())
        override fun getAdjustmentsForProduct(productId: String): Flow<List<StockAdjustment>> = flowOf(emptyList())
    }
}
