package com.harmony.tokoharmony.domain.usecase

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.usecase.cart.AddProductToDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.UpdateDraftCartItemQuantityUseCase
import com.harmony.tokoharmony.domain.usecase.product.CreateProductUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WeightedProductCalculationTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var transactionRepository: FakeTransactionRepository

    private lateinit var createProductUseCase: CreateProductUseCase
    private lateinit var addProductToDraftCartUseCase: AddProductToDraftCartUseCase
    private lateinit var updateDraftCartItemQuantityUseCase: UpdateDraftCartItemQuantityUseCase

    private val adminUser = User("admin-1", "Admin", Role.ADMIN, "hash")
    private val cashierUser = User("cashier-1", "Kasir", Role.CASHIER, null)

    @Before
    fun setup() {
        productRepository = FakeProductRepository()
        transactionRepository = FakeTransactionRepository(productRepository)

        createProductUseCase = CreateProductUseCase(productRepository)
        addProductToDraftCartUseCase = AddProductToDraftCartUseCase(transactionRepository)
        updateDraftCartItemQuantityUseCase = UpdateDraftCartItemQuantityUseCase(transactionRepository)
    }

    @Test
    fun weightedProduct_500g_subtotalIsAccurateInteger() = runTest {
        val telur = (createProductUseCase(
            categoryId = "cat-1",
            name = "Telur Ayam",
            barcode = null,
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_KG,
            quantityType = QuantityType.GRAM,
            sellingUnit = "kg",
            stockUnit = "kg",
            currentPrice = 30000L, // Rp30.000 / kg
            user = adminUser
        ) as Result.Success).data

        val addResult = addProductToDraftCartUseCase(telur.productId, 500L, cashierUser.userId) as Result.Success
        val item = addResult.data.items[0]

        assertEquals(500L, item.quantity)
        assertEquals(30000L, item.unitPrice)
        // (500 * 30000) / 1000 = 15000
        assertEquals(15000L, item.subtotal)
        assertEquals(15000L, addResult.data.totalAmount)
    }

    @Test
    fun weightedProduct_1000g_1500g_2500g_subtotalsAreAccurate() = runTest {
        val beras = (createProductUseCase(
            categoryId = "cat-1",
            name = "Beras Rojolele",
            barcode = null,
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_KG,
            quantityType = QuantityType.GRAM,
            sellingUnit = "kg",
            stockUnit = "kg",
            currentPrice = 14000L, // Rp14.000 / kg
            user = adminUser
        ) as Result.Success).data

        // 1.0 kg (1000g)
        val add1000 = addProductToDraftCartUseCase(beras.productId, 1000L, cashierUser.userId) as Result.Success
        val itemId = add1000.data.items[0].itemId
        assertEquals(14000L, add1000.data.items[0].subtotal)

        // Update to 1.5 kg (1500g) -> (1500 * 14000) / 1000 = 21000
        val update1500 = updateDraftCartItemQuantityUseCase(itemId, 1500L) as Result.Success
        assertEquals(1500L, update1500.data.items[0].quantity)
        assertEquals(21000L, update1500.data.items[0].subtotal)

        // Update to 2.5 kg (2500g) -> (2500 * 14000) / 1000 = 35000
        val update2500 = updateDraftCartItemQuantityUseCase(itemId, 2500L) as Result.Success
        assertEquals(2500L, update2500.data.items[0].quantity)
        assertEquals(35000L, update2500.data.items[0].subtotal)
    }

    @Test
    fun invalidWeightIncrement_non500g_isRejected() = runTest {
        val cabai = (createProductUseCase(
            categoryId = "cat-1",
            name = "Cabai Rawit",
            barcode = null,
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_KG,
            quantityType = QuantityType.GRAM,
            sellingUnit = "kg",
            stockUnit = "kg",
            currentPrice = 45000L,
            user = adminUser
        ) as Result.Success).data

        // 1370g (not a multiple of 500g)
        val addInvalid = addProductToDraftCartUseCase(cabai.productId, 1370L, cashierUser.userId)
        assertTrue(addInvalid is Result.Error)
        assertTrue((addInvalid as Result.Error).error is AppError.Validation)

        // 250g (not a multiple of 500g)
        val addInvalid250 = addProductToDraftCartUseCase(cabai.productId, 250L, cashierUser.userId)
        assertTrue(addInvalid250 is Result.Error)

        // Add valid 500g first, then try updating to invalid 750g
        val addValid = addProductToDraftCartUseCase(cabai.productId, 500L, cashierUser.userId) as Result.Success
        val itemId = addValid.data.items[0].itemId

        val updateInvalid = updateDraftCartItemQuantityUseCase(itemId, 750L)
        assertTrue(updateInvalid is Result.Error)
        assertTrue((updateInvalid as Result.Error).error is AppError.Validation)
    }
}
