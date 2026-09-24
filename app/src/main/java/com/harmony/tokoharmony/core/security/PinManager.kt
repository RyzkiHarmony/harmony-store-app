package com.harmony.tokoharmony.core.security

import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinManager {
    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    /**
     * Hashes a PIN with a randomly generated salt.
     * Returns a string in format: salt:hash (Base64 encoded).
     */
    fun hashPin(pin: String): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)

        val hash = pbkdf2(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hashBase64 = Base64.getEncoder().encodeToString(hash)

        return "$saltBase64:$hashBase64"
    }

    /**
     * Verifies that the entered PIN matches the stored salted hash.
     */
    fun verifyPin(pin: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false

        val salt = try {
            Base64.getDecoder().decode(parts[0])
        } catch (e: IllegalArgumentException) {
            return false
        }
        val expectedHash = try {
            Base64.getDecoder().decode(parts[1])
        } catch (e: IllegalArgumentException) {
            return false
        }

        val testHash = pbkdf2(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        return slowEquals(expectedHash, testHash)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLength)
        val skf = SecretKeyFactory.getInstance(ALGORITHM)
        return skf.generateSecret(spec).encoded
    }

    /**
     * Compares two byte arrays in constant time to prevent timing attacks.
     */
    private fun slowEquals(a: ByteArray, b: ByteArray): Boolean {
        var diff = a.size xor b.size
        for (i in a.indices) {
            diff = diff or (a[i].toInt() xor (if (i < b.size) b[i].toInt() else 0))
        }
        return diff == 0
    }
}
