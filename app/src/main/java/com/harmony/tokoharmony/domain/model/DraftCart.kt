package com.harmony.tokoharmony.domain.model

data class DraftCart(
    val transaction: Transaction,
    val items: List<TransactionItem> = emptyList()
) {
    val itemCount: Int get() = items.size
    val totalAmount: Long get() = items.sumOf { it.subtotal }
    val isEmpty: Boolean get() = items.isEmpty()
}
