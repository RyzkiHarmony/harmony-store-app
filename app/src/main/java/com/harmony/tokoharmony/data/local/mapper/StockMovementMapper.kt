package com.harmony.tokoharmony.data.local.mapper

import com.harmony.tokoharmony.core.database.entity.StockMovementEntity
import com.harmony.tokoharmony.domain.model.MovementType
import com.harmony.tokoharmony.domain.model.ReferenceType
import com.harmony.tokoharmony.domain.model.StockMovement

fun StockMovementEntity.toDomain(): StockMovement {
    return StockMovement(
        movementId = movementId,
        productId = productId,
        movementType = MovementType.valueOf(movementType),
        quantityDelta = quantityDelta,
        referenceType = referenceType?.let { ReferenceType.valueOf(it) },
        referenceId = referenceId,
        reason = reason,
        createdAt = createdAt,
        createdBy = createdBy
    )
}

fun StockMovement.toEntity(): StockMovementEntity {
    return StockMovementEntity(
        movementId = movementId,
        productId = productId,
        movementType = movementType.name,
        quantityDelta = quantityDelta,
        referenceType = referenceType?.name,
        referenceId = referenceId,
        reason = reason,
        createdAt = createdAt,
        createdBy = createdBy
    )
}
