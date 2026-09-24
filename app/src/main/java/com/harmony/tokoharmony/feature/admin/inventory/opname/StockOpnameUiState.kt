package com.harmony.tokoharmony.feature.admin.inventory.opname

import com.harmony.tokoharmony.domain.model.Product

data class StockOpnameUiState(
    val products: List<Product> = emptyList(),
    val stockMap: Map<String, Long> = emptyMap(),
    val selectedProduct: Product? = null,
    val systemStock: Long = 0L,
    val physicalStockInput: String = "",
    val selectedReason: String = "Stock opname",
    val noteInput: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val physicalStock: Long? get() = physicalStockInput.toLongOrNull()
    val difference: Long? get() = physicalStock?.let { it - systemStock }
}

val ADJUSTMENT_REASONS = listOf(
    "Stock opname",
    "Barang rusak",
    "Barang hilang",
    "Kedaluwarsa",
    "Kesalahan pencatatan",
    "Lainnya"
)

sealed interface StockOpnameEvent {
    data class Success(val message: String) : StockOpnameEvent
}
