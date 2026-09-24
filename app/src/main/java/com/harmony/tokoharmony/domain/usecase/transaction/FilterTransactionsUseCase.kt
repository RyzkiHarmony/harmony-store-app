package com.harmony.tokoharmony.domain.usecase.transaction

import com.harmony.tokoharmony.domain.model.PaymentMethod
import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.model.TransactionStatus
import java.util.Calendar
import javax.inject.Inject

enum class TransactionDateFilter {
    ALL,
    TODAY,
    LAST_7_DAYS,
    THIS_MONTH
}

enum class TransactionStatusFilter {
    ALL,
    COMPLETED,
    CANCELLED,
    DRAFT
}

enum class TransactionPaymentFilter {
    ALL,
    CASH,
    QRIS
}

class FilterTransactionsUseCase @Inject constructor() {

    operator fun invoke(
        transactions: List<Transaction>,
        dateFilter: TransactionDateFilter = TransactionDateFilter.ALL,
        statusFilter: TransactionStatusFilter = TransactionStatusFilter.ALL,
        paymentFilter: TransactionPaymentFilter = TransactionPaymentFilter.ALL,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<Transaction> {
        val startOfToday = getStartOfDay(currentTimeMillis)
        val sevenDaysAgo = currentTimeMillis - (7L * 24L * 60L * 60L * 1000L)
        val startOfMonth = getStartOfMonth(currentTimeMillis)

        return transactions.filter { tx ->
            // 1. Date filter
            val matchesDate = when (dateFilter) {
                TransactionDateFilter.ALL -> true
                TransactionDateFilter.TODAY -> tx.createdAt >= startOfToday
                TransactionDateFilter.LAST_7_DAYS -> tx.createdAt >= sevenDaysAgo
                TransactionDateFilter.THIS_MONTH -> tx.createdAt >= startOfMonth
            }

            // 2. Status filter
            val matchesStatus = when (statusFilter) {
                TransactionStatusFilter.ALL -> true
                TransactionStatusFilter.COMPLETED -> tx.transactionStatus == TransactionStatus.COMPLETED
                TransactionStatusFilter.CANCELLED -> tx.transactionStatus == TransactionStatus.CANCELLED
                TransactionStatusFilter.DRAFT -> tx.transactionStatus == TransactionStatus.DRAFT
            }

            // 3. Payment filter
            val matchesPayment = when (paymentFilter) {
                TransactionPaymentFilter.ALL -> true
                TransactionPaymentFilter.CASH -> tx.paymentMethod == PaymentMethod.CASH
                TransactionPaymentFilter.QRIS -> tx.paymentMethod == PaymentMethod.QRIS
            }

            matchesDate && matchesStatus && matchesPayment
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getStartOfMonth(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
