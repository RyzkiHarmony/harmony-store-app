package com.harmony.tokoharmony.domain.model

data class Product(
    val productId: String,
    val categoryId: String,
    val name: String,
    val barcode: String?,
    val productKind: ProductKind,
    val pricingMethod: PricingMethod,
    val quantityType: QuantityType,
    val sellingUnit: String,
    val stockUnit: String,
    val purchaseUnit: String? = null,
    val purchaseConversionFactor: Long? = null,
    val currentPrice: Long, // Integer Rupiah
    val minimumStock: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
