package com.harmony.tokoharmony.domain.usecase.digital

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.DigitalTransaction
import com.harmony.tokoharmony.domain.repository.DigitalTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeDigitalTransactionRepository : DigitalTransactionRepository {
    val items = mutableMapOf<String, DigitalTransaction>()

    override suspend fun recordDigitalTransaction(digitalTransaction: DigitalTransaction): Result<Unit> {
        if (digitalTransaction.customerNumber.isBlank()) {
            return Result.Error(AppError.Validation("Nomor pelanggan/tujuan tidak boleh kosong."))
        }
        if (digitalTransaction.serviceType.isBlank()) {
            return Result.Error(AppError.Validation("Jenis layanan tidak boleh kosong."))
        }
        val validStatuses = listOf("PENDING", "SUCCESS", "FAILED")
        if (digitalTransaction.status !in validStatuses) {
            return Result.Error(AppError.Validation("Status transaksi digital tidak valid: ${digitalTransaction.status}"))
        }

        items[digitalTransaction.digitalTransactionId] = digitalTransaction
        return Result.Success(Unit)
    }

    override suspend fun getDigitalTransactionById(id: String): DigitalTransaction? {
        return items[id]
    }

    override suspend fun getDigitalTransactionByItemId(itemId: String): DigitalTransaction? {
        return items.values.find { it.transactionItemId == itemId }
    }

    override fun observeDigitalTransactionByItemId(itemId: String): Flow<DigitalTransaction?> {
        return flowOf(items.values.find { it.transactionItemId == itemId })
    }

    override suspend fun getDigitalTransactionsForTransaction(transactionId: String): List<DigitalTransaction> {
        return items.values.toList()
    }

    override fun observeDigitalTransactionsForTransaction(transactionId: String): Flow<List<DigitalTransaction>> {
        return flowOf(items.values.toList())
    }

    override suspend fun updateDigitalTransactionStatus(id: String, status: String): Result<Unit> {
        val validStatuses = listOf("PENDING", "SUCCESS", "FAILED")
        if (status !in validStatuses) {
            return Result.Error(AppError.Validation("Status transaksi digital tidak valid: $status"))
        }
        val current = items[id] ?: return Result.Error(AppError.NotFound("Transaksi digital tidak ditemukan."))
        items[id] = current.copy(status = status)
        return Result.Success(Unit)
    }
}

class DigitalTransactionUseCaseTest {

    private lateinit var repository: FakeDigitalTransactionRepository
    private lateinit var recordUseCase: RecordDigitalTransactionUseCase
    private lateinit var getByItemUseCase: GetDigitalTransactionByItemUseCase
    private lateinit var getForTxUseCase: GetDigitalTransactionsForTransactionUseCase
    private lateinit var updateStatusUseCase: UpdateDigitalTransactionStatusUseCase

    @Before
    fun setUp() {
        repository = FakeDigitalTransactionRepository()
        recordUseCase = RecordDigitalTransactionUseCase(repository)
        getByItemUseCase = GetDigitalTransactionByItemUseCase(repository)
        getForTxUseCase = GetDigitalTransactionsForTransactionUseCase(repository)
        updateStatusUseCase = UpdateDigitalTransactionStatusUseCase(repository)
    }

    @Test
    fun `record digital transaction with valid data succeeds`() = runTest {
        val dt = DigitalTransaction(
            digitalTransactionId = "dt-1",
            transactionItemId = "item-1",
            serviceType = "PULSA",
            customerNumber = "08123456789",
            nominal = 50000L,
            providerReference = "REF123456",
            status = "PENDING",
            createdAt = 1000L
        )

        val result = recordUseCase(dt)
        assertTrue(result is Result.Success)

        val retrieved = getByItemUseCase("item-1")
        assertNotNull(retrieved)
        assertEquals("dt-1", retrieved?.digitalTransactionId)
        assertEquals("08123456789", retrieved?.customerNumber)
        assertEquals("PULSA", retrieved?.serviceType)
        assertEquals(50000L, retrieved?.nominal)
        assertEquals("PENDING", retrieved?.status)
    }

    @Test
    fun `record digital transaction with blank customer number fails validation`() = runTest {
        val dt = DigitalTransaction(
            digitalTransactionId = "dt-2",
            transactionItemId = "item-2",
            serviceType = "PLN",
            customerNumber = "   ",
            nominal = 20000L,
            status = "PENDING",
            createdAt = 1000L
        )

        val result = recordUseCase(dt)
        assertTrue(result is Result.Error)
        assertEquals("Nomor pelanggan / nomor tujuan tidak boleh kosong.", (result as Result.Error).error.message)
    }

    @Test
    fun `record digital transaction with blank service type fails validation`() = runTest {
        val dt = DigitalTransaction(
            digitalTransactionId = "dt-3",
            transactionItemId = "item-3",
            serviceType = "",
            customerNumber = "08123456789",
            nominal = 20000L,
            status = "PENDING",
            createdAt = 1000L
        )

        val result = recordUseCase(dt)
        assertTrue(result is Result.Error)
        assertEquals("Jenis layanan produk digital tidak boleh kosong.", (result as Result.Error).error.message)
    }

    @Test
    fun `record digital transaction with invalid status fails validation`() = runTest {
        val dt = DigitalTransaction(
            digitalTransactionId = "dt-4",
            transactionItemId = "item-4",
            serviceType = "PULSA",
            customerNumber = "08123456789",
            nominal = 20000L,
            status = "UNKNOWN_STATUS",
            createdAt = 1000L
        )

        val result = recordUseCase(dt)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error.message.contains("Status transaksi digital harus salah satu dari: PENDING, SUCCESS, FAILED."))
    }

    @Test
    fun `update digital transaction status succeeds with valid status and fails with invalid status`() = runTest {
        val dt = DigitalTransaction(
            digitalTransactionId = "dt-5",
            transactionItemId = "item-5",
            serviceType = "PULSA",
            customerNumber = "08123456789",
            nominal = 25000L,
            status = "PENDING",
            createdAt = 1000L
        )
        recordUseCase(dt)

        val updateSuccess = updateStatusUseCase("dt-5", "SUCCESS")
        assertTrue(updateSuccess is Result.Success)
        assertEquals("SUCCESS", getByItemUseCase("item-5")?.status)

        val updateInvalid = updateStatusUseCase("dt-5", "INVALID")
        assertTrue(updateInvalid is Result.Error)
        // Status remains SUCCESS
        assertEquals("SUCCESS", getByItemUseCase("item-5")?.status)
    }

    @Test
    fun `digital nominal and transaction item price can differ per business rules`() = runTest {
        // Guardrail 3: Nominal (e.g. 50,000 pulsa value) vs selling price (e.g. 52,000 charged to customer)
        val nominal = 50000L
        val customerChargedPrice = 52000L

        val dt = DigitalTransaction(
            digitalTransactionId = "dt-6",
            transactionItemId = "item-6",
            serviceType = "PULSA",
            customerNumber = "08123456789",
            nominal = nominal,
            status = "PENDING",
            createdAt = 1000L
        )
        recordUseCase(dt)

        val stored = getByItemUseCase("item-6")
        assertNotNull(stored)
        assertEquals(nominal, stored?.nominal)
        assertTrue(stored?.nominal != customerChargedPrice)
    }
}
