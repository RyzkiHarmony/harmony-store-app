package com.harmony.tokoharmony.domain.usecase.cart

import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.DraftCart
import com.harmony.tokoharmony.domain.repository.TransactionRepository
import javax.inject.Inject

class AddProductToDraftCartUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        productId: String,
        quantity: Long,
        userId: String
    ): Result<DraftCart> {
        return transactionRepository.addItemToDraft(productId, quantity, userId)
    }
}
