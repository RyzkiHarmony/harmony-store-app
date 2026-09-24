package com.harmony.tokoharmony.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["product_id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["changed_by"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["changed_by"]),
        Index(value = ["changed_at"])
    ]
)
data class PriceHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "history_id")
    val historyId: String,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "old_price")
    val oldPrice: Long,

    @ColumnInfo(name = "new_price")
    val newPrice: Long,

    @ColumnInfo(name = "changed_at")
    val changedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "changed_by")
    val changedBy: String
)
