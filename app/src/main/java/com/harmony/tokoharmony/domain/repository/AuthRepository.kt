package com.harmony.tokoharmony.domain.repository

import com.harmony.tokoharmony.domain.model.User

interface AuthRepository {
    suspend fun getAdminUser(): User?
    suspend fun getCashierUser(): User?
    suspend fun setAdminPin(pin: String)
    suspend fun verifyAdminPin(pin: String): Boolean
    suspend fun hasAdminPin(): Boolean
    suspend fun ensureDefaultUsers()
}
