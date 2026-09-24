package com.harmony.tokoharmony.domain.usecase.price

import com.harmony.tokoharmony.domain.model.PriceHistory
import com.harmony.tokoharmony.domain.repository.PriceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPriceHistoryUseCase @Inject constructor(
    private val priceRepository: PriceRepository
) {
    operator fun invoke(productId: String): Flow<List<PriceHistory>> {
        return priceRepository.getPriceHistory(productId)
    }

    suspend fun getList(productId: String): List<PriceHistory> {
        return priceRepository.getPriceHistoryList(productId)
    }
}
