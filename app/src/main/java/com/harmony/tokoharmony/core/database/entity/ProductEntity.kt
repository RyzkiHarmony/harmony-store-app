package com.harmony.tokoharmony.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["category_id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["name"]),
        Index(value = ["category_id"]),
        Index(value = ["is_active"])
    ]
)
data class ProductEntity(
    @PrimaryKey
    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "category_id")
    val categoryId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "barcode")
    val barcode: String?,

    @ColumnInfo(name = "product_kind")
    val productKind: String, // "PHYSICAL" or "DIGITAL"

    @ColumnInfo(name = "pricing_method")
    val pricingMethod: String, // "PER_UNIT" or "PER_KG"

    @ColumnInfo(name = "quantity_type")
    val quantityType: String, // "COUNT" or "GRAM"

    @ColumnInfo(name = "selling_unit")
    val sellingUnit: String,

    @ColumnInfo(name = "stock_unit")
    val stockUnit: String,

    @ColumnInfo(name = "purchase_unit")
    val purchaseUnit: String?,

    @ColumnInfo(name = "purchase_conversion_factor")
    val purchaseConversionFactor: Long?,

    @ColumnInfo(name = "current_price")
    val currentPrice: Long, // integer Rupiah

    @ColumnInfo(name = "minimum_stock")
    val minimumStock: Long?,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
