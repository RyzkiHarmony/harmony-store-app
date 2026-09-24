package com.harmony.tokoharmony.domain.model

data class StockIn(
    val stockInId: String,
    val productId: String,
    val purchaseQuantity: Long,
    val purchaseUnit: String,
    val conversionFactor: Long,
    val stockQuantity: Long,
    val note: String? = null,
    val createdAt: Long,
    val createdBy: String
)
