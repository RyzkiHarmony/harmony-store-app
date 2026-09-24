package com.harmony.tokoharmony.feature.admin.inventory.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.usecase.auth.GetAdminUserUseCase
import com.harmony.tokoharmony.domain.usecase.inventory.GetStockMovementsUseCase
import com.harmony.tokoharmony.domain.usecase.product.GetProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockHistoryViewModel @Inject constructor(
    private val getStockMovementsUseCase: GetStockMovementsUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val getAdminUserUseCase: GetAdminUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockHistoryUiState())
    val uiState: StateFlow<StockHistoryUiState> = _uiState.asStateFlow()

    init {
        loadMovements()
    }

    fun loadMovements() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val admin = getAdminUserUseCase()
            if (admin == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Pengguna Admin tidak ditemukan.") }
                return@launch
            }

            val products = getProductsUseCase(onlyActive = false).firstOrNull() ?: emptyList()
            val pMap = products.associateBy { it.productId }
            _uiState.update { it.copy(productsMap = pMap) }

            when (val result = getStockMovementsUseCase(user = admin)) {
                is Result.Success -> {
                    result.data.collect { movements ->
                        _uiState.update {
                            it.copy(
                                movements = movements,
                                isLoading = false
                            )
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.error.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }
}
