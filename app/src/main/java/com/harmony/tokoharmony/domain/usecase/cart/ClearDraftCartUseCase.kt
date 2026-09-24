package com.harmony.tokoharmony.domain.usecase.cart

import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.repository.TransactionRepository
import javax.inject.Inject

class ClearDraftCartUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transactionId: String): Result<Unit> {
        return transactionRepository.clearDraft(transactionId)
    }
}
