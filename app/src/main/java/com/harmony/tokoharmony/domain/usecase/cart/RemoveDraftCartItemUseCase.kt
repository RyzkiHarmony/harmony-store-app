package com.harmony.tokoharmony.domain.usecase.cart

import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.DraftCart
import com.harmony.tokoharmony.domain.repository.TransactionRepository
import javax.inject.Inject

class RemoveDraftCartItemUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(itemId: String): Result<DraftCart> {
        return transactionRepository.removeDraftItem(itemId)
    }
}
