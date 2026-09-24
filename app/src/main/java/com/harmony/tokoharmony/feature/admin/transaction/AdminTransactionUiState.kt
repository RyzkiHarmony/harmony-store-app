package com.harmony.tokoharmony.feature.admin.transaction

import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.model.TransactionItem
import com.harmony.tokoharmony.domain.usecase.transaction.TransactionDateFilter
import com.harmony.tokoharmony.domain.usecase.transaction.TransactionPaymentFilter
import com.harmony.tokoharmony.domain.usecase.transaction.TransactionStatusFilter

data class AdminTransactionHistoryUiState(
    val rawTransactions: List<Transaction> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val dateFilter: TransactionDateFilter = TransactionDateFilter.ALL,
    val statusFilter: TransactionStatusFilter = TransactionStatusFilter.ALL,
    val paymentFilter: TransactionPaymentFilter = TransactionPaymentFilter.ALL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class AdminTransactionDetailUiState(
    val transaction: Transaction? = null,
    val items: List<TransactionItem> = emptyList(),
    val isLoading: Boolean = false,
    val isCancelling: Boolean = false,
    val cancellationReasonInput: String = "Kesalahan transaksi / kasir",
    val noteInput: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

val CANCELLATION_REASONS = listOf(
    "Kesalahan transaksi / kasir",
    "Pelanggan membatalkan pesanan",
    "Barang retur / rusak",
    "Salah metode pembayaran",
    "Lainnya"
)

sealed interface AdminTransactionEvent {
    data class CancelSuccess(val message: String) : AdminTransactionEvent
}
