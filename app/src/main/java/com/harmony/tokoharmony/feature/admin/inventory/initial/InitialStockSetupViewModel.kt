package com.harmony.tokoharmony.feature.admin.inventory.initial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.repository.StockRepository
import com.harmony.tokoharmony.domain.usecase.auth.GetAdminUserUseCase
import com.harmony.tokoharmony.domain.usecase.inventory.RecordInitialStockUseCase
import com.harmony.tokoharmony.domain.usecase.product.GetProductsUseCase
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

@HiltViewModel
class InitialStockSetupViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val stockRepository: StockRepository,
    private val recordInitialStockUseCase: RecordInitialStockUseCase,
    private val getAdminUserUseCase: GetAdminUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InitialStockUiState())
    val uiState: StateFlow<InitialStockUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<InitialStockEvent>()
    val eventFlow: SharedFlow<InitialStockEvent> = _eventFlow.asSharedFlow()

    init {
        loadPhysicalProducts()
    }

    fun loadPhysicalProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getProductsUseCase(onlyActive = false).collect { products ->
                val physicalProducts = products.filter { it.productKind == ProductKind.PHYSICAL }
                val stockMap = mutableMapOf<String, Long>()
                val initializedMap = mutableMapOf<String, Boolean>()

                for (p in physicalProducts) {
                    stockMap[p.productId] = stockRepository.getCurrentStock(p.productId)
                    initializedMap[p.productId] = stockRepository.hasInitialStock(p.productId)
                }

                _uiState.update {
                    it.copy(
                        products = physicalProducts,
                        stockMap = stockMap,
                        initializedMap = initializedMap,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectProduct(product: Product) {
        _uiState.update {
            it.copy(
                selectedProduct = product,
                initialStockInput = "",
                errorMessage = null
            )
        }
    }

    fun clearSelectedProduct() {
        _uiState.update {
            it.copy(
                selectedProduct = null,
                initialStockInput = "",
                errorMessage = null
            )
        }
    }

    fun onInitialStockChanged(input: String) {
        _uiState.update { it.copy(initialStockInput = input, errorMessage = null) }
    }

    fun saveInitialStock() {
        val selectedProduct = _uiState.value.selectedProduct ?: return
        val input = _uiState.value.initialStockInput.trim()

        val quantity = input.toLongOrNull()
        if (quantity == null || quantity < 0) {
            _uiState.update { it.copy(errorMessage = "Masukkan stok awal yang valid (angka bulat >= 0).") }
            return
        }

        if (selectedProduct.quantityType == QuantityType.GRAM && quantity % 500L != 0L) {
            _uiState.update { it.copy(errorMessage = "Untuk produk timbang, stok awal harus kelipatan 500 gram (0,5 kg).") }
            return
        }

        viewModelScope.launch {
            val admin = getAdminUserUseCase()
            if (admin == null) {
                _uiState.update { it.copy(errorMessage = "Pengguna Admin tidak ditemukan.") }
                return@launch
            }

            _uiState.update { it.copy(isSubmitting = true) }
            when (val result = recordInitialStockUseCase(selectedProduct.productId, quantity, admin)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            selectedProduct = null,
                            initialStockInput = "",
                            successMessage = "Stok awal untuk ${selectedProduct.name} berhasil disimpan."
                        )
                    }
                    _eventFlow.emit(InitialStockEvent.Success("Stok awal berhasil disimpan."))
                    loadPhysicalProducts()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
