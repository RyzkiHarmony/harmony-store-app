package com.harmony.tokoharmony.data.local.mapper

import com.harmony.tokoharmony.core.database.entity.StockInEntity
import com.harmony.tokoharmony.domain.model.StockIn

fun StockInEntity.toDomain(): StockIn {
    return StockIn(
        stockInId = stockInId,
        productId = productId,
        purchaseQuantity = purchaseQuantity,
        purchaseUnit = purchaseUnit,
        conversionFactor = conversionFactor,
        stockQuantity = stockQuantity,
        note = note,
        createdAt = createdAt,
        createdBy = createdBy
    )
}

fun StockIn.toEntity(): StockInEntity {
    return StockInEntity(
        stockInId = stockInId,
        productId = productId,
        purchaseQuantity = purchaseQuantity,
        purchaseUnit = purchaseUnit,
        conversionFactor = conversionFactor,
        stockQuantity = stockQuantity,
        note = note,
        createdAt = createdAt,
        createdBy = createdBy
    )
}
