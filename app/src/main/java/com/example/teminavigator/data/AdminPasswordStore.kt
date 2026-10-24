package com.example.teminavigator.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Manages the admin password of the Temi Navigator app on Android
 *
 * Password is encrypted with PBKDF2 (Password-Based Key Derivation Function 2),
 * which generates a salt and a hash.
 * Both are stored in SharedPreferences.
 *
 * Behavior summary:
 * - On the very first use (no credentials stored yet), a default password is generated and
 *   persisted, so verification succeeds with the default password
 * - Passwords have to be at least [MIN_PASSWORD_LENGTH] characters long when changed via
 *   [changePassword]
 * - The plaintext password is wiped from memory after hashing ([PBEKeySpec.clearPassword]).
 * - All public operations are [Synchronized] because they are called from
 *   background threads, which keeps concurrent verify/change operations consistent
 *
 * @param context   Any context; the application context is extracted so no Activity reference
 *                  is retained.
 */
class AdminPasswordStore(context: Context) {

    // Uses the application context to get shared preferences
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Verifies password against the stored hash; generate default password on first use
     *
     * The given password-string is re-hashed with the stored salt and compareed to the stored hash
     * using [MessageDigest.isEqual]
     *
     * @param password  The password string entered by the user
     * @return          `true` if the password matches the stored credentials
     */
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

    /**
     * Replaces the stored admin password with a new one.
     *
     * The new password must be at least [MIN_PASSWORD_LENGTH] characters long, otherwise an
     * [IllegalArgumentException] is thrown.
     * A new random salt is generated for the new password (see [savePassword])
     *
     * @param password                      The new admin password to be hashed and stored
     * @throws IllegalArgumentException     if password is too short
     */
    @Synchronized
    fun changePassword(password: String) {
        require(password.length >= MIN_PASSWORD_LENGTH) {
            "Admin password must be at least $MIN_PASSWORD_LENGTH characters long"
        }
        savePassword(password)
    }

    /**
     * Generates fresh salt and hashes password with it
     * Both persist in SharedPreferences
     *
     * @param password The password to store
     */
    private fun savePassword(password: String) {
        val salt = ByteArray(SALT_LENGTH).also(SecureRandom()::nextBytes)
        val hash = hashPassword(password, salt)

        val saved = preferences.edit()
            .putString(SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(HASH_KEY, Base64.encodeToString(hash, Base64.NO_WRAP))
            .commit()
        check(saved) { "Unable to save admin password" }
    }

    /**
     * Derives the password hash from the password and salt using
     * [HASH_ALGORITHM] ("PBKDF2WithHmacSHA256") with
     * [HASH_ITERATIONS] iterations and a
     * [HASH_LENGTH_BITS]-bit output.
     *
     * The plaintext password is wiped from memory in a `finally` block, even
     * if hashing fails.
     *
     * @param password  The password to hash.
     * @param salt      The salt to hash with
     * @return          The derived key in raw bytes.
     */
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
        const val SALT_KEY = "password_salt"            // Base64-encoded salt entry
        const val HASH_KEY = "password_hash"            // Base64-encoded hash entry
        const val DEFAULT_PASSWORD = "admin"            // used to initialize on first launch
        const val MIN_PASSWORD_LENGTH = 8               // minimum length
        const val SALT_LENGTH = 16                      // salt size in bytes -> 128 bits
        const val HASH_LENGTH_BITS = 256                // derived key length in bits
        const val HASH_ITERATIONS = 210000              // PBKDF2 iteration count
        const val HASH_ALGORITHM = "PBKDF2WithHmacSHA256"
    }
}
