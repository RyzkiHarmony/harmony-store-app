package com.harmony.tokoharmony.feature.admin.inventory.stockin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.repository.StockRepository
import com.harmony.tokoharmony.domain.usecase.auth.GetAdminUserUseCase
import com.harmony.tokoharmony.domain.usecase.inventory.RecordStockInUseCase
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
class StockInViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val stockRepository: StockRepository,
    private val recordStockInUseCase: RecordStockInUseCase,
    private val getAdminUserUseCase: GetAdminUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockInUiState())
    val uiState: StateFlow<StockInUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<StockInEvent>()
    val eventFlow: SharedFlow<StockInEvent> = _eventFlow.asSharedFlow()

    init {
        loadPhysicalProducts()
    }

    fun loadPhysicalProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getProductsUseCase(onlyActive = true).collect { products ->
                val physicalProducts = products.filter { it.productKind == ProductKind.PHYSICAL }
                val stockMap = mutableMapOf<String, Long>()

                for (p in physicalProducts) {
                    stockMap[p.productId] = stockRepository.getCurrentStock(p.productId)
                }

                _uiState.update {
                    it.copy(
                        products = physicalProducts,
                        stockMap = stockMap,
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
                purchaseUnitInput = product.purchaseUnit ?: product.stockUnit,
                conversionFactorInput = (product.purchaseConversionFactor ?: 1L).toString(),
                purchaseQuantityInput = "",
                noteInput = "",
                errorMessage = null
            )
        }
    }

    fun clearSelectedProduct() {
        _uiState.update {
            it.copy(
                selectedProduct = null,
                purchaseQuantityInput = "",
                purchaseUnitInput = "",
                conversionFactorInput = "1",
                noteInput = "",
                errorMessage = null
            )
        }
    }

    fun onPurchaseQuantityChanged(input: String) {
        _uiState.update { it.copy(purchaseQuantityInput = input, errorMessage = null) }
    }

    fun onPurchaseUnitChanged(input: String) {
        _uiState.update { it.copy(purchaseUnitInput = input, errorMessage = null) }
    }

    fun onConversionFactorChanged(input: String) {
        _uiState.update { it.copy(conversionFactorInput = input, errorMessage = null) }
    }

    fun onNoteChanged(input: String) {
        _uiState.update { it.copy(noteInput = input) }
    }

    fun saveStockIn() {
        val selectedProduct = _uiState.value.selectedProduct ?: return
        val purchaseQty = _uiState.value.purchaseQuantityInput.trim().toLongOrNull()
        val conversion = _uiState.value.conversionFactorInput.trim().toLongOrNull()
        val unit = _uiState.value.purchaseUnitInput.trim()
        val note = _uiState.value.noteInput.trim().ifBlank { null }

        if (purchaseQty == null || purchaseQty <= 0) {
            _uiState.update { it.copy(errorMessage = "Jumlah pembelian harus lebih dari 0.") }
            return
        }

        if (conversion == null || conversion <= 0) {
            _uiState.update { it.copy(errorMessage = "Faktor konversi harus lebih dari 0.") }
            return
        }

        if (unit.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Satuan pembelian wajib diisi.") }
            return
        }

        viewModelScope.launch {
            val admin = getAdminUserUseCase()
            if (admin == null) {
                _uiState.update { it.copy(errorMessage = "Pengguna Admin tidak ditemukan.") }
                return@launch
            }

            _uiState.update { it.copy(isSubmitting = true) }
            when (val result = recordStockInUseCase(
                productId = selectedProduct.productId,
                purchaseQuantity = purchaseQty,
                purchaseUnit = unit,
                conversionFactor = conversion,
                note = note,
                user = admin
            )) {
                is Result.Success -> {
                    val totalAdded = result.data.stockQuantity
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            selectedProduct = null,
                            purchaseQuantityInput = "",
                            noteInput = "",
                            successMessage = "Berhasil mencatat barang masuk: +$totalAdded ${selectedProduct.stockUnit} untuk ${selectedProduct.name}."
                        )
                    }
                    _eventFlow.emit(StockInEvent.Success("Barang masuk berhasil dicatat."))
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
