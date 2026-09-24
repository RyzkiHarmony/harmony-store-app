package com.harmony.tokoharmony.domain.model

data class StockAdjustment(
    val adjustmentId: String,
    val productId: String,
    val systemQuantity: Long,
    val physicalQuantity: Long,
    val difference: Long,
    val reason: String,
    val note: String? = null,
    val createdAt: Long,
    val createdBy: String
)
