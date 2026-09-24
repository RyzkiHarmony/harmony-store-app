package com.harmony.tokoharmony.data.local.mapper

import com.harmony.tokoharmony.core.database.entity.TransactionEntity
import com.harmony.tokoharmony.core.database.entity.TransactionItemEntity
import com.harmony.tokoharmony.domain.model.PaymentMethod
import com.harmony.tokoharmony.domain.model.PaymentStatus
import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.model.TransactionItem
import com.harmony.tokoharmony.domain.model.TransactionStatus

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        transactionId = transactionId,
        transactionNumber = transactionNumber,
        transactionStatus = TransactionStatus.valueOf(transactionStatus),
        paymentStatus = PaymentStatus.valueOf(paymentStatus),
        paymentMethod = paymentMethod?.let { PaymentMethod.valueOf(it) },
        totalAmount = totalAmount,
        amountReceived = amountReceived,
        changeAmount = changeAmount,
        createdAt = createdAt,
        completedAt = completedAt,
        cancelledAt = cancelledAt,
        createdBy = createdBy,
        cancelledBy = cancelledBy,
        cancellationReason = cancellationReason
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        transactionId = transactionId,
        transactionNumber = transactionNumber,
        transactionStatus = transactionStatus.name,
        paymentStatus = paymentStatus.name,
        paymentMethod = paymentMethod?.name,
        totalAmount = totalAmount,
        amountReceived = amountReceived,
        changeAmount = changeAmount,
        createdAt = createdAt,
        completedAt = completedAt,
        cancelledAt = cancelledAt,
        createdBy = createdBy,
        cancelledBy = cancelledBy,
        cancellationReason = cancellationReason
    )
}

fun TransactionItemEntity.toDomain(): TransactionItem {
    return TransactionItem(
        itemId = itemId,
        transactionId = transactionId,
        productId = productId,
        productNameSnapshot = productNameSnapshot,
        quantity = quantity,
        unitPrice = unitPrice,
        subtotal = subtotal,
        sellingUnit = sellingUnit
    )
}

fun TransactionItem.toEntity(): TransactionItemEntity {
    return TransactionItemEntity(
        itemId = itemId,
        transactionId = transactionId,
        productId = productId,
        productNameSnapshot = productNameSnapshot,
        quantity = quantity,
        unitPrice = unitPrice,
        subtotal = subtotal,
        sellingUnit = sellingUnit
    )
}
