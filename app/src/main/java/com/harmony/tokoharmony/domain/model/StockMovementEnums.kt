package com.harmony.tokoharmony.domain.model

enum class MovementType {
    INITIAL_STOCK,
    STOCK_IN,
    SALE,
    SALE_REVERSAL,
    ADJUSTMENT
}

enum class ReferenceType {
    TRANSACTION,
    STOCK_IN,
    STOCK_ADJUSTMENT
}
