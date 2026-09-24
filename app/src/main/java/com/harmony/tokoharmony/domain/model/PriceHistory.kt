package com.harmony.tokoharmony.domain.model

data class PriceHistory(
    val historyId: String,
    val productId: String,
    val oldPrice: Long,
    val newPrice: Long,
    val changedAt: Long = System.currentTimeMillis(),
    val changedBy: String // userId of the admin
)
