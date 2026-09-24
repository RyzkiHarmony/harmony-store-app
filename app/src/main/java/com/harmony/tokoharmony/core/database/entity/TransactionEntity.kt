package com.harmony.tokoharmony.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["created_by"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["cancelled_by"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transaction_number"], unique = true),
        Index(value = ["transaction_status"]),
        Index(value = ["created_by"]),
        Index(value = ["cancelled_by"]),
        Index(value = ["created_at"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "transaction_id")
    val transactionId: String,

    @ColumnInfo(name = "transaction_number")
    val transactionNumber: String,

    @ColumnInfo(name = "transaction_status")
    val transactionStatus: String,

    @ColumnInfo(name = "payment_status")
    val paymentStatus: String,

    @ColumnInfo(name = "payment_method")
    val paymentMethod: String? = null,

    @ColumnInfo(name = "total_amount")
    val totalAmount: Long = 0L,

    @ColumnInfo(name = "amount_received")
    val amountReceived: Long? = null,

    @ColumnInfo(name = "change_amount")
    val changeAmount: Long = 0L,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    @ColumnInfo(name = "cancelled_at")
    val cancelledAt: Long? = null,

    @ColumnInfo(name = "created_by")
    val createdBy: String,

    @ColumnInfo(name = "cancelled_by")
    val cancelledBy: String? = null,

    @ColumnInfo(name = "cancellation_reason")
    val cancellationReason: String? = null
)
