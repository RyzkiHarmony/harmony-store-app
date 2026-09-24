package com.harmony.tokoharmony.feature.cashier.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.usecase.auth.GetCashierUserUseCase
import com.harmony.tokoharmony.domain.usecase.cart.AddProductToDraftCartUseCase
import com.harmony.tokoharmony.domain.usecase.product.GetProductByBarcodeUseCase
import com.harmony.tokoharmony.domain.usecase.scanner.BarcodeDebouncer
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

data class BarcodeScannerUiState(
    val isProcessing: Boolean = false,
    val unknownBarcode: String? = null,
    val selectedWeightProduct: Product? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
    val lastScannedProductName: String? = null
)

sealed interface BarcodeScannerNavigationEvent {
    data class ProductAdded(val productName: String) : BarcodeScannerNavigationEvent
    data class NavigateToRegisterProduct(val barcode: String) : BarcodeScannerNavigationEvent
}

@HiltViewModel
class BarcodeScannerViewModel @Inject constructor(
    private val getProductByBarcodeUseCase: GetProductByBarcodeUseCase,
    private val addProductToDraftCartUseCase: AddProductToDraftCartUseCase,
    private val getCashierUserUseCase: GetCashierUserUseCase
) : ViewModel() {

    val debouncer = BarcodeDebouncer(debounceWindowMs = 800L)

    private val _uiState = MutableStateFlow(BarcodeScannerUiState())
    val uiState: StateFlow<BarcodeScannerUiState> = _uiState.asStateFlow()

    private val _navEvents = MutableSharedFlow<BarcodeScannerNavigationEvent>()
    val navEvents: SharedFlow<BarcodeScannerNavigationEvent> = _navEvents.asSharedFlow()

    fun onBarcodeDetected(barcode: String) {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return

        if (!debouncer.canProcess(trimmed)) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val product = getProductByBarcodeUseCase(trimmed, onlyActive = true)

            if (product != null) {
                if (product.pricingMethod == PricingMethod.PER_KG || product.quantityType == QuantityType.GRAM) {
                    _uiState.update { it.copy(isProcessing = false, selectedWeightProduct = product) }
                } else {
                    val user = getCashierUserUseCase()
                    val result = addProductToDraftCartUseCase(product.productId, 1L, user.userId)
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            lastScannedProductName = if (result is Result.Success) product.name else null,
                            feedbackMessage = if (result is Result.Success) "Berhasil menambahkan ${product.name}" else null
                        )
                    }
                    if (result is Result.Success) {
                        _navEvents.emit(BarcodeScannerNavigationEvent.ProductAdded(product.name))
                    } else if (result is Result.Error) {
                        _uiState.update { it.copy(errorMessage = result.error.message) }
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        unknownBarcode = trimmed
                    )
                }
            }
        }
    }

    fun onWeightConfirmed(grams: Long) {
        val product = _uiState.value.selectedWeightProduct ?: return
        _uiState.update { it.copy(selectedWeightProduct = null) }
        viewModelScope.launch {
            val user = getCashierUserUseCase()
            val result = addProductToDraftCartUseCase(product.productId, grams, user.userId)
            if (result is Result.Success) {
                _uiState.update {
                    it.copy(
                        lastScannedProductName = product.name,
                        feedbackMessage = "Berhasil menambahkan ${product.name}"
                    )
                }
                _navEvents.emit(BarcodeScannerNavigationEvent.ProductAdded(product.name))
            } else if (result is Result.Error) {
                _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun dismissWeightDialog() {
        _uiState.update { it.copy(selectedWeightProduct = null) }
    }

    fun dismissUnknownBarcodeDialog() {
        _uiState.update { it.copy(unknownBarcode = null) }
    }

    fun onRegisterNewProduct() {
        val barcode = _uiState.value.unknownBarcode ?: return
        _uiState.update { it.copy(unknownBarcode = null) }
        viewModelScope.launch {
            _navEvents.emit(BarcodeScannerNavigationEvent.NavigateToRegisterProduct(barcode))
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, lastScannedProductName = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
