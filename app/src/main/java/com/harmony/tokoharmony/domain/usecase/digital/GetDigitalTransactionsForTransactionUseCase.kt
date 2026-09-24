package com.harmony.tokoharmony.domain.usecase.digital

import com.harmony.tokoharmony.domain.model.DigitalTransaction
import com.harmony.tokoharmony.domain.repository.DigitalTransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDigitalTransactionsForTransactionUseCase @Inject constructor(
    private val digitalTransactionRepository: DigitalTransactionRepository
) {
    suspend operator fun invoke(transactionId: String): List<DigitalTransaction> {
        return digitalTransactionRepository.getDigitalTransactionsForTransaction(transactionId)
    }

    fun observe(transactionId: String): Flow<List<DigitalTransaction>> {
        return digitalTransactionRepository.observeDigitalTransactionsForTransaction(transactionId)
    }
}
