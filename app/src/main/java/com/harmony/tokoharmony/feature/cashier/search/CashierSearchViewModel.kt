package com.harmony.tokoharmony.feature.cashier.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.usecase.auth.GetCashierUserUseCase
import com.harmony.tokoharmony.domain.usecase.cart.AddProductToDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.product.SearchProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CashierSearchUiState(
    val searchQuery: String = "",
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val selectedWeightProduct: Product? = null,
    val errorMessage: String? = null
)

sealed interface CashierSearchNavigationEvent {
    data object NavigateBackToCart : CashierSearchNavigationEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CashierSearchViewModel @Inject constructor(
    private val searchProductsUseCase: SearchProductsUseCase,
    private val addProductToDraftCartUseCase: AddProductToDraftCartUseCase,
    private val getCashierUserUseCase: GetCashierUserUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _uiState = MutableStateFlow(CashierSearchUiState())
    val uiState: StateFlow<CashierSearchUiState> = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<CashierSearchNavigationEvent>()
    val navEvents: SharedFlow<CashierSearchNavigationEvent> = _navEvents.asSharedFlow()

    init {
        observeSearch()
    }

    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery.flatMapLatest { query ->
                searchProductsUseCase(query = query, onlyActive = true)
            }.collect { productList ->
                _uiState.update {
                    it.copy(
                        products = productList,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query, isLoading = true) }
    }

    fun onProductSelected(product: Product) {
        if (product.pricingMethod == PricingMethod.PER_KG || product.quantityType == QuantityType.GRAM) {
            _uiState.update { it.copy(selectedWeightProduct = product) }
        } else {
            addProductToCart(product.productId, 1L)
        }
    }

    fun onWeightConfirmed(grams: Long) {
        val product = _uiState.value.selectedWeightProduct ?: return
        _uiState.update { it.copy(selectedWeightProduct = null) }
        addProductToCart(product.productId, grams)
    }

    fun dismissWeightDialog() {
        _uiState.update { it.copy(selectedWeightProduct = null) }
    }

    private fun addProductToCart(productId: String, quantity: Long) {
        viewModelScope.launch {
            val user = getCashierUserUseCase()
            val result = addProductToDraftCartUseCase(productId, quantity, user.userId)
            if (result is Result.Success) {
                _navEvents.emit(CashierSearchNavigationEvent.NavigateBackToCart)
            } else if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
