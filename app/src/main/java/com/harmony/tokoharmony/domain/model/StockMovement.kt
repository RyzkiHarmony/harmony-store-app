package com.harmony.tokoharmony.domain.model

data class StockMovement(
    val movementId: String,
    val productId: String,
    val movementType: MovementType,
    val quantityDelta: Long,
    val referenceType: ReferenceType? = null,
    val referenceId: String? = null,
    val reason: String? = null,
    val createdAt: Long,
    val createdBy: String
)
