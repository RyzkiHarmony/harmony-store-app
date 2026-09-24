package com.harmony.tokoharmony.domain.model

enum class StockLevelStatus {
    DIGITAL,
    OUT_OF_STOCK,
    LOW_STOCK,
    NORMAL
}

data class ProductStockInfo(
    val productId: String,
    val currentStock: Long,
    val stockLevel: StockLevelStatus,
    val stockUnit: String
)
