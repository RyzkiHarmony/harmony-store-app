package com.harmony.tokoharmony.domain.usecase.digital

import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.repository.DigitalTransactionRepository
import javax.inject.Inject

class UpdateDigitalTransactionStatusUseCase @Inject constructor(
    private val digitalTransactionRepository: DigitalTransactionRepository
) {
    suspend operator fun invoke(digitalTransactionId: String, newStatus: String): Result<Unit> {
        return digitalTransactionRepository.updateDigitalTransactionStatus(digitalTransactionId, newStatus)
    }
}
