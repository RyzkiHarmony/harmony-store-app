package com.harmony.tokoharmony.domain.usecase.product

import com.harmony.tokoharmony.domain.model.MovementType
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.StockLevelStatus
import com.harmony.tokoharmony.domain.model.StockMovement
import com.harmony.tokoharmony.domain.usecase.inventory.FakeInventoryStockRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class EvaluateProductStockStatusUseCaseTest {

    private lateinit var stockRepository: FakeInventoryStockRepository
    private lateinit var evaluateProductStockStatusUseCase: EvaluateProductStockStatusUseCase

    @Before
    fun setUp() {
        stockRepository = FakeInventoryStockRepository()
        evaluateProductStockStatusUseCase = EvaluateProductStockStatusUseCase(stockRepository)
    }

    private fun createProduct(
        id: String,
        kind: ProductKind,
        minStock: Long? = null
    ): Product {
        return Product(
            productId = id,
            name = "Test Product $id",
            barcode = null,
            categoryId = "cat-1",
            productKind = kind,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            currentPrice = 10000L,
            sellingUnit = "PCS",
            stockUnit = "PCS",
            minimumStock = minStock,
            isActive = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )
    }

    private fun addStock(productId: String, delta: Long) {
        stockRepository.movements.add(
            StockMovement(
                movementId = "mov-$productId",
                productId = productId,
                movementType = MovementType.INITIAL_STOCK,
                quantityDelta = delta,
                referenceType = null,
                referenceId = null,
                reason = "test",
                createdAt = 1000L,
                createdBy = "admin"
            )
        )
    }

    @Test
    fun `digital product always returns DIGITAL status regardless of stock or minimumStock`() {
        val digitalProd = createProduct("dig-1", ProductKind.DIGITAL, minStock = 10L)

        assertEquals(StockLevelStatus.DIGITAL, evaluateProductStockStatusUseCase(digitalProd, currentStock = -5L))
        assertEquals(StockLevelStatus.DIGITAL, evaluateProductStockStatusUseCase(digitalProd, currentStock = 0L))
        assertEquals(StockLevelStatus.DIGITAL, evaluateProductStockStatusUseCase(digitalProd, currentStock = 100L))
    }

    @Test
    fun `physical product with 0 or negative stock returns OUT_OF_STOCK`() {
        val prod = createProduct("phys-1", ProductKind.PHYSICAL, minStock = 5L)

        assertEquals(StockLevelStatus.OUT_OF_STOCK, evaluateProductStockStatusUseCase(prod, currentStock = 0L))
        assertEquals(StockLevelStatus.OUT_OF_STOCK, evaluateProductStockStatusUseCase(prod, currentStock = -2L))
    }

    @Test
    fun `physical product with stock less than or equal to minimumStock returns LOW_STOCK`() {
        val prod = createProduct("phys-2", ProductKind.PHYSICAL, minStock = 10L)

        assertEquals(StockLevelStatus.LOW_STOCK, evaluateProductStockStatusUseCase(prod, currentStock = 10L))
        assertEquals(StockLevelStatus.LOW_STOCK, evaluateProductStockStatusUseCase(prod, currentStock = 3L))
    }

    @Test
    fun `physical product with stock greater than minimumStock returns NORMAL`() {
        val prod = createProduct("phys-3", ProductKind.PHYSICAL, minStock = 10L)

        assertEquals(StockLevelStatus.NORMAL, evaluateProductStockStatusUseCase(prod, currentStock = 11L))
        assertEquals(StockLevelStatus.NORMAL, evaluateProductStockStatusUseCase(prod, currentStock = 100L))
    }

    @Test
    fun `physical product without minimumStock returns NORMAL if stock is positive`() {
        val prod = createProduct("phys-4", ProductKind.PHYSICAL, minStock = null)

        assertEquals(StockLevelStatus.NORMAL, evaluateProductStockStatusUseCase(prod, currentStock = 1L))
    }

    @Test
    fun `getStockInfo for digital product does not query repository stock and returns 0`() = runTest {
        val digitalProd = createProduct("dig-2", ProductKind.DIGITAL, minStock = 20L)
        addStock("dig-2", 500L) // Even if dummy movements exist

        val info = evaluateProductStockStatusUseCase.getStockInfo(digitalProd)
        assertEquals("dig-2", info.productId)
        assertEquals(0L, info.currentStock)
        assertEquals(StockLevelStatus.DIGITAL, info.stockLevel)
    }

    @Test
    fun `getStockInfo for physical product retrieves stock from repository`() = runTest {
        val physProd = createProduct("phys-5", ProductKind.PHYSICAL, minStock = 5L)
        addStock("phys-5", 3L)

        val info = evaluateProductStockStatusUseCase.getStockInfo(physProd)
        assertEquals("phys-5", info.productId)
        assertEquals(3L, info.currentStock)
        assertEquals(StockLevelStatus.LOW_STOCK, info.stockLevel)
    }
}
