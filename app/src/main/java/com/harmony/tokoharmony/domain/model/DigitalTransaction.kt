package com.harmony.tokoharmony.domain.model

data class DigitalTransaction(
    val digitalTransactionId: String,
    val transactionItemId: String,
    val serviceType: String,
    val customerNumber: String,
    val nominal: Long,
    val providerReference: String? = null,
    val status: String = "SUCCESS", // "PENDING", "SUCCESS", "FAILED"
    val createdAt: Long = System.currentTimeMillis()
) {
    // Backward-compatibility properties for existing sync mappers
    val providerName: String get() = serviceType
    val targetAccount: String get() = customerNumber
}
