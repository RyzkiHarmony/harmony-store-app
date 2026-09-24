package com.harmony.tokoharmony.feature.cashier.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.DraftCart
import com.harmony.tokoharmony.domain.model.PaymentMethod
import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.usecase.auth.GetCashierUserUseCase
import com.harmony.tokoharmony.domain.usecase.cart.GetActiveDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.payment.CalculateCashChangeUseCase
import com.harmony.tokoharmony.domain.usecase.payment.CompleteCashTransactionUseCase
import com.harmony.tokoharmony.domain.usecase.payment.CompleteQrisTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CashierPaymentUiState(
    val isLoading: Boolean = false,
    val cart: DraftCart? = null,
    val selectedPaymentMethod: PaymentMethod? = null,
    val amountReceivedInput: String = "",
    val amountReceived: Long? = null,
    val changeAmount: Long = 0L,
    val shortageAmount: Long? = null,
    val isPaymentValid: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

sealed interface CashierPaymentEvent {
    data class PaymentSuccess(val transactionId: String) : CashierPaymentEvent
}

@HiltViewModel
class CashierPaymentViewModel @Inject constructor(
    private val getActiveDraftCartUseCase: GetActiveDraftCartUseCase,
    private val calculateCashChangeUseCase: CalculateCashChangeUseCase,
    private val completeCashTransactionUseCase: CompleteCashTransactionUseCase,
    private val completeQrisTransactionUseCase: CompleteQrisTransactionUseCase,
    private val getCashierUserUseCase: GetCashierUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashierPaymentUiState(isLoading = true))
    val uiState: StateFlow<CashierPaymentUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<CashierPaymentEvent>()
    val eventFlow: SharedFlow<CashierPaymentEvent> = _eventFlow.asSharedFlow()

    init {
        loadDraftCart()
    }

    private fun loadDraftCart() {
        viewModelScope.launch {
            getActiveDraftCartUseCase().collect { cart ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cart = cart
                    )
                }
            }
        }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _uiState.update {
            it.copy(
                selectedPaymentMethod = method,
                errorMessage = null
            )
        }
    }

    fun onAmountReceivedChanged(input: String) {
        val cleanDigits = input.filter { it.isDigit() }
        val parsedAmount = cleanDigits.toLongOrNull()
        val total = _uiState.value.cart?.totalAmount ?: 0L

        if (parsedAmount == null || parsedAmount == 0L) {
            _uiState.update {
                it.copy(
                    amountReceivedInput = cleanDigits,
                    amountReceived = null,
                    changeAmount = 0L,
                    shortageAmount = null,
                    isPaymentValid = false
                )
            }
            return
        }

        val changeResult = calculateCashChangeUseCase(total, parsedAmount)
        when (changeResult) {
            is Result.Success -> {
                _uiState.update {
                    it.copy(
                        amountReceivedInput = cleanDigits,
                        amountReceived = parsedAmount,
                        changeAmount = changeResult.data,
                        shortageAmount = null,
                        isPaymentValid = true,
                        errorMessage = null
                    )
                }
            }
            is Result.Error -> {
                val shortage = total - parsedAmount
                _uiState.update {
                    it.copy(
                        amountReceivedInput = cleanDigits,
                        amountReceived = parsedAmount,
                        changeAmount = 0L,
                        shortageAmount = shortage,
                        isPaymentValid = false
                    )
                }
            }
        }
    }

    fun setQuickAmount(amount: Long) {
        onAmountReceivedChanged(amount.toString())
    }

    fun completeCashPayment() {
        val cart = _uiState.value.cart ?: return
        val amountReceived = _uiState.value.amountReceived ?: return
        if (!_uiState.value.isPaymentValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val user = getCashierUserUseCase()
            val result = completeCashTransactionUseCase(
                transactionId = cart.transaction.transactionId,
                amountReceived = amountReceived,
                userId = user.userId
            )

            _uiState.update { it.copy(isSubmitting = false) }

            when (result) {
                is Result.Success -> {
                    _eventFlow.emit(CashierPaymentEvent.PaymentSuccess(result.data.transactionId))
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun completeQrisPayment() {
        val cart = _uiState.value.cart ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val user = getCashierUserUseCase()
            val result = completeQrisTransactionUseCase(
                transactionId = cart.transaction.transactionId,
                userId = user.userId
            )

            _uiState.update { it.copy(isSubmitting = false) }

            when (result) {
                is Result.Success -> {
                    _eventFlow.emit(CashierPaymentEvent.PaymentSuccess(result.data.transactionId))
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
