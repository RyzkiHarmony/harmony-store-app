package com.harmony.tokoharmony.domain.usecase

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.model.PriceHistory
import com.harmony.tokoharmony.domain.repository.PriceRepository
import com.harmony.tokoharmony.domain.repository.ProductRepository
import com.harmony.tokoharmony.domain.usecase.product.ActivateProductUseCase
import com.harmony.tokoharmony.domain.usecase.product.CreateProductUseCase
import com.harmony.tokoharmony.domain.usecase.product.DeactivateProductUseCase
import com.harmony.tokoharmony.domain.usecase.product.GetProductByBarcodeUseCase
import com.harmony.tokoharmony.domain.usecase.product.GetProductByIdUseCase
import com.harmony.tokoharmony.domain.usecase.product.GetProductsUseCase
import com.harmony.tokoharmony.domain.usecase.product.SearchProductsUseCase
import com.harmony.tokoharmony.domain.usecase.product.UpdateProductUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeProductRepository : ProductRepository {
    val products = mutableMapOf<String, Product>()

    override fun getAllProducts(): Flow<List<Product>> {
        return flowOf(products.values.toList())
    }

    override fun getActiveProducts(): Flow<List<Product>> {
        return flowOf(products.values.filter { it.isActive })
    }

    override suspend fun getProductById(productId: String): Product? {
        return products[productId]
    }

    override suspend fun getProductByBarcode(barcode: String): Product? {
        return products.values.find { it.barcode == barcode }
    }

    override fun searchProducts(query: String, onlyActive: Boolean): Flow<List<Product>> {
        val filtered = products.values.filter {
            (!onlyActive || it.isActive) &&
                (it.name.contains(query, ignoreCase = true) || it.barcode?.contains(query, ignoreCase = true) == true)
        }
        return flowOf(filtered)
    }

    override suspend fun saveProduct(product: Product) {
        products[product.productId] = product
    }

    override suspend fun updateProduct(product: Product) {
        products[product.productId] = product
    }

    override suspend fun setProductActiveStatus(productId: String, isActive: Boolean) {
        products[productId]?.let {
            products[productId] = it.copy(isActive = isActive, updatedAt = System.currentTimeMillis())
        }
    }
}

class FakePriceRepository(private val productRepository: FakeProductRepository? = null) : PriceRepository {
    val history = mutableListOf<PriceHistory>()

    override fun getPriceHistory(productId: String): Flow<List<PriceHistory>> {
        return flowOf(history.filter { it.productId == productId }.reversed())
    }

    override suspend fun getPriceHistoryList(productId: String): List<PriceHistory> {
        return history.filter { it.productId == productId }.reversed()
    }

    override suspend fun recordPriceChange(priceHistory: PriceHistory, newPrice: Long) {
        history.add(priceHistory)
        productRepository?.let { repo ->
            repo.products[priceHistory.productId]?.let { prod ->
                repo.products[priceHistory.productId] = prod.copy(currentPrice = newPrice, updatedAt = priceHistory.changedAt)
            }
        }
    }
}

class ProductUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var priceRepository: FakePriceRepository

    private lateinit var createProductUseCase: CreateProductUseCase
    private lateinit var updateProductUseCase: UpdateProductUseCase
    private lateinit var deactivateProductUseCase: DeactivateProductUseCase
    private lateinit var activateProductUseCase: ActivateProductUseCase
    private lateinit var getProductsUseCase: GetProductsUseCase
    private lateinit var searchProductsUseCase: SearchProductsUseCase
    private lateinit var getProductByBarcodeUseCase: GetProductByBarcodeUseCase
    private lateinit var getProductByIdUseCase: GetProductByIdUseCase

    private val adminUser = User(
        userId = "admin-1",
        displayName = "Admin",
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
        priceRepository = FakePriceRepository()

        createProductUseCase = CreateProductUseCase(productRepository)
        updateProductUseCase = UpdateProductUseCase(productRepository, priceRepository)
        deactivateProductUseCase = DeactivateProductUseCase(productRepository)
        activateProductUseCase = ActivateProductUseCase(productRepository)
        getProductsUseCase = GetProductsUseCase(productRepository)
        searchProductsUseCase = SearchProductsUseCase(productRepository)
        getProductByBarcodeUseCase = GetProductByBarcodeUseCase(productRepository)
        getProductByIdUseCase = GetProductByIdUseCase(productRepository)
    }

    @Test
    fun createValidProduct_success() = runTest {
        val result = createProductUseCase(
            categoryId = "cat-1",
            name = "Indomie Goreng",
            barcode = "8991234567",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 3500L,
            user = adminUser
        )

        assertTrue(result is Result.Success)
        val product = (result as Result.Success).data
        assertEquals("Indomie Goreng", product.name)
        assertEquals("8991234567", product.barcode)
        assertEquals(3500L, product.currentPrice)
        assertTrue(product.isActive)
        assertNotNull(productRepository.getProductById(product.productId))
    }

    @Test
    fun createProduct_allowsNullOrBlankBarcode() = runTest {
        val result = createProductUseCase(
            categoryId = "cat-1",
            name = "Beras Rojolele",
            barcode = null,
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_KG,
            quantityType = QuantityType.GRAM,
            sellingUnit = "kg",
            stockUnit = "kg",
            currentPrice = 15000L,
            user = adminUser
        )

        assertTrue(result is Result.Success)
        val product = (result as Result.Success).data
        assertNull(product.barcode)
    }

    @Test
    fun createProduct_rejectsDuplicateBarcode() = runTest {
        createProductUseCase(
            categoryId = "cat-1",
            name = "Aqua 600ml",
            barcode = "8999999999",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "botol",
            stockUnit = "botol",
            currentPrice = 4000L,
            user = adminUser
        )

        val duplicateResult = createProductUseCase(
            categoryId = "cat-1",
            name = "Aqua 600ml Dingin",
            barcode = "8999999999",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "botol",
            stockUnit = "botol",
            currentPrice = 4500L,
            user = adminUser
        )

        assertTrue(duplicateResult is Result.Error)
    }

    @Test
    fun nonAdmin_cannotCreateProduct() = runTest {
        val result = createProductUseCase(
            categoryId = "cat-1",
            name = "Indomie Soto",
            barcode = "8991111111",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 3500L,
            user = cashierUser
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.Unauthorized)
    }

    @Test
    fun nonAdmin_cannotUpdateProduct() = runTest {
        val createResult = createProductUseCase(
            categoryId = "cat-1",
            name = "Teh Kotak",
            barcode = "8991112223",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "kotak",
            stockUnit = "kotak",
            currentPrice = 4000L,
            user = adminUser
        ) as Result.Success

        val updateResult = updateProductUseCase(createResult.data.copy(name = "Teh Kotak Dingin"), cashierUser)
        assertTrue(updateResult is Result.Error)
        assertTrue((updateResult as Result.Error).error is AppError.Unauthorized)
    }

    @Test
    fun nonAdmin_cannotDeactivateOrActivateProduct() = runTest {
        val createResult = createProductUseCase(
            categoryId = "cat-1",
            name = "Susu UHT",
            barcode = "8991113334",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "kotak",
            stockUnit = "kotak",
            currentPrice = 6000L,
            user = adminUser
        ) as Result.Success

        val deactResult = deactivateProductUseCase(createResult.data.productId, cashierUser)
        assertTrue(deactResult is Result.Error)
        assertTrue((deactResult as Result.Error).error is AppError.Unauthorized)

        val actResult = activateProductUseCase(createResult.data.productId, cashierUser)
        assertTrue(actResult is Result.Error)
        assertTrue((actResult as Result.Error).error is AppError.Unauthorized)
    }

    @Test
    fun invalidProductCombinations_areRejected() = runTest {
        // Per Kg must use GRAM quantity type
        val perKgInvalid = createProductUseCase(
            categoryId = "cat-1",
            name = "Gula Pasir Curah",
            barcode = null,
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_KG,
            quantityType = QuantityType.COUNT, // INVALID: should be GRAM
            sellingUnit = "kg",
            stockUnit = "kg",
            currentPrice = 17000L,
            user = adminUser
        )
        assertTrue(perKgInvalid is Result.Error)

        // Per Unit must use COUNT quantity type
        val perUnitInvalid = createProductUseCase(
            categoryId = "cat-1",
            name = "Indomie Goreng",
            barcode = null,
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.GRAM, // INVALID: should be COUNT
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 3500L,
            user = adminUser
        )
        assertTrue(perUnitInvalid is Result.Error)

        // Digital product cannot be PER_KG
        val digitalPerKg = createProductUseCase(
            categoryId = "cat-1",
            name = "Pulsa 10k",
            barcode = null,
            productKind = ProductKind.DIGITAL,
            pricingMethod = PricingMethod.PER_KG, // INVALID
            quantityType = QuantityType.GRAM,
            sellingUnit = "voucher",
            stockUnit = "voucher",
            currentPrice = 11000L,
            user = adminUser
        )
        assertTrue(digitalPerKg is Result.Error)
    }

    @Test
    fun updateProduct_success() = runTest {
        val createResult = createProductUseCase(
            categoryId = "cat-1",
            name = "Teh Pucuk",
            barcode = "8992222222",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "botol",
            stockUnit = "botol",
            currentPrice = 4000L,
            user = adminUser
        ) as Result.Success

        val existing = createResult.data
        val updated = existing.copy(name = "Teh Pucuk Harum 350ml")

        val updateResult = updateProductUseCase(updated, adminUser)
        assertTrue(updateResult is Result.Success)
        assertEquals("Teh Pucuk Harum 350ml", (updateResult as Result.Success).data.name)
    }

    @Test
    fun deactivateProduct_setsIsActiveFalse_doesNotPhysicallyDelete() = runTest {
        val createResult = createProductUseCase(
            categoryId = "cat-1",
            name = "Barang Discontinue",
            barcode = "8993333333",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 5000L,
            user = adminUser
        ) as Result.Success

        val productId = createResult.data.productId

        val deactivateResult = deactivateProductUseCase(productId, adminUser)
        assertTrue(deactivateResult is Result.Success)

        val productInDb = productRepository.getProductById(productId)
        assertNotNull(productInDb)
        assertFalse(productInDb!!.isActive)

        val activateResult = activateProductUseCase(productId, adminUser)
        assertTrue(activateResult is Result.Success)
        assertTrue(productRepository.getProductById(productId)!!.isActive)
    }

    @Test
    fun inactiveProduct_cannotBeLookedUpForNewActiveTransactions() = runTest {
        val createResult = createProductUseCase(
            categoryId = "cat-1",
            name = "Barang Nonaktif",
            barcode = "8994444444",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 10000L,
            user = adminUser
        ) as Result.Success

        val productId = createResult.data.productId
        deactivateProductUseCase(productId, adminUser)

        val activeLookup = getProductByBarcodeUseCase("8994444444", onlyActive = true)
        assertNull(activeLookup)

        val inactiveLookup = getProductByBarcodeUseCase("8994444444", onlyActive = false)
        assertNotNull(inactiveLookup)
    }

    @Test
    fun searchProducts_filtersByNameAndBarcode() = runTest {
        createProductUseCase(
            categoryId = "cat-1",
            name = "Sari Roti Tawar",
            barcode = "8995551111",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "bks",
            stockUnit = "bks",
            currentPrice = 15000L,
            user = adminUser
        )

        createProductUseCase(
            categoryId = "cat-1",
            name = "Sari Gandum",
            barcode = "8995552222",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "bks",
            stockUnit = "bks",
            currentPrice = 8000L,
            user = adminUser
        )

        val searchName = searchProductsUseCase("Roti").first()
        assertEquals(1, searchName.size)
        assertEquals("Sari Roti Tawar", searchName[0].name)

        val searchBarcode = searchProductsUseCase("5552222").first()
        assertEquals(1, searchBarcode.size)
        assertEquals("Sari Gandum", searchBarcode[0].name)
    }
}
