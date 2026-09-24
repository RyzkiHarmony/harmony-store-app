package com.harmony.tokoharmony.domain.model

data class TransactionItem(
    val itemId: String,
    val transactionId: String,
    val productId: String,
    val productNameSnapshot: String,
    val quantity: Long,
    val unitPrice: Long,
    val subtotal: Long,
    val sellingUnit: String
)
