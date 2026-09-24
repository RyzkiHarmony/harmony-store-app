package com.harmony.tokoharmony.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinManagerTest {

    @Test
    fun hashPin_producesValidFormatAndUniqueSalts() {
        val pin = "123456"
        val hash1 = PinManager.hashPin(pin)
        val hash2 = PinManager.hashPin(pin)

        assertTrue(hash1.contains(":"))
        assertTrue(hash2.contains(":"))
        assertNotEquals("Each hash should have a unique random salt", hash1, hash2)
    }

    @Test
    fun verifyPin_correctPin_returnsTrue() {
        val pin = "889900"
        val hash = PinManager.hashPin(pin)

        val isValid = PinManager.verifyPin(pin, hash)
        assertTrue("Verification should succeed for matching PIN", isValid)
    }

    @Test
    fun verifyPin_incorrectPin_returnsFalse() {
        val pin = "889900"
        val wrongPin = "889901"
        val hash = PinManager.hashPin(pin)

        val isValid = PinManager.verifyPin(wrongPin, hash)
        assertFalse("Verification should fail for incorrect PIN", isValid)
    }

    @Test
    fun verifyPin_invalidHashFormat_returnsFalse() {
        assertFalse(PinManager.verifyPin("123456", "invalid_format"))
        assertFalse(PinManager.verifyPin("123456", ""))
        assertFalse(PinManager.verifyPin("123456", "not_base64:also_not_base64"))
    }
}
