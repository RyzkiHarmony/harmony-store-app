package com.harmony.tokoharmony.domain.usecase.transaction

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.model.TransactionStatus
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.TransactionRepository
import javax.inject.Inject

class CancelTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        transactionId: String,
        reason: String,
        user: User
    ): Result<Transaction> {
        if (user.role != Role.ADMIN) {
            return Result.Error(AppError.Unauthorized("Hanya Admin yang memiliki hak akses untuk membatalkan transaksi."))
        }

        val trimmedReason = reason.trim()
        if (trimmedReason.isBlank()) {
            return Result.Error(AppError.Validation("Alasan pembatalan transaksi wajib diisi."))
        }

        val tx = transactionRepository.getTransactionById(transactionId)
            ?: return Result.Error(AppError.NotFound("Transaksi tidak ditemukan."))

        if (tx.transactionStatus == TransactionStatus.CANCELLED) {
            return Result.Error(AppError.Validation("Transaksi sudah berstatus CANCELLED."))
        }

        if (tx.transactionStatus != TransactionStatus.COMPLETED) {
            return Result.Error(AppError.Validation("Hanya transaksi selesai (COMPLETED) yang dapat dibatalkan."))
        }

        return transactionRepository.cancelTransaction(
            transactionId = transactionId,
            reason = trimmedReason,
            userId = user.userId
        )
    }
}
