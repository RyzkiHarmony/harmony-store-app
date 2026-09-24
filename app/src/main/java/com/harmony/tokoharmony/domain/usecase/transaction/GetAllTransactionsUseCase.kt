package com.harmony.tokoharmony.domain.usecase.transaction

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.Transaction
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(user: User): Result<Flow<List<Transaction>>> {
        if (user.role != Role.ADMIN) {
            return Result.Error(AppError.Unauthorized("Hanya Admin yang dapat melihat riwayat transaksi."))
        }

        return Result.Success(transactionRepository.getAllTransactions())
    }
}
