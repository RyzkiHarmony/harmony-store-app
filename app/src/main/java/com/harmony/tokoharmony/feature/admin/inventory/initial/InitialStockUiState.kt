package com.harmony.tokoharmony.feature.admin.inventory.initial

import com.harmony.tokoharmony.domain.model.Product

data class InitialStockUiState(
    val products: List<Product> = emptyList(),
    val stockMap: Map<String, Long> = emptyMap(),
    val initializedMap: Map<String, Boolean> = emptyMap(),
    val selectedProduct: Product? = null,
    val initialStockInput: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed interface InitialStockEvent {
    data class Success(val message: String) : InitialStockEvent
}
