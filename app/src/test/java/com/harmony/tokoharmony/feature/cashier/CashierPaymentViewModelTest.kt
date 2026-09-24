package com.harmony.tokoharmony.feature.cashier

import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.MovementType
import com.harmony.tokoharmony.domain.model.PaymentMethod
import com.harmony.tokoharmony.domain.model.PaymentStatus
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.StockMovement
import com.harmony.tokoharmony.domain.model.TransactionStatus
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.usecase.FakeAtomicTransactionRepository
import com.harmony.tokoharmony.domain.usecase.FakeAuthRepository
import com.harmony.tokoharmony.domain.usecase.FakePriceRepository
import com.harmony.tokoharmony.domain.usecase.FakeProductRepository
import com.harmony.tokoharmony.domain.usecase.FakeStockRepository
import com.harmony.tokoharmony.domain.usecase.auth.GetCashierUserUseCase
import com.harmony.tokoharmony.domain.usecase.cart.AddProductToDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.GetActiveDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.GetOrCreateDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.payment.CalculateCashChangeUseCase
import com.harmony.tokoharmony.domain.usecase.payment.CompleteCashTransactionUseCase
import com.harmony.tokoharmony.domain.usecase.payment.CompleteQrisTransactionUseCase
import com.harmony.tokoharmony.domain.usecase.product.CreateProductUseCase
import com.harmony.tokoharmony.feature.cashier.payment.CashierPaymentEvent
import com.harmony.tokoharmony.feature.cashier.payment.CashierPaymentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class CashierPaymentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var productRepository: FakeProductRepository
    private lateinit var priceRepository: FakePriceRepository
    private lateinit var stockRepository: FakeStockRepository
    private lateinit var transactionRepository: FakeAtomicTransactionRepository

    private lateinit var createProductUseCase: CreateProductUseCase
    private lateinit var getActiveDraftCartUseCase: GetActiveDraftCartUseCase
    private lateinit var getOrCreateDraftCartUseCase: GetOrCreateDraftCartUseCase
    private lateinit var addProductToDraftCartUseCase: AddProductToDraftCartUseCase
    private lateinit var calculateCashChangeUseCase: CalculateCashChangeUseCase
    private lateinit var completeCashTransactionUseCase: CompleteCashTransactionUseCase
    private lateinit var completeQrisTransactionUseCase: CompleteQrisTransactionUseCase
    private lateinit var getCashierUserUseCase: GetCashierUserUseCase

    private val adminUser = User("admin-1", "Admin", Role.ADMIN, "hash")
    private val cashierUser = User("cashier-1", "Kasir", Role.CASHIER, null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        productRepository = FakeProductRepository()
        priceRepository = FakePriceRepository(productRepository)
        stockRepository = FakeStockRepository()
        transactionRepository = FakeAtomicTransactionRepository(productRepository, stockRepository)

        createProductUseCase = CreateProductUseCase(productRepository)
        getActiveDraftCartUseCase = GetActiveDraftCartUseCase(transactionRepository)
        getOrCreateDraftCartUseCase = GetOrCreateDraftCartUseCase(transactionRepository)
        addProductToDraftCartUseCase = AddProductToDraftCartUseCase(transactionRepository)
        calculateCashChangeUseCase = CalculateCashChangeUseCase()
        completeCashTransactionUseCase = CompleteCashTransactionUseCase(transactionRepository)
        completeQrisTransactionUseCase = CompleteQrisTransactionUseCase(transactionRepository)

        val authRepository = FakeAuthRepository()
        getCashierUserUseCase = GetCashierUserUseCase(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun cashPaymentFlow_calculatesChangeAndCompletes() = runTest {
        val prod = (createProductUseCase(
            categoryId = "cat-1",
            name = "Beras 5kg",
            barcode = "8990001",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "sak",
            stockUnit = "sak",
            currentPrice = 65000L,
            user = adminUser
        ) as Result.Success).data
        stockRepository.insertStockMovement(
            StockMovement(
                movementId = UUID.randomUUID().toString(),
                productId = prod.productId,
                movementType = MovementType.INITIAL_STOCK,
                quantityDelta = 10L,
                createdAt = System.currentTimeMillis(),
                createdBy = adminUser.userId
            )
        )

        val draft = getOrCreateDraftCartUseCase(cashierUser.userId)
        addProductToDraftCartUseCase(prod.productId, 1L, cashierUser.userId)

        val viewModel = CashierPaymentViewModel(
            getActiveDraftCartUseCase = getActiveDraftCartUseCase,
            calculateCashChangeUseCase = calculateCashChangeUseCase,
            completeCashTransactionUseCase = completeCashTransactionUseCase,
            completeQrisTransactionUseCase = completeQrisTransactionUseCase,
            getCashierUserUseCase = getCashierUserUseCase
        )

        advanceUntilIdle()

        // Verify loaded cart
        assertEquals(65000L, viewModel.uiState.value.cart?.totalAmount)
        assertNull(viewModel.uiState.value.selectedPaymentMethod)

        // Select Cash
        viewModel.selectPaymentMethod(PaymentMethod.CASH)
        assertEquals(PaymentMethod.CASH, viewModel.uiState.value.selectedPaymentMethod)

        // Type insufficient amount: 50000
        viewModel.onAmountReceivedChanged("50000")
        assertEquals(50000L, viewModel.uiState.value.amountReceived)
        assertFalse(viewModel.uiState.value.isPaymentValid)
        assertEquals(15000L, viewModel.uiState.value.shortageAmount)

        // Type valid amount: 100000
        viewModel.onAmountReceivedChanged("100000")
        assertEquals(100000L, viewModel.uiState.value.amountReceived)
        assertTrue(viewModel.uiState.value.isPaymentValid)
        assertEquals(35000L, viewModel.uiState.value.changeAmount)
        assertNull(viewModel.uiState.value.shortageAmount)

        // Complete Cash Payment
        var successEvent: CashierPaymentEvent.PaymentSuccess? = null
        val collectJob = launch {
            viewModel.eventFlow.collect { event ->
                if (event is CashierPaymentEvent.PaymentSuccess) {
                    successEvent = event
                }
            }
        }

        viewModel.completeCashPayment()
        advanceUntilIdle()

        assertNotNull(successEvent)
        assertEquals(draft.transaction.transactionId, successEvent?.transactionId)
        collectJob.cancel()
    }

    @Test
    fun qrisPaymentFlow_completesOnlyAfterExplicitCashierConfirmation() = runTest {
        val prod = (createProductUseCase(
            categoryId = "cat-1",
            name = "Minyak Goreng",
            barcode = "8990002",
            productKind = ProductKind.PHYSICAL,
            pricingMethod = PricingMethod.PER_UNIT,
            quantityType = QuantityType.COUNT,
            sellingUnit = "btl",
            stockUnit = "btl",
            currentPrice = 18000L,
            user = adminUser
        ) as Result.Success).data
        stockRepository.insertStockMovement(
            StockMovement(
                movementId = UUID.randomUUID().toString(),
                productId = prod.productId,
                movementType = MovementType.INITIAL_STOCK,
                quantityDelta = 10L,
                createdAt = System.currentTimeMillis(),
                createdBy = adminUser.userId
            )
        )

        val draft = getOrCreateDraftCartUseCase(cashierUser.userId)
        addProductToDraftCartUseCase(prod.productId, 1L, cashierUser.userId)

        val viewModel = CashierPaymentViewModel(
            getActiveDraftCartUseCase = getActiveDraftCartUseCase,
            calculateCashChangeUseCase = calculateCashChangeUseCase,
            completeCashTransactionUseCase = completeCashTransactionUseCase,
            completeQrisTransactionUseCase = completeQrisTransactionUseCase,
            getCashierUserUseCase = getCashierUserUseCase
        )

        advanceUntilIdle()

        // Select QRIS
        viewModel.selectPaymentMethod(PaymentMethod.QRIS)
        assertEquals(PaymentMethod.QRIS, viewModel.uiState.value.selectedPaymentMethod)

        // Confirm QRIS Payment
        var successEvent: CashierPaymentEvent.PaymentSuccess? = null
        val collectJob = launch {
            viewModel.eventFlow.collect { event ->
                if (event is CashierPaymentEvent.PaymentSuccess) {
                    successEvent = event
                }
            }
        }

        viewModel.completeQrisPayment()
        advanceUntilIdle()

        assertNotNull(successEvent)
        assertEquals(draft.transaction.transactionId, successEvent?.transactionId)

        val tx = transactionRepository.getTransactionById(draft.transaction.transactionId)!!
        assertEquals(TransactionStatus.COMPLETED, tx.transactionStatus)
        assertEquals(PaymentStatus.PAID, tx.paymentStatus)
        assertEquals(PaymentMethod.QRIS, tx.paymentMethod)
        assertEquals(18000L, tx.amountReceived)
        assertEquals(0L, tx.changeAmount)

        collectJob.cancel()
    }
}
