package com.finora.android.core.security

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SecurityManagerTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var securityManager: SecurityManager

    @Before
    fun setup() {
        fakePrefs = FakeSharedPreferences()
        securityManager = SecurityManager(customPrefs = fakePrefs)
    }

    @Test
    fun testDefaultState() {
        assertFalse(securityManager.isAppLockEnabled())
        assertTrue(securityManager.isBiometricEnabled())
        assertFalse(securityManager.hasPinSet())
        assertFalse(securityManager.shouldRequireUnlock())
    }

    @Test
    fun testSetAndVerifyPin() {
        // Minimum 4 digits
        assertFalse(securityManager.setPin("12"))

        // Valid PIN
        assertTrue(securityManager.setPin("1234"))
        assertTrue(securityManager.hasPinSet())

        // Verification matches
        assertTrue(securityManager.verifyPin("1234"))
        assertTrue(securityManager.isSessionUnlocked)

        // Invalid PIN rejected
        assertFalse(securityManager.verifyPin("9999"))
        assertFalse(securityManager.verifyPin("123"))
    }

    @Test
    fun testAppLockRequirement() {
        securityManager.setPin("5678")
        securityManager.setAppLockEnabled(true)
        securityManager.markLocked()

        assertTrue(securityManager.shouldRequireUnlock())

        // Unlock
        assertTrue(securityManager.verifyPin("5678"))
        assertFalse(securityManager.shouldRequireUnlock())
    }

    @Test
    fun testLockTimeout() {
        securityManager.setPin("4321")
        securityManager.setAppLockEnabled(true)
        securityManager.setLockTimeout(SecurityManager.TIMEOUT_ONE_MINUTE)
        securityManager.markUnlocked()

        // Backgrounded recently (< 60s)
        securityManager.onAppBackgrounded()
        assertFalse(securityManager.shouldRequireUnlock())

        // Reset
        securityManager.resetLockData()
        assertFalse(securityManager.isAppLockEnabled())
        assertFalse(securityManager.hasPinSet())
    }
}

/**
 * In-memory FakeSharedPreferences for unit testing without Android runtime.
 */
class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = data

    override fun getString(key: String?, defValue: String?): String? =
        data[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String> ?: defValues)

    override fun getInt(key: String?, defValue: Int): Int =
        data[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        data[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        data[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        data[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(data)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val backingMap: MutableMap<String, Any>) : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) pending[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) pending[key] = null
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) backingMap.clear()
            pending.forEach { (k, v) ->
                if (v == null) backingMap.remove(k) else backingMap[k] = v
            }
            pending.clear()
        }
    }
}
