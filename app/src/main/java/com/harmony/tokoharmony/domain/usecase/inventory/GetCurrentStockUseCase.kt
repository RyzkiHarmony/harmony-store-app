package com.harmony.tokoharmony.domain.usecase.inventory

import com.harmony.tokoharmony.domain.repository.StockRepository
import javax.inject.Inject

class GetCurrentStockUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    suspend operator fun invoke(productId: String): Long {
        return stockRepository.getCurrentStock(productId)
    }
}
