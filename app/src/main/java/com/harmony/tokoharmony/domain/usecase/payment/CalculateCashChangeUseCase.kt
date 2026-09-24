package com.harmony.tokoharmony.domain.usecase.payment

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import javax.inject.Inject

class CalculateCashChangeUseCase @Inject constructor() {

    operator fun invoke(totalAmount: Long, amountReceived: Long): Result<Long> {
        if (totalAmount < 0) {
            return Result.Error(AppError.Validation("Total transaksi tidak valid."))
        }
        if (amountReceived < totalAmount) {
            val shortage = totalAmount - amountReceived
            return Result.Error(AppError.Validation("Uang diterima kurang sebesar Rp$shortage"))
        }
        return Result.Success(amountReceived - totalAmount)
    }
}
