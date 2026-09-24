package com.harmony.tokoharmony.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_adjustments",
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
data class StockAdjustmentEntity(
    @PrimaryKey
    @ColumnInfo(name = "adjustment_id")
    val adjustmentId: String,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "system_quantity")
    val systemQuantity: Long,

    @ColumnInfo(name = "physical_quantity")
    val physicalQuantity: Long,

    @ColumnInfo(name = "difference")
    val difference: Long,

    @ColumnInfo(name = "reason")
    val reason: String,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "created_by")
    val createdBy: String
)
