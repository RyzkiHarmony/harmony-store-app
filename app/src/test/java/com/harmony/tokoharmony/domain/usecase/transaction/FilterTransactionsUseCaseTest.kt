package com.harmony.tokoharmony.domain.usecase.transaction

import com.harmony.tokoharmony.domain.model.PaymentMethod
import com.harmony.tokoharmony.domain.model.PaymentStatus
import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.model.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class FilterTransactionsUseCaseTest {

    private lateinit var filterTransactionsUseCase: FilterTransactionsUseCase
    private val now = System.currentTimeMillis()

    private fun createTx(
        id: String,
        status: TransactionStatus,
        paymentMethod: PaymentMethod?,
        createdAt: Long
    ): Transaction {
        return Transaction(
            transactionId = id,
            transactionNumber = "TRX-$id",
            transactionStatus = status,
            paymentStatus = if (status == TransactionStatus.COMPLETED) PaymentStatus.PAID else PaymentStatus.UNPAID,
            paymentMethod = paymentMethod,
            totalAmount = 50000L,
            amountReceived = 50000L,
            changeAmount = 0L,
            createdAt = createdAt,
            completedAt = if (status == TransactionStatus.COMPLETED) createdAt else null,
            cancelledAt = if (status == TransactionStatus.CANCELLED) createdAt else null,
            createdBy = "user-1",
            cancelledBy = null,
            cancellationReason = null
        )
    }

    @Before
    fun setUp() {
        filterTransactionsUseCase = FilterTransactionsUseCase()
    }

    @Test
    fun `default filters ALL returns all transactions unmodified`() {
        val list = listOf(
            createTx("1", TransactionStatus.COMPLETED, PaymentMethod.CASH, now),
            createTx("2", TransactionStatus.CANCELLED, PaymentMethod.QRIS, now),
            createTx("3", TransactionStatus.DRAFT, null, now)
        )

        val result = filterTransactionsUseCase(
            transactions = list,
            dateFilter = TransactionDateFilter.ALL,
            statusFilter = TransactionStatusFilter.ALL,
            paymentFilter = TransactionPaymentFilter.ALL,
            currentTimeMillis = now
        )

        assertEquals(3, result.size)
    }

    @Test
    fun `filter by status isolates COMPLETED, CANCELLED, and DRAFT correctly`() {
        val list = listOf(
            createTx("1", TransactionStatus.COMPLETED, PaymentMethod.CASH, now),
            createTx("2", TransactionStatus.CANCELLED, PaymentMethod.CASH, now),
            createTx("3", TransactionStatus.DRAFT, null, now)
        )

        val completed = filterTransactionsUseCase(
            transactions = list,
            statusFilter = TransactionStatusFilter.COMPLETED,
            currentTimeMillis = now
        )
        assertEquals(1, completed.size)
        assertEquals("1", completed[0].transactionId)

        val cancelled = filterTransactionsUseCase(
            transactions = list,
            statusFilter = TransactionStatusFilter.CANCELLED,
            currentTimeMillis = now
        )
        assertEquals(1, cancelled.size)
        assertEquals("2", cancelled[0].transactionId)

        val draft = filterTransactionsUseCase(
            transactions = list,
            statusFilter = TransactionStatusFilter.DRAFT,
            currentTimeMillis = now
        )
        assertEquals(1, draft.size)
        assertEquals("3", draft[0].transactionId)
    }

    @Test
    fun `filter by payment method isolates CASH and QRIS`() {
        val list = listOf(
            createTx("1", TransactionStatus.COMPLETED, PaymentMethod.CASH, now),
            createTx("2", TransactionStatus.COMPLETED, PaymentMethod.QRIS, now),
            createTx("3", TransactionStatus.DRAFT, null, now)
        )

        val cashOnly = filterTransactionsUseCase(
            transactions = list,
            paymentFilter = TransactionPaymentFilter.CASH,
            currentTimeMillis = now
        )
        assertEquals(1, cashOnly.size)
        assertEquals("1", cashOnly[0].transactionId)

        val qrisOnly = filterTransactionsUseCase(
            transactions = list,
            paymentFilter = TransactionPaymentFilter.QRIS,
            currentTimeMillis = now
        )
        assertEquals(1, qrisOnly.size)
        assertEquals("2", qrisOnly[0].transactionId)
    }

    @Test
    fun `filter by date range filters TODAY, LAST_7_DAYS, and THIS_MONTH`() {
        // Start of today
        val calToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 10)
        }
        val todayTxTime = calToday.timeInMillis

        // 2 days ago
        val cal2DaysAgo = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -2)
        }
        val twoDaysAgoTime = cal2DaysAgo.timeInMillis

        // 40 days ago (outside this month)
        val cal40DaysAgo = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -40)
        }
        val fortyDaysAgoTime = cal40DaysAgo.timeInMillis

        val list = listOf(
            createTx("today", TransactionStatus.COMPLETED, PaymentMethod.CASH, todayTxTime),
            createTx("2days", TransactionStatus.COMPLETED, PaymentMethod.CASH, twoDaysAgoTime),
            createTx("40days", TransactionStatus.COMPLETED, PaymentMethod.CASH, fortyDaysAgoTime)
        )

        val todayResult = filterTransactionsUseCase(
            transactions = list,
            dateFilter = TransactionDateFilter.TODAY,
            currentTimeMillis = now
        )
        assertEquals(1, todayResult.size)
        assertEquals("today", todayResult[0].transactionId)

        val monthResult = filterTransactionsUseCase(
            transactions = list,
            dateFilter = TransactionDateFilter.THIS_MONTH,
            currentTimeMillis = now
        )
        // 40 days ago should not be in this month
        assertTrue(monthResult.none { it.transactionId == "40days" })
    }

    @Test
    fun `combining multiple filters applies AND logic correctly`() {
        val list = listOf(
            createTx("1", TransactionStatus.COMPLETED, PaymentMethod.CASH, now),
            createTx("2", TransactionStatus.COMPLETED, PaymentMethod.QRIS, now),
            createTx("3", TransactionStatus.CANCELLED, PaymentMethod.CASH, now),
            createTx("4", TransactionStatus.DRAFT, null, now)
        )

        // COMPLETED + CASH
        val result = filterTransactionsUseCase(
            transactions = list,
            dateFilter = TransactionDateFilter.ALL,
            statusFilter = TransactionStatusFilter.COMPLETED,
            paymentFilter = TransactionPaymentFilter.CASH,
            currentTimeMillis = now
        )

        assertEquals(1, result.size)
        assertEquals("1", result[0].transactionId)
    }

    @Test
    fun `no matching transactions returns empty list`() {
        val list = listOf(
            createTx("1", TransactionStatus.COMPLETED, PaymentMethod.CASH, now)
        )

        val result = filterTransactionsUseCase(
            transactions = list,
            statusFilter = TransactionStatusFilter.CANCELLED,
            currentTimeMillis = now
        )

        assertTrue(result.isEmpty())
    }
}
