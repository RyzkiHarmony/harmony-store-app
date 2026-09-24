package com.harmony.tokoharmony.domain.usecase.inventory

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StockOpnameUseCaseTest {

    private lateinit var stockRepository: FakeInventoryStockRepository
    private lateinit var productRepository: FakeProductRepo
    private lateinit var recordStockAdjustmentUseCase: RecordStockAdjustmentUseCase

    private val adminUser = User("admin-1", "Admin", Role.ADMIN, "hash", true, 0L, 0L)
    private val cashierUser = User("cashier-1", "Cashier", Role.CASHIER, null, true, 0L, 0L)

    @Before
    fun setUp() {
        stockRepository = FakeInventoryStockRepository()
        productRepository = FakeProductRepo()
        recordStockAdjustmentUseCase = RecordStockAdjustmentUseCase(stockRepository, productRepository)

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

        // Set initial stock to 157 pcs
        stockRepository.movements.add(
            com.harmony.tokoharmony.domain.model.StockMovement(
                movementId = "init-1",
                productId = "prod-indomie",
                movementType = com.harmony.tokoharmony.domain.model.MovementType.INITIAL_STOCK,
                quantityDelta = 157L,
                createdAt = 0L,
                createdBy = "admin-1"
            )
        )
    }

    @Test
    fun negativeAdjustment_system157_physical155_differenceMinus2() = runTest {
        // System = 157, Physical = 155 -> diff = -2
        val result = recordStockAdjustmentUseCase(
            productId = "prod-indomie",
            physicalQuantity = 155L,
            reason = "Barang rusak",
            note = "2 pcs bocor",
            user = adminUser
        )

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(157L, data.systemQuantity)
        assertEquals(155L, data.physicalQuantity)
        assertEquals(-2L, data.difference)
        assertEquals("Barang rusak", data.reason)
        assertEquals(155L, stockRepository.getCurrentStock("prod-indomie"))
    }

    @Test
    fun positiveAdjustment_system157_physical160_differencePlus3() = runTest {
        // System = 157, Physical = 160 -> diff = +3
        val result = recordStockAdjustmentUseCase(
            productId = "prod-indomie",
            physicalQuantity = 160L,
            reason = "Kesalahan pencatatan",
            note = "Ditemukan di rak bawah",
            user = adminUser
        )

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(157L, data.systemQuantity)
        assertEquals(160L, data.physicalQuantity)
        assertEquals(3L, data.difference)
        assertEquals(160L, stockRepository.getCurrentStock("prod-indomie"))
    }

    @Test
    fun zeroDifference_adjustment_allowed() = runTest {
        val result = recordStockAdjustmentUseCase(
            productId = "prod-indomie",
            physicalQuantity = 157L,
            reason = "Stock opname",
            note = "Semua sesuai",
            user = adminUser
        )

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(0L, data.difference)
        assertEquals(157L, stockRepository.getCurrentStock("prod-indomie"))
    }

    @Test
    fun cashier_cannotAdjustStock_rejected() = runTest {
        val result = recordStockAdjustmentUseCase(
            productId = "prod-indomie",
            physicalQuantity = 150L,
            reason = "Stock opname",
            note = null,
            user = cashierUser
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Unauthorized)
        assertEquals(157L, stockRepository.getCurrentStock("prod-indomie"))
    }

    @Test
    fun negativePhysicalStock_rejected() = runTest {
        val result = recordStockAdjustmentUseCase(
            productId = "prod-indomie",
            physicalQuantity = -5L,
            reason = "Stock opname",
            note = null,
            user = adminUser
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Validation)
    }

    @Test
    fun blankReason_rejected() = runTest {
        val result = recordStockAdjustmentUseCase(
            productId = "prod-indomie",
            physicalQuantity = 150L,
            reason = "   ",
            note = null,
            user = adminUser
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Validation)
    }
}
