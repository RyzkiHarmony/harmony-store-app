package com.harmony.tokoharmony.domain.model

data class Transaction(
    val transactionId: String,
    val transactionNumber: String,
    val transactionStatus: TransactionStatus,
    val paymentStatus: PaymentStatus,
    val paymentMethod: PaymentMethod? = null,
    val totalAmount: Long = 0L,
    val amountReceived: Long? = null,
    val changeAmount: Long = 0L,
    val createdAt: Long,
    val completedAt: Long? = null,
    val cancelledAt: Long? = null,
    val createdBy: String,
    val cancelledBy: String? = null,
    val cancellationReason: String? = null
)
