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

class StockInUseCaseTest {

    private lateinit var stockRepository: FakeInventoryStockRepository
    private lateinit var productRepository: FakeProductRepo
    private lateinit var recordStockInUseCase: RecordStockInUseCase

    private val adminUser = User("admin-1", "Admin", Role.ADMIN, "hash", true, 0L, 0L)
    private val cashierUser = User("cashier-1", "Cashier", Role.CASHIER, null, true, 0L, 0L)

    @Before
    fun setUp() {
        stockRepository = FakeInventoryStockRepository()
        productRepository = FakeProductRepo()
        recordStockInUseCase = RecordStockInUseCase(stockRepository, productRepository)

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
            purchaseUnit = "Dus",
            purchaseConversionFactor = 40L,
            currentPrice = 3500L,
            isActive = true,
            createdAt = 0L,
            updatedAt = 0L
        )

        productRepository.products["prod-digital"] = Product(
            productId = "prod-digital",
            categoryId = "cat-digital",
            name = "Token Listrik 20k",
            barcode = null,
            productKind = ProductKind.DIGITAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "voucher",
            stockUnit = "voucher",
            currentPrice = 22000L,
            isActive = true,
            createdAt = 0L,
            updatedAt = 0L
        )
    }

    @Test
    fun admin_canRecordStockIn_withValidConversion() = runTest {
        // 4 Dus × 40 pcs/dus = 160 pcs
        val result = recordStockInUseCase(
            productId = "prod-indomie",
            purchaseQuantity = 4L,
            purchaseUnit = "Dus",
            conversionFactor = 40L,
            note = "Beli dari Agen Sembako",
            user = adminUser
        )

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(4L, data.purchaseQuantity)
        assertEquals("Dus", data.purchaseUnit)
        assertEquals(40L, data.conversionFactor)
        assertEquals(160L, data.stockQuantity)
        assertEquals(160L, stockRepository.getCurrentStock("prod-indomie"))
    }

    @Test
    fun cashier_cannotRecordStockIn_rejectedWithAuthorization() = runTest {
        val result = recordStockInUseCase(
            productId = "prod-indomie",
            purchaseQuantity = 4L,
            purchaseUnit = "Dus",
            conversionFactor = 40L,
            note = null,
            user = cashierUser
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Unauthorized)
        assertEquals(0L, stockRepository.getCurrentStock("prod-indomie"))
    }

    @Test
    fun invalidQuantity_zeroOrNegative_rejected() = runTest {
        val resZero = recordStockInUseCase(
            productId = "prod-indomie",
            purchaseQuantity = 0L,
            purchaseUnit = "Dus",
            conversionFactor = 40L,
            note = null,
            user = adminUser
        )
        assertTrue(resZero is Result.Error)
        assertTrue((resZero as Result.Error).error is AppError.Validation)

        val resNeg = recordStockInUseCase(
            productId = "prod-indomie",
            purchaseQuantity = -2L,
            purchaseUnit = "Dus",
            conversionFactor = 40L,
            note = null,
            user = adminUser
        )
        assertTrue(resNeg is Result.Error)
        assertTrue((resNeg as Result.Error).error is AppError.Validation)
    }

    @Test
    fun invalidConversionFactor_rejected() = runTest {
        val res = recordStockInUseCase(
            productId = "prod-indomie",
            purchaseQuantity = 5L,
            purchaseUnit = "Dus",
            conversionFactor = 0L,
            note = null,
            user = adminUser
        )
        assertTrue(res is Result.Error)
        assertTrue((res as Result.Error).error is AppError.Validation)
    }

    @Test
    fun blankPurchaseUnit_rejected() = runTest {
        val res = recordStockInUseCase(
            productId = "prod-indomie",
            purchaseQuantity = 5L,
            purchaseUnit = "   ",
            conversionFactor = 10L,
            note = null,
            user = adminUser
        )
        assertTrue(res is Result.Error)
        assertTrue((res as Result.Error).error is AppError.Validation)
    }

    @Test
    fun digitalProduct_stockInRejected() = runTest {
        val res = recordStockInUseCase(
            productId = "prod-digital",
            purchaseQuantity = 10L,
            purchaseUnit = "lembar",
            conversionFactor = 1L,
            note = null,
            user = adminUser
        )
        assertTrue(res is Result.Error)
        assertTrue((res as Result.Error).error is AppError.Validation)
    }
}
