package com.harmony.tokoharmony.data.local.mapper

import com.harmony.tokoharmony.core.database.entity.ProductEntity
import com.harmony.tokoharmony.domain.model.PricingMethod
import com.harmony.tokoharmony.domain.model.Product
import com.harmony.tokoharmony.domain.model.ProductKind
import com.harmony.tokoharmony.domain.model.QuantityType

fun ProductEntity.toDomain(): Product {
    return Product(
        productId = productId,
        categoryId = categoryId,
        name = name,
        barcode = barcode,
        productKind = try {
            ProductKind.valueOf(productKind)
        } catch (_: Exception) {
            ProductKind.PHYSICAL
        },
        pricingMethod = try {
            PricingMethod.valueOf(pricingMethod)
        } catch (_: Exception) {
            PricingMethod.PER_UNIT
        },
        quantityType = try {
            QuantityType.valueOf(quantityType)
        } catch (_: Exception) {
            QuantityType.COUNT
        },
        sellingUnit = sellingUnit,
        stockUnit = stockUnit,
        purchaseUnit = purchaseUnit,
        purchaseConversionFactor = purchaseConversionFactor,
        currentPrice = currentPrice,
        minimumStock = minimumStock,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Product.toEntity(): ProductEntity {
    return ProductEntity(
        productId = productId,
        categoryId = categoryId,
        name = name,
        barcode = barcode?.takeIf { it.isNotBlank() },
        productKind = productKind.name,
        pricingMethod = pricingMethod.name,
        quantityType = quantityType.name,
        sellingUnit = sellingUnit,
        stockUnit = stockUnit,
        purchaseUnit = purchaseUnit?.takeIf { it.isNotBlank() },
        purchaseConversionFactor = purchaseConversionFactor,
        currentPrice = currentPrice,
        minimumStock = minimumStock,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
