package com.harmony.tokoharmony.domain.usecase.auth

import com.harmony.tokoharmony.core.common.AppError
import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.domain.repository.AuthRepository
import javax.inject.Inject

class SetAdminPinUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(pin: String, confirmPin: String): Result<Unit> {
        if (pin.length != 6 || !pin.all { it.isDigit() }) {
            return Result.Error(AppError.Validation("PIN harus terdiri dari 6 digit angka."))
        }
        if (pin != confirmPin) {
            return Result.Error(AppError.Validation("Konfirmasi PIN tidak cocok."))
        }

        return try {
            authRepository.setAdminPin(pin)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Database("Gagal menyimpan PIN: ${e.message}", e))
        }
    }
}
