package com.harmony.tokoharmony.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_ins",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["created_by"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["created_by"]),
        Index(value = ["created_at"])
    ]
)
data class StockInEntity(
    @PrimaryKey
    @ColumnInfo(name = "stock_in_id")
    val stockInId: String,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "purchase_quantity")
    val purchaseQuantity: Long,

    @ColumnInfo(name = "purchase_unit")
    val purchaseUnit: String,

    @ColumnInfo(name = "conversion_factor")
    val conversionFactor: Long,

    @ColumnInfo(name = "stock_quantity")
    val stockQuantity: Long,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "created_by")
    val createdBy: String
)
