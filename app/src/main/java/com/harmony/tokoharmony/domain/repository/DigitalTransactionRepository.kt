package com.harmony.tokoharmony.domain.repository

import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.DigitalTransaction
import kotlinx.coroutines.flow.Flow

interface DigitalTransactionRepository {
    suspend fun recordDigitalTransaction(digitalTransaction: DigitalTransaction): Result<Unit>
    suspend fun getDigitalTransactionById(id: String): DigitalTransaction?
    suspend fun getDigitalTransactionByItemId(itemId: String): DigitalTransaction?
    fun observeDigitalTransactionByItemId(itemId: String): Flow<DigitalTransaction?>
    suspend fun getDigitalTransactionsForTransaction(transactionId: String): List<DigitalTransaction>
    fun observeDigitalTransactionsForTransaction(transactionId: String): Flow<List<DigitalTransaction>>
    suspend fun updateDigitalTransactionStatus(id: String, status: String): Result<Unit>
}
