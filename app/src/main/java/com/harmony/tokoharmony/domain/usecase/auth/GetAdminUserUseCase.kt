package com.harmony.tokoharmony.domain.usecase.auth

import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.AuthRepository
import javax.inject.Inject

class GetAdminUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): User? {
        return authRepository.getAdminUser()
    }
}
