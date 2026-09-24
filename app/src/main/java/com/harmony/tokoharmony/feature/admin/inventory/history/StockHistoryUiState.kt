package com.harmony.tokoharmony.feature.admin.inventory.history

import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.StockMovement

data class StockHistoryUiState(
    val movements: List<StockMovement> = emptyList(),
    val productsMap: Map<String, Product> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
