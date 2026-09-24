package com.harmony.tokoharmony.domain.usecase.auth

import com.harmony.tokoharmony.domain.repository.AuthRepository
import javax.inject.Inject

class AuthenticateAdminUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(pin: String): Boolean {
        if (pin.length != 6 || !pin.all { it.isDigit() }) {
            return false
        }
        return authRepository.verifyAdminPin(pin)
    }
}
