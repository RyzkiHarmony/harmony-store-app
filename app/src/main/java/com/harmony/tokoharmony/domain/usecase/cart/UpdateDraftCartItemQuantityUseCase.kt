package com.harmony.tokoharmony.domain.usecase.cart

import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.DraftCart
import com.harmony.tokoharmony.domain.repository.TransactionRepository
import javax.inject.Inject

class UpdateDraftCartItemQuantityUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        itemId: String,
        newQuantity: Long
    ): Result<DraftCart> {
        return transactionRepository.updateDraftItemQuantity(itemId, newQuantity)
    }
}
