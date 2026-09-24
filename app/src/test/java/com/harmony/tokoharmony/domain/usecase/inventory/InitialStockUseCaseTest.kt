package com.harmony.tokoharmony.domain.usecase.inventory

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.MovementType
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.StockMovement
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.ProductRepository
import com.harmony.tokoharmony.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeInventoryStockRepository : StockRepository {
    val movements = mutableListOf<StockMovement>()
    val stockIns = mutableListOf<com.harmony.tokoharmony.domain.model.StockIn>()
    val adjustments = mutableListOf<com.harmony.tokoharmony.domain.model.StockAdjustment>()

    override suspend fun getCurrentStock(productId: String): Long {
        return movements.filter { it.productId == productId }.sumOf { it.quantityDelta }
    }

    override suspend fun insertStockMovement(movement: StockMovement): Result<Unit> {
        movements.add(movement)
        return Result.Success(Unit)
    }

    override suspend fun insertStockMovements(movements: List<StockMovement>): Result<Unit> {
        this.movements.addAll(movements)
        return Result.Success(Unit)
    }

    override fun getMovementsForProduct(productId: String): Flow<List<StockMovement>> {
        return flowOf(movements.filter { it.productId == productId })
    }

    override fun getAllStockMovements(): Flow<List<StockMovement>> {
        return flowOf(movements.toList())
    }

    override suspend fun hasInitialStock(productId: String): Boolean {
        return movements.any { it.productId == productId && it.movementType == MovementType.INITIAL_STOCK }
    }

    override suspend fun recordInitialStock(productId: String, initialStock: Long, userId: String): Result<Unit> {
        if (hasInitialStock(productId)) {
            return Result.Error(AppError.Validation("Stok awal sudah ada"))
        }
        val mov = StockMovement(
            movementId = "mov-init-$productId",
            productId = productId,
            movementType = MovementType.INITIAL_STOCK,
            quantityDelta = initialStock,
            reason = "Stok Awal",
            createdAt = System.currentTimeMillis(),
            createdBy = userId
        )
        movements.add(mov)
        return Result.Success(Unit)
    }

    override suspend fun recordStockIn(
        productId: String,
        purchaseQuantity: Long,
        purchaseUnit: String,
        conversionFactor: Long,
        note: String?,
        userId: String
    ): Result<com.harmony.tokoharmony.domain.model.StockIn> {
        val stockQty = purchaseQuantity * conversionFactor
        val item = com.harmony.tokoharmony.domain.model.StockIn(
            stockInId = "stockin-${stockIns.size + 1}",
            productId = productId,
            purchaseQuantity = purchaseQuantity,
            purchaseUnit = purchaseUnit,
            conversionFactor = conversionFactor,
            stockQuantity = stockQty,
            note = note,
            createdAt = System.currentTimeMillis(),
            createdBy = userId
        )
        stockIns.add(item)
        val mov = StockMovement(
            movementId = "mov-in-${movements.size + 1}",
            productId = productId,
            movementType = MovementType.STOCK_IN,
            quantityDelta = stockQty,
            referenceType = com.harmony.tokoharmony.domain.model.ReferenceType.STOCK_IN,
            referenceId = item.stockInId,
            reason = note ?: "Barang masuk",
            createdAt = System.currentTimeMillis(),
            createdBy = userId
        )
        movements.add(mov)
        return Result.Success(item)
    }

    override suspend fun recordStockAdjustment(
        productId: String,
        physicalQuantity: Long,
        reason: String,
        note: String?,
        userId: String
    ): Result<com.harmony.tokoharmony.domain.model.StockAdjustment> {
        val current = getCurrentStock(productId)
        val diff = physicalQuantity - current
        val item = com.harmony.tokoharmony.domain.model.StockAdjustment(
            adjustmentId = "adj-${adjustments.size + 1}",
            productId = productId,
            systemQuantity = current,
            physicalQuantity = physicalQuantity,
            difference = diff,
            reason = reason,
            note = note,
            createdAt = System.currentTimeMillis(),
            createdBy = userId
        )
        adjustments.add(item)
        val mov = StockMovement(
            movementId = "mov-adj-${movements.size + 1}",
            productId = productId,
            movementType = MovementType.ADJUSTMENT,
            quantityDelta = diff,
            referenceType = com.harmony.tokoharmony.domain.model.ReferenceType.STOCK_ADJUSTMENT,
            referenceId = item.adjustmentId,
            reason = reason,
            createdAt = System.currentTimeMillis(),
            createdBy = userId
        )
        movements.add(mov)
        return Result.Success(item)
    }

    override fun getAllStockIns(): Flow<List<com.harmony.tokoharmony.domain.model.StockIn>> = flowOf(stockIns)
    override fun getAllStockAdjustments(): Flow<List<com.harmony.tokoharmony.domain.model.StockAdjustment>> = flowOf(adjustments)
    override fun getStockInsForProduct(productId: String): Flow<List<com.harmony.tokoharmony.domain.model.StockIn>> = flowOf(stockIns.filter { it.productId == productId })
    override fun getAdjustmentsForProduct(productId: String): Flow<List<com.harmony.tokoharmony.domain.model.StockAdjustment>> = flowOf(adjustments.filter { it.productId == productId })
}

class FakeProductRepo : ProductRepository {
    val products = mutableMapOf<String, Product>()

    override fun getAllProducts(): Flow<List<Product>> = flowOf(products.values.toList())
    override fun getActiveProducts(): Flow<List<Product>> = flowOf(products.values.filter { it.isActive })
    override suspend fun getProductById(productId: String): Product? = products[productId]
    override suspend fun getProductByBarcode(barcode: String): Product? = products.values.find { it.barcode == barcode }
    override fun searchProducts(query: String, onlyActive: Boolean): Flow<List<Product>> = flowOf(products.values.toList())
    override suspend fun saveProduct(product: Product) {
        products[product.productId] = product
    }
    override suspend fun updateProduct(product: Product) {
        products[product.productId] = product
    }
    override suspend fun setProductActiveStatus(productId: String, isActive: Boolean) {
        products[productId]?.let {
            products[productId] = it.copy(isActive = isActive)
        }
    }
}

class InitialStockUseCaseTest {

    private lateinit var stockRepository: FakeInventoryStockRepository
    private lateinit var productRepository: FakeProductRepo
    private lateinit var recordInitialStockUseCase: RecordInitialStockUseCase

    private val adminUser = User("admin-1", "Admin", Role.ADMIN, "hash", true, 0L, 0L)
    private val cashierUser = User("cashier-1", "Cashier", Role.CASHIER, null, true, 0L, 0L)

    @Before
    fun setUp() {
        stockRepository = FakeInventoryStockRepository()
        productRepository = FakeProductRepo()
        recordInitialStockUseCase = RecordInitialStockUseCase(stockRepository, productRepository)

        productRepository.products["prod-indomie"] = Product(
            productId = "prod-indomie",
            categoryId = "cat-1",
            name = "Indomie Goreng",
            barcode = "8991234567",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 3500L,
            isActive = true,
            createdAt = 0L,
            updatedAt = 0L
        )

        productRepository.products["prod-pulsa"] = Product(
            productId = "prod-pulsa",
            categoryId = "cat-digital",
            name = "Pulsa Telkomsel 10k",
            barcode = null,
            productKind = ProductKind.DIGITAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "voucher",
            stockUnit = "voucher",
            currentPrice = 12000L,
            isActive = true,
            createdAt = 0L,
            updatedAt = 0L
        )
    }

    @Test
    fun admin_canCreateInitialStock_successfully() = runTest {
        val result = recordInitialStockUseCase("prod-indomie", 120L, adminUser)
        assertTrue(result is Result.Success)
        assertEquals(120L, stockRepository.getCurrentStock("prod-indomie"))
        assertTrue(stockRepository.hasInitialStock("prod-indomie"))
    }

    @Test
    fun cashier_cannotCreateInitialStock_rejectedWithAuthorization() = runTest {
        val result = recordInitialStockUseCase("prod-indomie", 120L, cashierUser)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Unauthorized)
        assertEquals(0L, stockRepository.getCurrentStock("prod-indomie"))
    }

    @Test
    fun zeroInitialStock_allowed() = runTest {
        val result = recordInitialStockUseCase("prod-indomie", 0L, adminUser)
        assertTrue(result is Result.Success)
        assertEquals(0L, stockRepository.getCurrentStock("prod-indomie"))
        assertTrue(stockRepository.hasInitialStock("prod-indomie"))
    }

    @Test
    fun negativeInitialStock_rejected() = runTest {
        val result = recordInitialStockUseCase("prod-indomie", -10L, adminUser)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Validation)
    }

    @Test
    fun digitalProduct_initialStockRejected() = runTest {
        val result = recordInitialStockUseCase("prod-pulsa", 100L, adminUser)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Validation)
    }

    @Test
    fun duplicateInitialStock_rejected() = runTest {
        val first = recordInitialStockUseCase("prod-indomie", 120L, adminUser)
        assertTrue(first is Result.Success)

        val second = recordInitialStockUseCase("prod-indomie", 150L, adminUser)
        assertTrue(second is Result.Error)
        assertTrue((second as Result.Error).error is AppError.Validation)
        assertEquals(120L, stockRepository.getCurrentStock("prod-indomie"))
    }
}
