package com.harmony.tokoharmony.data.repository

import com.harmony.tokoharmony.core.database.dao.UserDao
import com.harmony.tokoharmony.core.database.entity.UserEntity
import com.harmony.tokoharmony.core.security.PinManager
import com.harmony.tokoharmony.data.local.mapper.toDomain
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.AuthRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : AuthRepository {

    override suspend fun getAdminUser(): User? {
        ensureDefaultUsers()
        return userDao.getAdminUser()?.toDomain()
    }

    override suspend fun getCashierUser(): User? {
        ensureDefaultUsers()
        return userDao.getActiveUsersByRole(Role.CASHIER.name).firstOrNull()?.toDomain()
    }

    override suspend fun setAdminPin(pin: String) {
        ensureDefaultUsers()
        val admin = userDao.getAdminUser() ?: return
        val hashedPin = PinManager.hashPin(pin)
        val updatedAdmin = admin.copy(
            pinHash = hashedPin,
            updatedAt = System.currentTimeMillis()
        )
        userDao.updateUser(updatedAdmin)
    }

    override suspend fun verifyAdminPin(pin: String): Boolean {
        ensureDefaultUsers()
        val admin = userDao.getAdminUser() ?: return false
        val storedHash = admin.pinHash ?: return false
        return PinManager.verifyPin(pin, storedHash)
    }

    override suspend fun hasAdminPin(): Boolean {
        ensureDefaultUsers()
        val admin = userDao.getAdminUser() ?: return false
        return !admin.pinHash.isNullOrBlank()
    }

    override suspend fun ensureDefaultUsers() {
        val admin = userDao.getAdminUser()
        if (admin == null) {
            val defaultAdmin = UserEntity(
                userId = "admin-primary",
                displayName = "Admin / Orang Tua",
                role = Role.ADMIN.name,
                pinHash = null,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            userDao.insertUser(defaultAdmin)
        }

        val cashier = userDao.getActiveUsersByRole(Role.CASHIER.name).firstOrNull()
        if (cashier == null) {
            val defaultCashier = UserEntity(
                userId = "cashier-primary",
                displayName = "Kasir Toko",
                role = Role.CASHIER.name,
                pinHash = null,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            userDao.insertUser(defaultCashier)
        }
    }
}
