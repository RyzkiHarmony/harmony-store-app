package com.harmony.tokoharmony.feature.admin.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.Category
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType
import com.harmony.tokoharmony.domain.usecase.auth.GetAdminUserUseCase
import com.harmony.tokoharmony.domain.usecase.category.CreateCategoryUseCase
import com.harmony.tokoharmony.domain.usecase.category.GetCategoriesUseCase
import com.harmony.tokoharmony.domain.usecase.product.CreateProductUseCase
import com.harmony.tokoharmony.domain.usecase.product.GetProductByIdUseCase
import com.harmony.tokoharmony.domain.usecase.product.UpdateProductUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductFormUiState(
    val isEditMode: Boolean = false,
    val productId: String? = null,
    val name: String = "",
    val barcode: String = "",
    val categoryId: String = "",
    val productKind: ProductKind = ProductKind.PHYSICAL,
    val pricingMethod: PricingMethod = PricingMethod.PER_UNIT,
    val quantityType: QuantityType = QuantityType.COUNT,
    val sellingUnit: String = "pcs",
    val stockUnit: String = "pcs",
    val purchaseUnit: String = "",
    val purchaseConversionFactor: String = "",
    val currentPrice: String = "",
    val minimumStock: String = "",
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ProductFormEvent {
    data class SaveSuccess(val productId: String? = null) : ProductFormEvent
}

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val createProductUseCase: CreateProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val getAdminUserUseCase: GetAdminUserUseCase
) : ViewModel() {

    private val productIdArg: String? = savedStateHandle.get<String>("productId")
    private val barcodeArg: String? = savedStateHandle.get<String>("barcode")

    private val _uiState = MutableStateFlow(
        ProductFormUiState(
            isEditMode = !productIdArg.isNullOrBlank(),
            productId = productIdArg,
            barcode = barcodeArg.orEmpty()
        )
    )
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ProductFormEvent>()
    val eventFlow: SharedFlow<ProductFormEvent> = _eventFlow.asSharedFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val categories = getCategoriesUseCase(onlyActive = false).firstOrNull() ?: emptyList()
            var selectedCategoryId = categories.firstOrNull()?.categoryId.orEmpty()

            if (categories.isEmpty()) {
                val admin = getAdminUserUseCase()
                if (admin != null) {
                    val created = createCategoryUseCase("Umum", admin)
                    if (created is Result.Success) {
                        selectedCategoryId = created.data.categoryId
                    }
                }
            }

            val updatedCategories = getCategoriesUseCase(onlyActive = false).firstOrNull() ?: emptyList()

            if (!productIdArg.isNullOrBlank()) {
                val product = getProductByIdUseCase(productIdArg)
                if (product != null) {
                    _uiState.update {
                        it.copy(
                            isEditMode = true,
                            productId = product.productId,
                            name = product.name,
                            barcode = product.barcode.orEmpty(),
                            categoryId = product.categoryId,
                            productKind = product.productKind,
                            pricingMethod = product.pricingMethod,
                            quantityType = product.quantityType,
                            sellingUnit = product.sellingUnit,
                            stockUnit = product.stockUnit,
                            purchaseUnit = product.purchaseUnit.orEmpty(),
                            purchaseConversionFactor = product.purchaseConversionFactor?.toString().orEmpty(),
                            currentPrice = product.currentPrice.toString(),
                            minimumStock = product.minimumStock?.toString().orEmpty(),
                            categories = updatedCategories,
                            isLoading = false
                        )
                    }
                    return@launch
                }
            }

            _uiState.update {
                it.copy(
                    categoryId = if (it.categoryId.isBlank()) selectedCategoryId else it.categoryId,
                    categories = updatedCategories,
                    isLoading = false
                )
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, errorMessage = null) }
    fun onBarcodeChange(barcode: String) = _uiState.update { it.copy(barcode = barcode, errorMessage = null) }
    fun onCategoryChange(categoryId: String) = _uiState.update { it.copy(categoryId = categoryId, errorMessage = null) }
    fun onProductKindChange(kind: ProductKind) = _uiState.update { it.copy(productKind = kind) }
    fun onPricingMethodChange(method: PricingMethod) = _uiState.update {
        if (method == PricingMethod.PER_KG) {
            it.copy(pricingMethod = method, quantityType = QuantityType.GRAM, sellingUnit = "kg", stockUnit = "kg")
        } else {
            it.copy(pricingMethod = method, quantityType = QuantityType.COUNT, sellingUnit = "pcs", stockUnit = "pcs")
        }
    }
    fun onQuantityTypeChange(type: QuantityType) = _uiState.update { it.copy(quantityType = type) }
    fun onSellingUnitChange(unit: String) = _uiState.update { it.copy(sellingUnit = unit, errorMessage = null) }
    fun onStockUnitChange(unit: String) = _uiState.update { it.copy(stockUnit = unit, errorMessage = null) }
    fun onPurchaseUnitChange(unit: String) = _uiState.update { it.copy(purchaseUnit = unit) }
    fun onPurchaseConversionFactorChange(factor: String) = _uiState.update { it.copy(purchaseConversionFactor = factor) }
    fun onCurrentPriceChange(price: String) = _uiState.update { it.copy(currentPrice = price.filter { c -> c.isDigit() }, errorMessage = null) }
    fun onMinimumStockChange(stock: String) = _uiState.update { it.copy(minimumStock = stock.filter { c -> c.isDigit() }) }

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            val admin = getAdminUserUseCase() ?: return@launch
            when (val result = createCategoryUseCase(categoryName, admin)) {
                is Result.Success -> {
                    val updated = getCategoriesUseCase(onlyActive = false).firstOrNull() ?: emptyList()
                    _uiState.update {
                        it.copy(
                            categories = updated,
                            categoryId = result.data.categoryId
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun saveProduct() {
        val state = _uiState.value
        val priceLong = state.currentPrice.toLongOrNull()
        if (priceLong == null || priceLong < 0) {
            _uiState.update { it.copy(errorMessage = "Harga jual produk wajib diisi angka valid (>= 0).") }
            return
        }

        val convFactor = state.purchaseConversionFactor.toLongOrNull()
        if (state.purchaseUnit.isNotBlank() && (convFactor == null || convFactor <= 0)) {
            _uiState.update { it.copy(errorMessage = "Faktor konversi satuan beli harus berupa angka lebih besar dari 0.") }
            return
        }

        viewModelScope.launch {
            val admin = getAdminUserUseCase()
            if (admin == null) {
                _uiState.update { it.copy(errorMessage = "Pengguna Admin tidak ditemukan.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (state.isEditMode && state.productId != null) {
                val existing = getProductByIdUseCase(state.productId)
                if (existing == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Produk tidak ditemukan.") }
                    return@launch
                }
                val updated = existing.copy(
                    categoryId = state.categoryId,
                    name = state.name.trim(),
                    barcode = state.barcode.trim().takeIf { it.isNotBlank() },
                    productKind = state.productKind,
                    pricingMethod = state.pricingMethod,
                    quantityType = state.quantityType,
                    sellingUnit = state.sellingUnit.trim(),
                    stockUnit = state.stockUnit.trim(),
                    purchaseUnit = state.purchaseUnit.trim().takeIf { it.isNotBlank() },
                    purchaseConversionFactor = convFactor,
                    currentPrice = priceLong,
                    minimumStock = state.minimumStock.toLongOrNull(),
                    updatedAt = System.currentTimeMillis()
                )
                val result = updateProductUseCase(updated, admin)
                _uiState.update { it.copy(isLoading = false) }
                when (result) {
                    is Result.Success -> _eventFlow.emit(ProductFormEvent.SaveSuccess(updated.productId))
                    is Result.Error -> _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            } else {
                val result = createProductUseCase(
                    categoryId = state.categoryId,
                    name = state.name,
                    barcode = state.barcode,
                    productKind = state.productKind,
                    pricingMethod = state.pricingMethod,
                    quantityType = state.quantityType,
                    sellingUnit = state.sellingUnit,
                    stockUnit = state.stockUnit,
                    purchaseUnit = state.purchaseUnit.takeIf { it.isNotBlank() },
                    purchaseConversionFactor = convFactor,
                    currentPrice = priceLong,
                    minimumStock = state.minimumStock.toLongOrNull(),
                    user = admin
                )
                _uiState.update { it.copy(isLoading = false) }
                when (result) {
                    is Result.Success -> _eventFlow.emit(ProductFormEvent.SaveSuccess(result.data.productId))
                    is Result.Error -> _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }
}
