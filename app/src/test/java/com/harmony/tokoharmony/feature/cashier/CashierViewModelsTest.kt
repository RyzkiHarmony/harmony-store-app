package com.harmony.tokoharmony.feature.cashier

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.DraftCart
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.TransactionItem
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.usecase.FakeAuthRepository
import com.harmony.tokoharmony.domain.usecase.FakePriceRepository
import com.harmony.tokoharmony.domain.usecase.FakeProductRepository
import com.harmony.tokoharmony.domain.usecase.FakeTransactionRepository
import com.harmony.tokoharmony.domain.usecase.auth.GetCashierUserUseCase
import com.harmony.tokoharmony.domain.usecase.cart.AddProductToDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.ClearDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.GetActiveDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.GetOrCreateDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.RemoveDraftCartItemUseCase
import com.harmony.tokoharmony.domain.usecase.cart.UpdateDraftCartItemQuantityUseCase
import com.harmony.tokoharmony.domain.usecase.product.CreateProductUseCase
import com.harmony.tokoharmony.domain.usecase.product.GetProductByBarcodeUseCase
import com.harmony.tokoharmony.domain.usecase.product.GetProductByIdUseCase
import com.harmony.tokoharmony.domain.usecase.product.SearchProductsUseCase
import com.harmony.tokoharmony.feature.cashier.cart.CashierCartViewModel
import com.harmony.tokoharmony.feature.cashier.scanner.BarcodeScannerNavigationEvent
import com.harmony.tokoharmony.feature.cashier.scanner.BarcodeScannerViewModel
import com.harmony.tokoharmony.feature.cashier.search.CashierSearchNavigationEvent
import com.harmony.tokoharmony.feature.cashier.search.CashierSearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CashierViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var productRepository: FakeProductRepository
    private lateinit var priceRepository: FakePriceRepository
    private lateinit var transactionRepository: FakeTransactionRepository

    private lateinit var createProductUseCase: CreateProductUseCase
    private lateinit var getProductByIdUseCase: GetProductByIdUseCase
    private lateinit var getProductByBarcodeUseCase: GetProductByBarcodeUseCase
    private lateinit var searchProductsUseCase: SearchProductsUseCase

    private lateinit var getActiveDraftCartUseCase: GetActiveDraftCartUseCase
    private lateinit var getOrCreateDraftCartUseCase: GetOrCreateDraftCartUseCase
    private lateinit var addProductToDraftCartUseCase: AddProductToDraftCartUseCase
    private lateinit var updateDraftCartItemQuantityUseCase: UpdateDraftCartItemQuantityUseCase
    private lateinit var removeDraftCartItemUseCase: RemoveDraftCartItemUseCase
    private lateinit var clearDraftCartUseCase: ClearDraftCartUseCase
    private lateinit var getCashierUserUseCase: GetCashierUserUseCase

    private val adminUser = User("admin-1", "Admin", Role.ADMIN, "hash")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        productRepository = FakeProductRepository()
        priceRepository = FakePriceRepository(productRepository)
        transactionRepository = FakeTransactionRepository(productRepository)

        createProductUseCase = CreateProductUseCase(productRepository)
        getProductByIdUseCase = GetProductByIdUseCase(productRepository)
        getProductByBarcodeUseCase = GetProductByBarcodeUseCase(productRepository)
        searchProductsUseCase = SearchProductsUseCase(productRepository)

        getActiveDraftCartUseCase = GetActiveDraftCartUseCase(transactionRepository)
        getOrCreateDraftCartUseCase = GetOrCreateDraftCartUseCase(transactionRepository)
        addProductToDraftCartUseCase = AddProductToDraftCartUseCase(transactionRepository)
        updateDraftCartItemQuantityUseCase = UpdateDraftCartItemQuantityUseCase(transactionRepository)
        removeDraftCartItemUseCase = RemoveDraftCartItemUseCase(transactionRepository)
        clearDraftCartUseCase = ClearDraftCartUseCase(transactionRepository)
        val authRepository = FakeAuthRepository()
        getCashierUserUseCase = GetCashierUserUseCase(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun cashierCartViewModel_initializesActiveDraft_andAllowsQuantityEdits() = runTest {
        // Prepare product
        val prod = (createProductUseCase(
            categoryId = "cat-1",
            name = "Minyak Goreng 1L",
            barcode = "8990001",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "btl",
            stockUnit = "btl",
            currentPrice = 18000L,
            user = adminUser
        ) as Result.Success).data

        val fakeDigitalRepository = com.harmony.tokoharmony.domain.usecase.digital.FakeDigitalTransactionRepository()
        val recordDigitalTransactionUseCase = com.harmony.tokoharmony.domain.usecase.digital.RecordDigitalTransactionUseCase(fakeDigitalRepository)
        val getDigitalTransactionByItemUseCase = com.harmony.tokoharmony.domain.usecase.digital.GetDigitalTransactionByItemUseCase(fakeDigitalRepository)

        val viewModel = CashierCartViewModel(
            getActiveDraftCartUseCase = getActiveDraftCartUseCase,
            getOrCreateDraftCartUseCase = getOrCreateDraftCartUseCase,
            addProductToDraftCartUseCase = addProductToDraftCartUseCase,
            updateDraftCartItemQuantityUseCase = updateDraftCartItemQuantityUseCase,
            removeDraftCartItemUseCase = removeDraftCartItemUseCase,
            clearDraftCartUseCase = clearDraftCartUseCase,
            getProductByIdUseCase = getProductByIdUseCase,
            getCashierUserUseCase = getCashierUserUseCase,
            recordDigitalTransactionUseCase = recordDigitalTransactionUseCase,
            getDigitalTransactionByItemUseCase = getDigitalTransactionByItemUseCase
        )

        advanceUntilIdle()

        // Cart is initially empty
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.cart)
        assertTrue(viewModel.uiState.value.cart!!.isEmpty)

        // Auto-add product
        viewModel.autoAddProductById(prod.productId, 1L)
        advanceUntilIdle()

        val cart = viewModel.uiState.value.cart!!
        assertEquals(1, cart.itemCount)
        assertEquals(18000L, cart.totalAmount)

        val item = cart.items[0]

        // Increment
        viewModel.incrementQuantity(item)
        advanceUntilIdle()
        assertEquals(2L, viewModel.uiState.value.cart!!.items[0].quantity)
        assertEquals(36000L, viewModel.uiState.value.cart!!.totalAmount)

        // Decrement
        viewModel.decrementQuantity(viewModel.uiState.value.cart!!.items[0])
        advanceUntilIdle()
        assertEquals(1L, viewModel.uiState.value.cart!!.items[0].quantity)
        assertEquals(18000L, viewModel.uiState.value.cart!!.totalAmount)

        // Decrementing from 1 removes item
        viewModel.decrementQuantity(viewModel.uiState.value.cart!!.items[0])
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.cart!!.isEmpty)
        assertEquals(0L, viewModel.uiState.value.cart!!.totalAmount)
    }

    @Test
    fun cashierSearchViewModel_filtersByQuery_andHandlesWeightSelection() = runTest {
        val countProd = (createProductUseCase(
            categoryId = "cat-1",
            name = "Kecap Manis",
            barcode = "8990002",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "btl",
            stockUnit = "btl",
            currentPrice = 9000L,
            user = adminUser
        ) as Result.Success).data

        val gramProd = (createProductUseCase(
            categoryId = "cat-1",
            name = "Gula Pasir Curah",
            barcode = null,
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_KG,
            quantityType = QuantityType.GRAM,
            sellingUnit = "kg",
            stockUnit = "kg",
            currentPrice = 17000L,
            user = adminUser
        ) as Result.Success).data

        val viewModel = CashierSearchViewModel(
            searchProductsUseCase = searchProductsUseCase,
            addProductToDraftCartUseCase = addProductToDraftCartUseCase,
            getCashierUserUseCase = getCashierUserUseCase
        )

        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.products.size)

        // Search partial name "Kecap"
        viewModel.onSearchQueryChanged("kecap")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.products.size)
        assertEquals(countProd.productId, viewModel.uiState.value.products[0].productId)

        // Select count product triggers direct add & navigation event
        viewModel.onProductSelected(countProd)
        advanceUntilIdle()

        // Clear search query to see all
        viewModel.onSearchQueryChanged("")
        advanceUntilIdle()

        // Select gram product triggers weight dialog
        viewModel.onProductSelected(gramProd)
        assertEquals(gramProd.productId, viewModel.uiState.value.selectedWeightProduct?.productId)

        // Confirm 1500g weight
        viewModel.onWeightConfirmed(1500L)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.selectedWeightProduct)

        // Check active draft
        val cart = getActiveDraftCartUseCase().first()!!
        assertEquals(2, cart.itemCount)
        // 9000 + (1500 * 17000 / 1000 = 25500) = 34500
        assertEquals(34500L, cart.totalAmount)
    }

    @Test
    fun barcodeScannerViewModel_scansKnownBarcode_andHandlesUnknownBarcode() = runTest {
        val knownProd = (createProductUseCase(
            categoryId = "cat-1",
            name = "Susu UHT",
            barcode = "8990003",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "ktk",
            stockUnit = "ktk",
            currentPrice = 6000L,
            user = adminUser
        ) as Result.Success).data

        val viewModel = BarcodeScannerViewModel(
            getProductByBarcodeUseCase = getProductByBarcodeUseCase,
            addProductToDraftCartUseCase = addProductToDraftCartUseCase,
            getCashierUserUseCase = getCashierUserUseCase
        )

        // 1. Scan known barcode
        viewModel.onBarcodeDetected("8990003")
        advanceUntilIdle()

        val cart = getActiveDraftCartUseCase().first()!!
        assertEquals(1, cart.itemCount)
        assertEquals(6000L, cart.totalAmount)

        // 2. Scan unknown barcode
        viewModel.onBarcodeDetected("8999999999")
        advanceUntilIdle()

        assertEquals("8999999999", viewModel.uiState.value.unknownBarcode)

        // Trigger registration navigation
        viewModel.onRegisterNewProduct()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.unknownBarcode)
    }

    @Test
    fun barcodeScannerViewModel_continuousMultiScan_aggregatesCart_andTracksFeedback() = runTest {
        val prodA = (createProductUseCase(
            categoryId = "cat-1",
            name = "Mie Instan Goreng",
            barcode = "8991001",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "pcs",
            stockUnit = "pcs",
            currentPrice = 3500L,
            user = adminUser
        ) as Result.Success).data

        val prodB = (createProductUseCase(
            categoryId = "cat-1",
            name = "Kopi Sachet",
            barcode = "8991002",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "sachet",
            stockUnit = "sachet",
            currentPrice = 2000L,
            user = adminUser
        ) as Result.Success).data

        val viewModel = BarcodeScannerViewModel(
            getProductByBarcodeUseCase = getProductByBarcodeUseCase,
            addProductToDraftCartUseCase = addProductToDraftCartUseCase,
            getCashierUserUseCase = getCashierUserUseCase
        )

        // 1. Scan Item A (Mie)
        viewModel.onBarcodeDetected("8991001")
        advanceUntilIdle()
        assertEquals("Mie Instan Goreng", viewModel.uiState.value.lastScannedProductName)

        // 2. Scan Item B (Kopi) without closing scanner
        viewModel.onBarcodeDetected("8991002")
        advanceUntilIdle()
        assertEquals("Kopi Sachet", viewModel.uiState.value.lastScannedProductName)

        // 3. Scan Item A (Mie) again after item B (canProcess is true because barcode changed)
        viewModel.onBarcodeDetected("8991001")
        advanceUntilIdle()
        assertEquals("Mie Instan Goreng", viewModel.uiState.value.lastScannedProductName)

        val cart = getActiveDraftCartUseCase().first()!!
        // Distinct items/lines in cart: 2 (Mie and Kopi)
        assertEquals(2, cart.itemCount)
        assertEquals(2, cart.items.size)
        // Total aggregated quantity: 2 Mie + 1 Kopi = 3
        assertEquals(3L, cart.items.sumOf { it.quantity })
        // 2 * 3500 + 1 * 2000 = 7000 + 2000 = 9000
        assertEquals(9000L, cart.totalAmount)

        // Mie quantity should be aggregated to 2
        val mieItem = cart.items.first { it.productId == prodA.productId }
        assertEquals(2L, mieItem.quantity)

        // Clear feedback test
        viewModel.clearFeedback()
        assertNull(viewModel.uiState.value.lastScannedProductName)
    }
}
