package com.harmony.tokoharmony.domain.usecase.auth

import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.AuthRepository
import javax.inject.Inject

class GetCashierUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): User {
        authRepository.ensureDefaultUsers()
        return authRepository.getCashierUser()
            ?: authRepository.getAdminUser()
            ?: User(
                userId = "default-cashier",
                displayName = "Kasir",
                role = Role.CASHIER,
                pinHash = null
            )
    }
}
