package com.harmony.tokoharmony.feature.cashier.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.DraftCart
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.TransactionItem
import com.harmony.tokoharmony.domain.usecase.auth.GetCashierUserUseCase
import com.harmony.tokoharmony.domain.usecase.cart.AddProductToDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.ClearDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.GetActiveDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.GetOrCreateDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.cart.RemoveDraftCartItemUseCase
import com.harmony.tokoharmony.domain.usecase.cart.UpdateDraftCartItemQuantityUseCase
import com.harmony.tokoharmony.domain.usecase.product.GetProductByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.harmony.tokoharmony.domain.model.DigitalTransaction
import com.harmony.tokoharmony.domain.usecase.digital.GetDigitalTransactionByItemUseCase
import com.harmony.tokoharmony.domain.usecase.digital.RecordDigitalTransactionUseCase

data class CashierCartUiState(
    val isLoading: Boolean = false,
    val cart: DraftCart? = null,
    val errorMessage: String? = null,
    val weightEditItem: Pair<Product, TransactionItem>? = null,
    val digitalEditItem: Pair<Product, TransactionItem>? = null,
    val productsMap: Map<String, Product> = emptyMap(),
    val digitalTransactionsMap: Map<String, DigitalTransaction> = emptyMap()
)

@HiltViewModel
class CashierCartViewModel @Inject constructor(
    private val getActiveDraftCartUseCase: GetActiveDraftCartUseCase,
    private val getOrCreateDraftCartUseCase: GetOrCreateDraftCartUseCase,
    private val addProductToDraftCartUseCase: AddProductToDraftCartUseCase,
    private val updateDraftCartItemQuantityUseCase: UpdateDraftCartItemQuantityUseCase,
    private val removeDraftCartItemUseCase: RemoveDraftCartItemUseCase,
    private val clearDraftCartUseCase: ClearDraftCartUseCase,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val getCashierUserUseCase: GetCashierUserUseCase,
    private val recordDigitalTransactionUseCase: RecordDigitalTransactionUseCase,
    private val getDigitalTransactionByItemUseCase: GetDigitalTransactionByItemUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashierCartUiState(isLoading = true))
    val uiState: StateFlow<CashierCartUiState> = _uiState.asStateFlow()

    init {
        observeDraftCart()
    }

    private fun observeDraftCart() {
        viewModelScope.launch {
            val user = getCashierUserUseCase()
            getOrCreateDraftCartUseCase(user.userId)

            getActiveDraftCartUseCase().collect { draftCart ->
                if (draftCart != null) {
                    val pMap = mutableMapOf<String, Product>()
                    val dtMap = mutableMapOf<String, DigitalTransaction>()
                    for (item in draftCart.items) {
                        val product = getProductByIdUseCase(item.productId)
                        if (product != null) {
                            pMap[item.productId] = product
                            val dt = getDigitalTransactionByItemUseCase(item.itemId)
                            if (dt != null) {
                                dtMap[item.itemId] = dt
                            }
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            cart = draftCart,
                            productsMap = pMap,
                            digitalTransactionsMap = dtMap
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            cart = null,
                            productsMap = emptyMap(),
                            digitalTransactionsMap = emptyMap()
                        )
                    }
                }
            }
        }
    }

    fun incrementQuantity(item: TransactionItem) {
        viewModelScope.launch {
            val result = updateDraftCartItemQuantityUseCase(item.itemId, item.quantity + 1)
            if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun decrementQuantity(item: TransactionItem) {
        viewModelScope.launch {
            if (item.quantity > 1) {
                val result = updateDraftCartItemQuantityUseCase(item.itemId, item.quantity - 1)
                if (result is Result.Error) {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            } else {
                removeItem(item)
            }
        }
    }

    fun removeItem(item: TransactionItem) {
        viewModelScope.launch {
            val result = removeDraftCartItemUseCase(item.itemId)
            if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun openWeightEditDialog(item: TransactionItem) {
        viewModelScope.launch {
            val product = getProductByIdUseCase(item.productId)
            if (product != null) {
                _uiState.update { it.copy(weightEditItem = Pair(product, item)) }
            }
        }
    }

    fun saveWeightEdit(grams: Long) {
        val currentItem = _uiState.value.weightEditItem?.second ?: return
        viewModelScope.launch {
            val result = updateDraftCartItemQuantityUseCase(currentItem.itemId, grams)
            _uiState.update { it.copy(weightEditItem = null) }
            if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun dismissWeightEditDialog() {
        _uiState.update { it.copy(weightEditItem = null) }
    }

    fun clearCart() {
        val cart = _uiState.value.cart ?: return
        viewModelScope.launch {
            val result = clearDraftCartUseCase(cart.transaction.transactionId)
            if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun autoAddProductById(productId: String, quantity: Long = 1L) {
        viewModelScope.launch {
            val user = getCashierUserUseCase()
            addProductToDraftCartUseCase(productId, quantity, user.userId)
        }
    }

    fun openDigitalEditDialog(item: TransactionItem) {
        viewModelScope.launch {
            val product = getProductByIdUseCase(item.productId)
            if (product != null) {
                _uiState.update { it.copy(digitalEditItem = Pair(product, item)) }
            }
        }
    }

    fun saveDigitalTransaction(
        serviceType: String,
        customerNumber: String,
        nominal: Long,
        providerRef: String?,
        status: String
    ) {
        val currentItem = _uiState.value.digitalEditItem?.second ?: return
        viewModelScope.launch {
            val result = recordDigitalTransactionUseCase(
                transactionItemId = currentItem.itemId,
                serviceType = serviceType,
                customerNumber = customerNumber,
                nominal = nominal,
                providerReference = providerRef,
                status = status
            )
            when (result) {
                is Result.Success -> {
                    val updatedMap = _uiState.value.digitalTransactionsMap.toMutableMap()
                    updatedMap[currentItem.itemId] = result.data
                    _uiState.update {
                        it.copy(
                            digitalEditItem = null,
                            digitalTransactionsMap = updatedMap
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun dismissDigitalEditDialog() {
        _uiState.update { it.copy(digitalEditItem = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
