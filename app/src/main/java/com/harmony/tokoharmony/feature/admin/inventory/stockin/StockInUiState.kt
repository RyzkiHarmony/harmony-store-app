package com.harmony.tokoharmony.feature.admin.inventory.stockin

import com.harmony.tokoharmony.domain.model.Product

data class StockInUiState(
    val products: List<Product> = emptyList(),
    val stockMap: Map<String, Long> = emptyMap(),
    val selectedProduct: Product? = null,
    val purchaseQuantityInput: String = "",
    val purchaseUnitInput: String = "",
    val conversionFactorInput: String = "1",
    val noteInput: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val purchaseQuantity: Long get() = purchaseQuantityInput.toLongOrNull() ?: 0L
    val conversionFactor: Long get() = conversionFactorInput.toLongOrNull() ?: 1L
    val totalStockQuantity: Long get() = purchaseQuantity * conversionFactor
}

sealed interface StockInEvent {
    data class Success(val message: String) : StockInEvent
}
