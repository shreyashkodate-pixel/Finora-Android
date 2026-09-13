package com.finora.android.core.backup

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupManagerTest {

    @Test
    fun testEncryptionAndDecryptionRoundTrip() {
        val magic = "FINORA_ENC".toByteArray(Charsets.US_ASCII)
        val formatVersion = 1
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val password = "StrongBackupPassword#123"

        // Create sample JSON
        val root = JSONObject().apply {
            put("formatVersion", 1)
            put("schemaVersion", 1)
            put("createdAt", 1726000000000L)
            put("expenses", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "exp-1")
                    put("amountMinorUnits", 50000L)
                })
            })
            put("categories", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "cat-1")
                    put("name", "Groceries")
                })
            })
        }

        val plaintextBytes = root.toString().toByteArray(Charsets.UTF_8)

        // Derive key
        val spec = PBEKeySpec(password.toCharArray(), salt, 10_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plaintextBytes)

        val output = ByteArrayOutputStream()
        output.write(magic)
        output.write(formatVersion)
        output.write(salt)
        output.write(iv)
        output.write(ciphertext)

        // Verify with BackupManager decryption logic
        val bytes = output.toByteArray()
        val inputStream = ByteArrayInputStream(bytes)

        val dummyManager = BackupManager()
        val (preview, json) = dummyManager.decryptAndPreview(password, inputStream, bytes.size.toLong())

        assertEquals(1, preview.expenseCount)
        assertEquals(1, preview.categoryCount)
        assertEquals(1, preview.schemaVersion)
        assertEquals(bytes.size.toLong(), preview.fileSizeBytes)
        assertNotNull(json.getJSONArray("expenses"))

        // Decrypt with incorrect password must throw InvalidBackupPasswordException
        try {
            dummyManager.decryptAndPreview("WrongPassword", ByteArrayInputStream(bytes))
            fail("Expected InvalidBackupPasswordException on wrong password")
        } catch (e: InvalidBackupPasswordException) {
            // Success
        }

        // Corrupt header must throw UnsupportedBackupFormatException
        val corruptedBytes = bytes.copyOf()
        corruptedBytes[0] = 'X'.code.toByte()
        try {
            dummyManager.decryptAndPreview(password, ByteArrayInputStream(corruptedBytes))
            fail("Expected UnsupportedBackupFormatException on corrupted header")
        } catch (e: UnsupportedBackupFormatException) {
            // Success
        }
    }
}
