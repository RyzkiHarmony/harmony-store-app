package com.harmony.tokoharmony.domain.model

data class BootstrapData(
    val users: List<User> = emptyList(),
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val priceHistories: List<PriceHistory> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val transactionItems: List<TransactionItem> = emptyList(),
    val stockMovements: List<StockMovement> = emptyList(),
    val stockIns: List<StockIn> = emptyList(),
    val stockAdjustments: List<StockAdjustment> = emptyList(),
    val digitalTransactions: List<DigitalTransaction> = emptyList()
)
