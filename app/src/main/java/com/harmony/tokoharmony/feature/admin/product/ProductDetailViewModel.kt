package com.harmony.tokoharmony.feature.admin.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.Category
import com.harmony.tokoharmony.domain.model.PriceHistory
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.repository.CategoryRepository
import com.harmony.tokoharmony.domain.usecase.auth.GetAdminUserUseCase
import com.harmony.tokoharmony.domain.usecase.price.ChangeProductPriceUseCase
import com.harmony.tokoharmony.domain.usecase.price.GetPriceHistoryUseCase
import com.harmony.tokoharmony.domain.usecase.product.ActivateProductUseCase
import com.harmony.tokoharmony.domain.usecase.product.DeactivateProductUseCase
import com.harmony.tokoharmony.domain.usecase.product.GetProductByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val product: Product? = null,
    val category: Category? = null,
    val priceHistories: List<PriceHistory> = emptyList(),
    val isPriceChangeDialogVisible: Boolean = false,
    val isDeactivateDialogVisible: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val categoryRepository: CategoryRepository,
    private val getPriceHistoryUseCase: GetPriceHistoryUseCase,
    private val changeProductPriceUseCase: ChangeProductPriceUseCase,
    private val deactivateProductUseCase: DeactivateProductUseCase,
    private val activateProductUseCase: ActivateProductUseCase,
    private val getAdminUserUseCase: GetAdminUserUseCase
) : ViewModel() {

    private val productId: String = checkNotNull(savedStateHandle["productId"])

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        loadProductData()
        observePriceHistory()
    }

    fun loadProductData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val product = getProductByIdUseCase(productId)
            if (product != null) {
                val category = categoryRepository.getCategoryById(product.categoryId)
                _uiState.update {
                    it.copy(
                        product = product,
                        category = category,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Produk tidak ditemukan."
                    )
                }
            }
        }
    }

    private fun observePriceHistory() {
        viewModelScope.launch {
            getPriceHistoryUseCase(productId).collectLatest { histories ->
                _uiState.update { it.copy(priceHistories = histories) }
            }
        }
    }

    fun showPriceChangeDialog() = _uiState.update { it.copy(isPriceChangeDialogVisible = true) }
    fun dismissPriceChangeDialog() = _uiState.update { it.copy(isPriceChangeDialogVisible = false) }

    fun showDeactivateDialog() = _uiState.update { it.copy(isDeactivateDialogVisible = true) }
    fun dismissDeactivateDialog() = _uiState.update { it.copy(isDeactivateDialogVisible = false) }

    fun changePrice(newPrice: Long) {
        viewModelScope.launch {
            val admin = getAdminUserUseCase()
            if (admin == null) {
                _uiState.update { it.copy(errorMessage = "Pengguna Admin tidak ditemukan.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, isPriceChangeDialogVisible = false) }
            val result = changeProductPriceUseCase(productId, newPrice, admin)
            when (result) {
                is Result.Success -> {
                    loadProductData()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun toggleProductActiveStatus() {
        viewModelScope.launch {
            val product = _uiState.value.product ?: return@launch
            val admin = getAdminUserUseCase() ?: return@launch

            _uiState.update { it.copy(isLoading = true, isDeactivateDialogVisible = false) }
            val result = if (product.isActive) {
                deactivateProductUseCase(productId, admin)
            } else {
                activateProductUseCase(productId, admin)
            }

            when (result) {
                is Result.Success -> {
                    loadProductData()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }
}
