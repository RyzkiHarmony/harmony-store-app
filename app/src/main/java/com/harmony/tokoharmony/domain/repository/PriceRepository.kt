package com.harmony.tokoharmony.domain.repository

import com.harmony.tokoharmony.domain.model.PriceHistory
import kotlinx.coroutines.flow.Flow

interface PriceRepository {
    fun getPriceHistory(productId: String): Flow<List<PriceHistory>>
    suspend fun getPriceHistoryList(productId: String): List<PriceHistory>
    suspend fun recordPriceChange(priceHistory: PriceHistory, newPrice: Long)
}
