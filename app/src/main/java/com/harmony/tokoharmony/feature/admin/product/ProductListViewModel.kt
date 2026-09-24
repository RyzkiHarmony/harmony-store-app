package com.harmony.tokoharmony.feature.admin.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harmony.tokoharmony.domain.model.Category
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductStockInfo
import com.harmony.tokoharmony.domain.repository.StockRepository
import com.harmony.tokoharmony.domain.usecase.category.GetCategoriesUseCase
import com.harmony.tokoharmony.domain.usecase.product.EvaluateProductStockStatusUseCase
import com.harmony.tokoharmony.domain.usecase.product.SearchProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class ProductFilterStatus {
    ALL,
    ACTIVE_ONLY,
    INACTIVE_ONLY
}

data class ProductListUiState(
    val searchQuery: String = "",
    val filterStatus: ProductFilterStatus = ProductFilterStatus.ALL,
    val products: List<Product> = emptyList(),
    val categoriesMap: Map<String, String> = emptyMap(),
    val stockInfoMap: Map<String, ProductStockInfo> = emptyMap(),
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val searchProductsUseCase: SearchProductsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val stockRepository: StockRepository,
    private val evaluateProductStockStatusUseCase: EvaluateProductStockStatusUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterStatus = MutableStateFlow(ProductFilterStatus.ALL)

    private val _categories = getCategoriesUseCase(onlyActive = false)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _movements = stockRepository.getAllStockMovements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<ProductListUiState> = combine(
        _searchQuery,
        _filterStatus,
        _categories,
        _searchQuery.flatMapLatest { query ->
            searchProductsUseCase(query = query, onlyActive = false)
        },
        _movements
    ) { query, filter, categories, products, movements ->
        val filtered = when (filter) {
            ProductFilterStatus.ALL -> products
            ProductFilterStatus.ACTIVE_ONLY -> products.filter { it.isActive }
            ProductFilterStatus.INACTIVE_ONLY -> products.filter { !it.isActive }
        }
        val catMap = categories.associate { it.categoryId to it.name }

        // Compute current stock from StockMovement ledger
        val stockDeltaMap = movements.groupBy { it.productId }
            .mapValues { (_, movList) -> movList.sumOf { it.quantityDelta } }

        val stockMap = filtered.associate { product ->
            val currentStock = stockDeltaMap[product.productId] ?: 0L
            val level = evaluateProductStockStatusUseCase(product, currentStock)
            product.productId to ProductStockInfo(
                productId = product.productId,
                currentStock = currentStock,
                stockLevel = level,
                stockUnit = product.stockUnit
            )
        }

        ProductListUiState(
            searchQuery = query,
            filterStatus = filter,
            products = filtered,
            categoriesMap = catMap,
            stockInfoMap = stockMap,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProductListUiState(isLoading = true))

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterStatusChanged(status: ProductFilterStatus) {
        _filterStatus.value = status
    }
}
