package com.harmony.tokoharmony.data.local.mapper

import com.harmony.tokoharmony.core.database.entity.StockAdjustmentEntity
import com.harmony.tokoharmony.domain.model.StockAdjustment

fun StockAdjustmentEntity.toDomain(): StockAdjustment {
    return StockAdjustment(
        adjustmentId = adjustmentId,
        productId = productId,
        systemQuantity = systemQuantity,
        physicalQuantity = physicalQuantity,
        difference = difference,
        reason = reason,
        note = note,
        createdAt = createdAt,
        createdBy = createdBy
    )
}

fun StockAdjustment.toEntity(): StockAdjustmentEntity {
    return StockAdjustmentEntity(
        adjustmentId = adjustmentId,
        productId = productId,
        systemQuantity = systemQuantity,
        physicalQuantity = physicalQuantity,
        difference = difference,
        reason = reason,
        note = note,
        createdAt = createdAt,
        createdBy = createdBy
    )
}
