package com.harmony.tokoharmony.domain.usecase.transaction

import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionByIdUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transactionId: String): Transaction? {
        return transactionRepository.getTransactionById(transactionId)
    }
}
