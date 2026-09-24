package com.harmony.tokoharmony.domain.model

enum class TransactionStatus {
    DRAFT,
    COMPLETED,
    CANCELLED
}

enum class PaymentStatus {
    UNPAID,
    PAID
}

enum class PaymentMethod {
    CASH,
    QRIS
}
