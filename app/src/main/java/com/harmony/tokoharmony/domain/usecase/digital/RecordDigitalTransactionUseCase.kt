package com.harmony.tokoharmony.domain.usecase.digital

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.DigitalTransaction
import com.harmony.tokoharmony.domain.repository.DigitalTransactionRepository
import java.util.UUID
import javax.inject.Inject

class RecordDigitalTransactionUseCase @Inject constructor(
    private val digitalTransactionRepository: DigitalTransactionRepository
) {
    suspend operator fun invoke(digitalTransaction: DigitalTransaction): Result<DigitalTransaction> {
        return invoke(
            transactionItemId = digitalTransaction.transactionItemId,
            serviceType = digitalTransaction.serviceType,
            customerNumber = digitalTransaction.customerNumber,
            nominal = digitalTransaction.nominal,
            providerReference = digitalTransaction.providerReference,
            status = digitalTransaction.status,
            digitalTransactionId = digitalTransaction.digitalTransactionId
        )
    }

    suspend operator fun invoke(
        transactionItemId: String,
        serviceType: String,
        customerNumber: String,
        nominal: Long,
        providerReference: String?,
        status: String = "SUCCESS",
        digitalTransactionId: String = UUID.randomUUID().toString()
    ): Result<DigitalTransaction> {
        val cleanServiceType = serviceType.trim()
        val cleanCustomerNumber = customerNumber.trim()
        val cleanStatus = status.trim().uppercase()

        if (cleanServiceType.isBlank()) {
            return Result.Error(AppError.Validation("Jenis layanan produk digital tidak boleh kosong."))
        }
        if (cleanCustomerNumber.isBlank()) {
            return Result.Error(AppError.Validation("Nomor pelanggan / nomor tujuan tidak boleh kosong."))
        }
        if (nominal < 0) {
            return Result.Error(AppError.Validation("Nominal layanan digital tidak boleh bernilai negatif."))
        }
        val validStatuses = listOf("PENDING", "SUCCESS", "FAILED")
        if (cleanStatus !in validStatuses) {
            return Result.Error(AppError.Validation("Status transaksi digital harus salah satu dari: PENDING, SUCCESS, FAILED."))
        }

        val digitalTx = DigitalTransaction(
            digitalTransactionId = digitalTransactionId,
            transactionItemId = transactionItemId,
            serviceType = cleanServiceType,
            customerNumber = cleanCustomerNumber,
            nominal = nominal,
            providerReference = providerReference?.trim()?.ifBlank { null },
            status = cleanStatus,
            createdAt = System.currentTimeMillis()
        )

        return when (val saveResult = digitalTransactionRepository.recordDigitalTransaction(digitalTx)) {
            is Result.Success -> Result.Success(digitalTx)
            is Result.Error -> saveResult
        }
    }
}
