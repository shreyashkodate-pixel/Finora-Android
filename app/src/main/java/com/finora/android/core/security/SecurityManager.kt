package com.finora.android.core.security

import android.content.Context
import android.content.SharedPreferences
import java.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Manages App Lock, Biometrics settings, PIN hashing, and session timeouts per SRS FR-SEC-V1.1.
 * Raw PIN is never persisted; only PBKDF2 derived artifacts with random salt are stored.
 */
class SecurityManager(
    context: Context? = null,
    customPrefs: SharedPreferences? = null
) {

    private val prefs: SharedPreferences =
        customPrefs ?: context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ?: error("Context or SharedPreferences required")

    companion object {
        private const val PREFS_NAME = "finora_security_prefs"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_LOCK_TIMEOUT = "lock_timeout_ms"
        private const val KEY_LAST_BACKGROUND_TIME = "last_background_time"

        const val TIMEOUT_IMMEDIATELY = 0L
        const val TIMEOUT_ONE_MINUTE = 60_000L
        const val TIMEOUT_FIVE_MINUTES = 300_000L

        private const val PBKDF2_ITERATIONS = 10_000
        private const val HASH_KEY_LENGTH = 256
        private const val SALT_LENGTH = 16

        @Volatile
        private var INSTANCE: SecurityManager? = null

        fun getInstance(context: Context): SecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    var isSessionUnlocked: Boolean = false
        private set

    fun isAppLockEnabled(): Boolean = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)

    fun hasPinSet(): Boolean =
        prefs.getString(KEY_PIN_HASH, null) != null && prefs.getString(KEY_PIN_SALT, null) != null

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
        if (!enabled) {
            isSessionUnlocked = true
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getLockTimeout(): Long = prefs.getLong(KEY_LOCK_TIMEOUT, TIMEOUT_IMMEDIATELY)

    fun setLockTimeout(timeoutMs: Long) {
        prefs.edit().putLong(KEY_LOCK_TIMEOUT, timeoutMs).apply()
    }

    fun setPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)

        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, Base64.getEncoder().encodeToString(salt))
            .putString(KEY_PIN_HASH, Base64.getEncoder().encodeToString(hash))
            .apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val saltBase64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val hashBase64 = prefs.getString(KEY_PIN_HASH, null) ?: return false

        val salt = Base64.getDecoder().decode(saltBase64)
        val expectedHash = Base64.getDecoder().decode(hashBase64)
        val computedHash = hashPin(pin, salt)

        val matches = computedHash.contentEquals(expectedHash)
        if (matches) {
            markUnlocked()
        }
        return matches
    }

    fun markUnlocked() {
        isSessionUnlocked = true
    }

    fun markLocked() {
        isSessionUnlocked = false
    }

    fun onAppBackgrounded() {
        prefs.edit().putLong(KEY_LAST_BACKGROUND_TIME, System.currentTimeMillis()).apply()
    }

    fun shouldRequireUnlock(): Boolean {
        if (!isAppLockEnabled() || !hasPinSet()) return false
        if (!isSessionUnlocked) return true

        val lastBackground = prefs.getLong(KEY_LAST_BACKGROUND_TIME, 0L)
        if (lastBackground == 0L) return false

        val timeout = getLockTimeout()
        val elapsed = System.currentTimeMillis() - lastBackground
        return elapsed >= timeout
    }

    fun resetLockData() {
        prefs.edit()
            .remove(KEY_APP_LOCK_ENABLED)
            .remove(KEY_BIOMETRIC_ENABLED)
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .remove(KEY_LAST_BACKGROUND_TIME)
            .apply()
        isSessionUnlocked = true
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}
