package com.harmony.tokoharmony.data.local.mapper

import com.harmony.tokoharmony.core.database.entity.PriceHistoryEntity
import com.harmony.tokoharmony.domain.model.PriceHistory

fun PriceHistoryEntity.toDomain(): PriceHistory {
    return PriceHistory(
        historyId = historyId,
        productId = productId,
        oldPrice = oldPrice,
        newPrice = newPrice,
        changedAt = changedAt,
        changedBy = changedBy
    )
}

fun PriceHistory.toEntity(): PriceHistoryEntity {
    return PriceHistoryEntity(
        historyId = historyId,
        productId = productId,
        oldPrice = oldPrice,
        newPrice = newPrice,
        changedAt = changedAt,
        changedBy = changedBy
    )
}
