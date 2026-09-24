package com.harmony.tokoharmony.domain.usecase.digital

import com.harmony.tokoharmony.domain.model.DigitalTransaction
import com.harmony.tokoharmony.domain.repository.DigitalTransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDigitalTransactionByItemUseCase @Inject constructor(
    private val digitalTransactionRepository: DigitalTransactionRepository
) {
    suspend operator fun invoke(transactionItemId: String): DigitalTransaction? {
        return digitalTransactionRepository.getDigitalTransactionByItemId(transactionItemId)
    }

    fun observe(transactionItemId: String): Flow<DigitalTransaction?> {
        return digitalTransactionRepository.observeDigitalTransactionByItemId(transactionItemId)
    }
}
