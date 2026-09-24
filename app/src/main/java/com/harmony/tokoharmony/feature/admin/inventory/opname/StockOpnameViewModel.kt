package com.harmony.tokoharmony.feature.admin.inventory.opname

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.repository.StockRepository
import com.harmony.tokoharmony.domain.usecase.auth.GetAdminUserUseCase
import com.harmony.tokoharmony.domain.usecase.inventory.RecordStockAdjustmentUseCase
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
class StockOpnameViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val stockRepository: StockRepository,
    private val recordStockAdjustmentUseCase: RecordStockAdjustmentUseCase,
    private val getAdminUserUseCase: GetAdminUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockOpnameUiState())
    val uiState: StateFlow<StockOpnameUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<StockOpnameEvent>()
    val eventFlow: SharedFlow<StockOpnameEvent> = _eventFlow.asSharedFlow()

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
        viewModelScope.launch {
            val systemStock = stockRepository.getCurrentStock(product.productId)
            _uiState.update {
                it.copy(
                    selectedProduct = product,
                    systemStock = systemStock,
                    physicalStockInput = systemStock.toString(),
                    selectedReason = "Stock opname",
                    noteInput = "",
                    errorMessage = null
                )
            }
        }
    }

    fun clearSelectedProduct() {
        _uiState.update {
            it.copy(
                selectedProduct = null,
                physicalStockInput = "",
                systemStock = 0L,
                noteInput = "",
                errorMessage = null
            )
        }
    }

    fun onPhysicalStockChanged(input: String) {
        _uiState.update { it.copy(physicalStockInput = input, errorMessage = null) }
    }

    fun onReasonSelected(reason: String) {
        _uiState.update { it.copy(selectedReason = reason) }
    }

    fun onNoteChanged(input: String) {
        _uiState.update { it.copy(noteInput = input) }
    }

    fun saveAdjustment() {
        val selectedProduct = _uiState.value.selectedProduct ?: return
        val physicalQty = _uiState.value.physicalStockInput.trim().toLongOrNull()
        val reason = _uiState.value.selectedReason
        val note = _uiState.value.noteInput.trim().ifBlank { null }

        if (physicalQty == null || physicalQty < 0) {
            _uiState.update { it.copy(errorMessage = "Masukkan jumlah stok fisik yang valid (angka bulat >= 0).") }
            return
        }

        if (selectedProduct.quantityType == QuantityType.GRAM && physicalQty % 500L != 0L) {
            _uiState.update { it.copy(errorMessage = "Untuk produk timbang, stok fisik harus kelipatan 500 gram (0,5 kg).") }
            return
        }

        if (reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Alasan penyesuaian wajib dipilih.") }
            return
        }

        viewModelScope.launch {
            val admin = getAdminUserUseCase()
            if (admin == null) {
                _uiState.update { it.copy(errorMessage = "Pengguna Admin tidak ditemukan.") }
                return@launch
            }

            _uiState.update { it.copy(isSubmitting = true) }
            when (val result = recordStockAdjustmentUseCase(
                productId = selectedProduct.productId,
                physicalQuantity = physicalQty,
                reason = reason,
                note = note,
                user = admin
            )) {
                is Result.Success -> {
                    val diff = result.data.difference
                    val diffStr = if (diff >= 0) "+$diff" else "$diff"
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            selectedProduct = null,
                            physicalStockInput = "",
                            noteInput = "",
                            successMessage = "Koreksi stok ${selectedProduct.name} tersimpan ($diffStr ${selectedProduct.stockUnit})."
                        )
                    }
                    _eventFlow.emit(StockOpnameEvent.Success("Koreksi stok berhasil disimpan."))
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
