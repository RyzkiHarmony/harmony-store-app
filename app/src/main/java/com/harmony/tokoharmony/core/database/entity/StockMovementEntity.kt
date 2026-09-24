package com.harmony.tokoharmony.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
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
        Index(value = ["movement_type"]),
        Index(value = ["created_by"]),
        Index(value = ["created_at"]),
        Index(value = ["reference_id"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey
    @ColumnInfo(name = "movement_id")
    val movementId: String,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "movement_type")
    val movementType: String,

    @ColumnInfo(name = "quantity_delta")
    val quantityDelta: Long,

    @ColumnInfo(name = "reference_type")
    val referenceType: String? = null,

    @ColumnInfo(name = "reference_id")
    val referenceId: String? = null,

    @ColumnInfo(name = "reason")
    val reason: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "created_by")
    val createdBy: String
)
