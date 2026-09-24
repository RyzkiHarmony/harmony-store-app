package com.harmony.tokoharmony.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.harmony.tokoharmony.domain.model.DigitalTransaction

@Entity(
    tableName = "digital_transactions",
    foreignKeys = [
        ForeignKey(
            entity = TransactionItemEntity::class,
            parentColumns = ["item_id"],
            childColumns = ["transaction_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transaction_item_id"]),
        Index(value = ["status"])
    ]
)
data class DigitalTransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "digital_transaction_id")
    val digitalTransactionId: String,

    @ColumnInfo(name = "transaction_item_id")
    val transactionItemId: String,

    @ColumnInfo(name = "service_type")
    val serviceType: String,

    @ColumnInfo(name = "customer_number")
    val customerNumber: String,

    @ColumnInfo(name = "nominal")
    val nominal: Long,

    @ColumnInfo(name = "provider_reference")
    val providerReference: String?,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
) {
    fun toDomain(): DigitalTransaction = DigitalTransaction(
        digitalTransactionId = digitalTransactionId,
        transactionItemId = transactionItemId,
        serviceType = serviceType,
        customerNumber = customerNumber,
        nominal = nominal,
        providerReference = providerReference,
        status = status,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: DigitalTransaction): DigitalTransactionEntity = DigitalTransactionEntity(
            digitalTransactionId = domain.digitalTransactionId,
            transactionItemId = domain.transactionItemId,
            serviceType = domain.serviceType,
            customerNumber = domain.customerNumber,
            nominal = domain.nominal,
            providerReference = domain.providerReference,
            status = domain.status,
            createdAt = domain.createdAt
        )
    }
}
