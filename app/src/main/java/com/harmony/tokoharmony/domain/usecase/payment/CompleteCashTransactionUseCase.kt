package com.harmony.tokoharmony.domain.usecase.payment

import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.repository.TransactionRepository
import javax.inject.Inject

class CompleteCashTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        transactionId: String,
        amountReceived: Long,
        userId: String
    ): Result<Transaction> {
        return transactionRepository.completeCashTransaction(
            transactionId = transactionId,
            amountReceived = amountReceived,
            userId = userId
        )
    }
}
