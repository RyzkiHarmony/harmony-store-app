package com.harmony.tokoharmony.domain.usecase.cart

import com.harmony.tokoharmony.domain.model.DraftCart
import com.harmony.tokoharmony.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveDraftCartUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<DraftCart?> {
        return transactionRepository.getActiveDraftCart()
    }
}
