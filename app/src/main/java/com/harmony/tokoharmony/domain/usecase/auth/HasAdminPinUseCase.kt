package com.harmony.tokoharmony.domain.usecase.auth

import com.harmony.tokoharmony.domain.repository.AuthRepository
import javax.inject.Inject

class HasAdminPinUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Boolean {
        return authRepository.hasAdminPin()
    }
}
