package com.example.teminavigator.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Manages admin password on Android
 *
 * Password is encrypted with PBKDF2 (Password-Based Key Derivation Function 2),
 * which generates a salt and a hash.
 * Both are stored in SharedPreferences.
 */
class AdminPasswordStore(context: Context) {

    // Uses the application context to get shared preferences
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    // Verify password against stored hash; generate default password on first use
    @Synchronized
    fun verifyPassword(password: String): Boolean {
        var saltValue = preferences.getString(SALT_KEY, null)
        var hashValue = preferences.getString(HASH_KEY, null)

        // First use (no credentials yet)
        if (saltValue == null && hashValue == null) {
            savePassword(DEFAULT_PASSWORD)
            saltValue = preferences.getString(SALT_KEY, null)
            hashValue = preferences.getString(HASH_KEY, null)
        }

        if (saltValue == null || hashValue == null) return false

        val salt = Base64.decode(saltValue, Base64.NO_WRAP)
        val expectedHash = Base64.decode(hashValue, Base64.NO_WRAP)
        val actualHash = hashPassword(password, salt)
        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    @Synchronized
    fun changePassword(password: String) {
        require(password.length >= MIN_PASSWORD_LENGTH) {
            "Admin password must be at least $MIN_PASSWORD_LENGTH characters long"
        }
        savePassword(password)
    }

    // Generates fresh salt and hashes password with it
    // Both persist in  SharedPreferences
    private fun savePassword(password: String) {
        val salt = ByteArray(SALT_LENGTH).also(SecureRandom()::nextBytes)
        val hash = hashPassword(password, salt)

        val saved = preferences.edit()
            .putString(SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(HASH_KEY, Base64.encodeToString(hash, Base64.NO_WRAP))
            .commit()
        check(saved) { "Unable to save admin password" }
    }

    // Derives key from password and salt using HASH_ALGORITHM = "PBKDF2WithHmacSHA256"
    private fun hashPassword(password: String, salt: ByteArray): ByteArray {

        val passwordSpec = PBEKeySpec(
            password.toCharArray(),
            salt,
            HASH_ITERATIONS,
            HASH_LENGTH_BITS
        )

        // return the encoded key in raw bytes
        return try {
            SecretKeyFactory.getInstance(HASH_ALGORITHM)
                .generateSecret(passwordSpec)
                .encoded

        } finally {
            passwordSpec.clearPassword() // wipe plaintext password from memory
        }
    }

    // Constants for storage keys and hash parameters -> reset device if parameters are changed
    private companion object {
        const val PREFERENCES_NAME = "admin_security"   // SharedPreferences file name
        const val SALT_KEY = "password_salt"
        const val HASH_KEY = "password_hash"
        const val DEFAULT_PASSWORD = "admin"
        const val MIN_PASSWORD_LENGTH = 8
        const val SALT_LENGTH = 16                      // salt size in bytes -> 128 bits
        const val HASH_LENGTH_BITS = 256
        const val HASH_ITERATIONS = 210000
        const val HASH_ALGORITHM = "PBKDF2WithHmacSHA256"
    }
}
