package com.harmony.tokoharmony.domain.usecase

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.usecase.price.ChangeProductPriceUseCase
import com.harmony.tokoharmony.domain.usecase.price.GetPriceHistoryUseCase
import com.harmony.tokoharmony.domain.usecase.product.CreateProductUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PriceManagementTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var priceRepository: FakePriceRepository

    private lateinit var createProductUseCase: CreateProductUseCase
    private lateinit var changeProductPriceUseCase: ChangeProductPriceUseCase
    private lateinit var getPriceHistoryUseCase: GetPriceHistoryUseCase

    private val adminUser = User(
        userId = "admin-1",
        displayName = "Pemilik Toko",
        role = Role.ADMIN,
        pinHash = "hash"
    )

    private val cashierUser = User(
        userId = "cashier-1",
        displayName = "Kasir",
        role = Role.CASHIER,
        pinHash = null
    )

    @Before
    fun setup() {
        productRepository = FakeProductRepository()
        priceRepository = FakePriceRepository(productRepository)

        createProductUseCase = CreateProductUseCase(productRepository)
        changeProductPriceUseCase = ChangeProductPriceUseCase(productRepository, priceRepository)
        getPriceHistoryUseCase = GetPriceHistoryUseCase(priceRepository)
    }

    @Test
    fun admin_canChangeProductPrice_andPriceHistoryIsCreated() = runTest {
        val createResult = createProductUseCase(
            categoryId = "cat-1",
            name = "Minyak Goreng 1L",
            barcode = "8996661111",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pouch",
            stockUnit = "pouch",
            currentPrice = 14000L,
            user = adminUser
        ) as Result.Success

        val productId = createResult.data.productId

        val changeResult = changeProductPriceUseCase(
            productId = productId,
            newPrice = 15500L,
            user = adminUser
        )

        assertTrue(changeResult is Result.Success)
        val historyRecord = (changeResult as Result.Success).data
        assertEquals(productId, historyRecord.productId)
        assertEquals(14000L, historyRecord.oldPrice)
        assertEquals(15500L, historyRecord.newPrice)
        assertEquals(adminUser.userId, historyRecord.changedBy)

        val historyList = getPriceHistoryUseCase(productId).first()
        assertEquals(1, historyList.size)
        assertEquals(14000L, historyList[0].oldPrice)
        assertEquals(15500L, historyList[0].newPrice)
    }

    @Test
    fun cashier_cannotChangeProductPrice() = runTest {
        val createResult = createProductUseCase(
            categoryId = "cat-1",
            name = "Gula Pasir 1kg",
            barcode = "8996662222",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "kg",
            stockUnit = "kg",
            currentPrice = 17000L,
            user = adminUser
        ) as Result.Success

        val productId = createResult.data.productId

        val changeResult = changeProductPriceUseCase(
            productId = productId,
            newPrice = 16000L,
            user = cashierUser
        )

        assertTrue(changeResult is Result.Error)
        val error = (changeResult as Result.Error).error
        assertTrue(error is AppError.Unauthorized)

        val historyList = getPriceHistoryUseCase(productId).first()
        assertTrue(historyList.isEmpty())
    }

    @Test
    fun identicalPrice_isRejected_andDoesNotCreatePriceHistory() = runTest {
        val createResult = createProductUseCase(
            categoryId = "cat-1",
            name = "Garam Dapur",
            barcode = "8996663333",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "bks",
            stockUnit = "bks",
            currentPrice = 3000L,
            user = adminUser
        ) as Result.Success

        val productId = createResult.data.productId

        val changeResult = changeProductPriceUseCase(
            productId = productId,
            newPrice = 3000L, // Same as current price
            user = adminUser
        )

        assertTrue(changeResult is Result.Error)
        assertTrue((changeResult as Result.Error).error is AppError.Validation)

        val historyList = getPriceHistoryUseCase(productId).first()
        assertTrue(historyList.isEmpty())
    }

    @Test
    fun multiplePriceChanges_preserveCompleteHistory() = runTest {
        val createResult = createProductUseCase(
            categoryId = "cat-1",
            name = "Telur Ayam",
            barcode = null,
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_KG,
            quantityType = QuantityType.GRAM,
            sellingUnit = "kg",
            stockUnit = "kg",
            currentPrice = 28000L,
            user = adminUser
        ) as Result.Success

        val productId = createResult.data.productId

        // Price change 1: 28000 -> 29000
        val change1 = changeProductPriceUseCase(productId, 29000L, adminUser)
        assertTrue(change1 is Result.Success)

        // Update product in repository to simulate DB update
        productRepository.saveProduct(productRepository.getProductById(productId)!!.copy(currentPrice = 29000L))

        // Ensure distinct timestamp in test
        delay(10)

        // Price change 2: 29000 -> 30500
        val change2 = changeProductPriceUseCase(productId, 30500L, adminUser)
        assertTrue(change2 is Result.Success)

        val historyList = getPriceHistoryUseCase(productId).first()
        assertEquals(2, historyList.size)
        // Verify price history items
        assertEquals(30500L, historyList[0].newPrice)
        assertEquals(29000L, historyList[0].oldPrice)
        assertEquals(29000L, historyList[1].newPrice)
        assertEquals(28000L, historyList[1].oldPrice)
    }

    @Test
    fun priceMustBeNonNegative_rejectsNegativePrice() = runTest {
        val createResult = createProductUseCase(
            categoryId = "cat-1",
            name = "Kopi Sachet",
            barcode = "8997771111",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "sachet",
            stockUnit = "sachet",
            currentPrice = 2000L,
            user = adminUser
        ) as Result.Success

        val changeResult = changeProductPriceUseCase(
            productId = createResult.data.productId,
            newPrice = -500L,
            user = adminUser
        )

        assertTrue(changeResult is Result.Error)
        assertTrue((changeResult as Result.Error).error is AppError.Validation)
    }
}
