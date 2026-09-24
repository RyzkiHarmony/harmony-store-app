package com.harmony.tokoharmony.domain.usecase

import com.harmony.tokoharmony.core.common.Result
import com.harmony.tokoharmony.core.security.PinManager
import com.harmony.tokoharmony.domain.model.Role
import com.harmony.tokoharmony.domain.model.User
import com.harmony.tokoharmony.domain.repository.AuthRepository
import com.harmony.tokoharmony.domain.usecase.auth.AuthenticateAdminUseCase
import com.harmony.tokoharmony.domain.usecase.auth.HasAdminPinUseCase
import com.harmony.tokoharmony.domain.usecase.auth.SetAdminPinUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAuthRepository : AuthRepository {
    var adminUser: User? = User(
        userId = "admin-1",
        displayName = "Admin",
        role = Role.ADMIN,
        pinHash = null
    )

    override suspend fun getAdminUser(): User? = adminUser
    override suspend fun getCashierUser(): User? = User("cashier-1", "Kasir", Role.CASHIER, null)

    override suspend fun setAdminPin(pin: String) {
        adminUser = adminUser?.copy(pinHash = PinManager.hashPin(pin))
    }

    override suspend fun verifyAdminPin(pin: String): Boolean {
        val hash = adminUser?.pinHash ?: return false
        return PinManager.verifyPin(pin, hash)
    }

    override suspend fun hasAdminPin(): Boolean {
        return !adminUser?.pinHash.isNullOrBlank()
    }

    override suspend fun ensureDefaultUsers() {}
}

class AuthUseCaseTest {

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var setAdminPinUseCase: SetAdminPinUseCase
    private lateinit var authenticateAdminUseCase: AuthenticateAdminUseCase
    private lateinit var hasAdminPinUseCase: HasAdminPinUseCase

    @Before
    fun setup() {
        authRepository = FakeAuthRepository()
        setAdminPinUseCase = SetAdminPinUseCase(authRepository)
        authenticateAdminUseCase = AuthenticateAdminUseCase(authRepository)
        hasAdminPinUseCase = HasAdminPinUseCase(authRepository)
    }

    @Test
    fun pinLifecycle_setup_and_authenticate() = runTest {
        assertFalse(hasAdminPinUseCase())

        // Non 6-digit PIN rejected
        val invalidLength = setAdminPinUseCase("12345", "12345")
        assertTrue(invalidLength is Result.Error)

        // Mismatched confirmation rejected
        val mismatch = setAdminPinUseCase("123456", "654321")
        assertTrue(mismatch is Result.Error)

        // Valid 6-digit PIN
        val success = setAdminPinUseCase("123456", "123456")
        assertTrue(success is Result.Success)
        assertTrue(hasAdminPinUseCase())

        // Verify correct PIN
        assertTrue(authenticateAdminUseCase("123456"))

        // Verify incorrect PIN
        assertFalse(authenticateAdminUseCase("000000"))
    }
}
