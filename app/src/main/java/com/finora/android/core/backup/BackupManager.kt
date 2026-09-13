package com.finora.android.core.backup

import androidx.room.withTransaction
import com.finora.android.data.local.FinoraDatabase
import com.finora.android.data.local.entity.AccountEntity
import com.finora.android.data.local.entity.BudgetEntity
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.ExpenseEntity
import com.finora.android.data.local.entity.IncomeEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
import com.finora.android.data.local.entity.ProfileEntity
import com.finora.android.data.local.entity.RecurringExpenseEntity
import com.finora.android.data.local.entity.SavingsContributionEntity
import com.finora.android.data.local.entity.SavingsGoalEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Preview summary displayed to the user prior to committing a database restore per FR-BKP-V1.1-009.
 */
data class BackupPreview(
    val formatVersion: Int,
    val schemaVersion: Int,
    val createdAtEpochMillis: Long,
    val expenseCount: Int,
    val categoryCount: Int,
    val budgetCount: Int,
    val paymentMethodCount: Int,
    val accountCount: Int = 0,
    val incomeCount: Int = 0,
    val recurringCount: Int = 0,
    val savingsGoalCount: Int = 0,
    val fileSizeBytes: Long = 0L
)

enum class RestoreMode {
    REPLACE,
    MERGE
}

class InvalidBackupPasswordException : Exception("Incorrect backup password or corrupted backup file.")
class UnsupportedBackupFormatException(message: String) : Exception(message)

/**
 * Encrypted Backup & Atomic Restore Manager per SRS FR-BKP-V1.1-001 to 018.
 * Uses AES-256-GCM with PBKDF2WithHmacSHA256 key derivation.
 * Supports V1.0 and V1.2 schemas seamlessly.
 */
class BackupManager(private val database: FinoraDatabase? = null) {

    companion object {
        private val MAGIC_HEADER = "FINORA_ENC".toByteArray(Charsets.US_ASCII)
        private const val CURRENT_FORMAT_VERSION = 1
        private const val CURRENT_SCHEMA_VERSION = 2

        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 12 // Standard 96-bit GCM IV
        private const val GCM_TAG_LENGTH = 128 // 128-bit authentication tag
        private const val PBKDF2_ITERATIONS = 10_000
        private const val KEY_LENGTH = 256
    }

    suspend fun createBackup(password: String, outputStream: OutputStream) {
        val db = checkNotNull(database) { "Database required for creating backup" }
        require(password.isNotEmpty()) { "Backup password cannot be empty" }

        // 1. Collect all local data
        val profiles = db.profileDao().getAllProfiles()
        val categories = db.categoryDao().getAllCategories()
        val paymentMethods = db.paymentMethodDao().getAllPaymentMethods()
        val expenses = db.expenseDao().getAllExpenses()
        val budgets = db.budgetDao().getAllBudgets()
        val accounts = db.accountDao().getAllAccounts()
        val income = db.incomeDao().getAllIncome()
        val recurring = db.recurringExpenseDao().getAllRecurring()
        val savingsGoals = db.savingsGoalDao().getAllGoals()
        val savingsContributions = db.savingsGoalDao().getAllContributions()

        // 2. Build JSON package
        val root = JSONObject()
        root.put("formatVersion", CURRENT_FORMAT_VERSION)
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        root.put("createdAt", System.currentTimeMillis())

        // Profiles
        val profilesArr = JSONArray()
        profiles.forEach {
            profilesArr.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("currencyCode", it.currencyCode)
                put("themeMode", it.themeMode)
                put("createdAt", it.createdAt)
                put("updatedAt", it.updatedAt)
            })
        }
        root.put("profiles", profilesArr)

        // Categories
        val categoriesArr = JSONArray()
        categories.forEach {
            categoriesArr.put(JSONObject().apply {
                put("id", it.id)
                put("profileId", it.profileId)
                put("name", it.name)
                put("iconName", it.iconName)
                put("colorHex", it.colorHex)
                put("isDefault", it.isDefault)
                put("displayOrder", it.displayOrder)
                put("createdAt", it.createdAt)
                put("updatedAt", it.updatedAt)
            })
        }
        root.put("categories", categoriesArr)

        // Payment Methods
        val pmArr = JSONArray()
        paymentMethods.forEach {
            pmArr.put(JSONObject().apply {
                put("id", it.id)
                put("profileId", it.profileId)
                put("name", it.name)
                put("type", it.type)
                put("isDefault", it.isDefault)
                put("createdAt", it.createdAt)
                put("updatedAt", it.updatedAt)
            })
        }
        root.put("paymentMethods", pmArr)

        // Expenses
        val expensesArr = JSONArray()
        expenses.forEach {
            expensesArr.put(JSONObject().apply {
                put("id", it.id)
                put("profileId", it.profileId)
                put("amountMinorUnits", it.amountMinorUnits)
                put("currencyCode", it.currencyCode)
                put("categoryId", it.categoryId)
                put("paymentMethodId", it.paymentMethodId)
                put("expenseDate", it.expenseDate)
                put("title", it.title)
                put("notes", it.notes)
                put("attachmentUri", it.attachmentUri)
                put("source", it.source)
                put("isRecurring", it.isRecurring)
                put("createdAt", it.createdAt)
                put("updatedAt", it.updatedAt)
            })
        }
        root.put("expenses", expensesArr)

        // Budgets
        val budgetsArr = JSONArray()
        budgets.forEach {
            budgetsArr.put(JSONObject().apply {
                put("id", it.id)
                put("profileId", it.profileId)
                put("yearMonth", it.yearMonth)
                put("categoryId", it.categoryId)
                put("amountMinorUnits", it.amountMinorUnits)
                put("createdAt", it.createdAt)
                put("updatedAt", it.updatedAt)
            })
        }
        root.put("budgets", budgetsArr)

        // Accounts
        val accountsArr = JSONArray()
        accounts.forEach {
            accountsArr.put(JSONObject().apply {
                put("id", it.id)
                put("profileId", it.profileId)
                put("name", it.name)
                put("type", it.type)
                put("initialBalanceMinorUnits", it.initialBalanceMinorUnits)
                put("colorHex", it.colorHex)
                put("iconName", it.iconName)
                put("isDefault", it.isDefault)
                put("isArchived", it.isArchived)
                put("createdAt", it.createdAt)
                put("updatedAt", it.updatedAt)
            })
        }
        root.put("accounts", accountsArr)

        // Income
        val incomeArr = JSONArray()
        income.forEach {
            incomeArr.put(JSONObject().apply {
                put("id", it.id)
                put("profileId", it.profileId)
                put("amountMinorUnits", it.amountMinorUnits)
                put("currencyCode", it.currencyCode)
                put("source", it.source)
                put("incomeDate", it.incomeDate)
                put("accountId", it.accountId)
                put("notes", it.notes)
                put("createdAt", it.createdAt)
                put("updatedAt", it.updatedAt)
            })
        }
        root.put("income", incomeArr)

        // Recurring Expenses
        val recurringArr = JSONArray()
        recurring.forEach {
            recurringArr.put(JSONObject().apply {
                put("id", it.id)
                put("profileId", it.profileId)
                put("title", it.title)
                put("amountMinorUnits", it.amountMinorUnits)
                put("currencyCode", it.currencyCode)
                put("categoryId", it.categoryId)
                put("frequency", it.frequency)
                put("startDate", it.startDate)
                put("nextDueDate", it.nextDueDate)
                put("accountId", it.accountId)
                put("isActive", it.isActive)
                put("autoLog", it.autoLog)
                put("createdAt", it.createdAt)
                put("updatedAt", it.updatedAt)
            })
        }
        root.put("recurring", recurringArr)

        // Savings Goals
        val savingsGoalsArr = JSONArray()
        savingsGoals.forEach {
            savingsGoalsArr.put(JSONObject().apply {
                put("id", it.id)
                put("profileId", it.profileId)
                put("name", it.name)
                put("targetAmountMinorUnits", it.targetAmountMinorUnits)
                put("currencyCode", it.currencyCode)
                put("targetDate", it.targetDate)
                put("templateType", it.templateType)
                put("colorHex", it.colorHex)
                put("iconName", it.iconName)
                put("isCompleted", it.isCompleted)
                put("createdAt", it.createdAt)
                put("updatedAt", it.updatedAt)
            })
        }
        root.put("savingsGoals", savingsGoalsArr)

        // Savings Contributions
        val contributionsArr = JSONArray()
        savingsContributions.forEach {
            contributionsArr.put(JSONObject().apply {
                put("id", it.id)
                put("goalId", it.goalId)
                put("profileId", it.profileId)
                put("amountMinorUnits", it.amountMinorUnits)
                put("contributionDate", it.contributionDate)
                put("type", it.type)
                put("notes", it.notes)
                put("createdAt", it.createdAt)
            })
        }
        root.put("savingsContributions", contributionsArr)

        val plaintextBytes = root.toString().toByteArray(Charsets.UTF_8)

        // 3. Encrypt with AES-256-GCM
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val ciphertext = cipher.doFinal(plaintextBytes)

        // 4. Write binary container
        outputStream.write(MAGIC_HEADER)
        outputStream.write(CURRENT_FORMAT_VERSION)
        outputStream.write(salt)
        outputStream.write(iv)
        outputStream.write(ciphertext)
        outputStream.flush()
    }

    fun decryptAndPreview(password: String, inputStream: InputStream, fileSizeBytes: Long = 0L): Pair<BackupPreview, JSONObject> {
        val bytes = inputStream.readBytes()
        if (bytes.size < MAGIC_HEADER.size + 1 + SALT_LENGTH + IV_LENGTH) {
            throw UnsupportedBackupFormatException("Invalid backup file: header too short.")
        }

        // Validate Magic Header
        for (i in MAGIC_HEADER.indices) {
            if (bytes[i] != MAGIC_HEADER[i]) {
                throw UnsupportedBackupFormatException("File is not a valid Finora encrypted backup.")
            }
        }

        var offset = MAGIC_HEADER.size
        val formatVersion = bytes[offset].toInt()
        offset += 1
        if (formatVersion > CURRENT_FORMAT_VERSION) {
            throw UnsupportedBackupFormatException("Backup format v$formatVersion is not supported by this app version.")
        }

        val salt = bytes.copyOfRange(offset, offset + SALT_LENGTH)
        offset += SALT_LENGTH

        val iv = bytes.copyOfRange(offset, offset + IV_LENGTH)
        offset += IV_LENGTH

        val ciphertext = bytes.copyOfRange(offset, bytes.size)

        val plaintextBytes = try {
            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw InvalidBackupPasswordException()
        }

        val json = JSONObject(String(plaintextBytes, Charsets.UTF_8))
        val preview = BackupPreview(
            formatVersion = json.optInt("formatVersion", 1),
            schemaVersion = json.optInt("schemaVersion", 1),
            createdAtEpochMillis = json.optLong("createdAt", 0L),
            expenseCount = json.optJSONArray("expenses")?.length() ?: 0,
            categoryCount = json.optJSONArray("categories")?.length() ?: 0,
            budgetCount = json.optJSONArray("budgets")?.length() ?: 0,
            paymentMethodCount = json.optJSONArray("paymentMethods")?.length() ?: 0,
            accountCount = json.optJSONArray("accounts")?.length() ?: 0,
            incomeCount = json.optJSONArray("income")?.length() ?: 0,
            recurringCount = json.optJSONArray("recurring")?.length() ?: 0,
            savingsGoalCount = json.optJSONArray("savingsGoals")?.length() ?: 0,
            fileSizeBytes = if (fileSizeBytes > 0) fileSizeBytes else bytes.size.toLong()
        )

        return Pair(preview, json)
    }

    suspend fun restoreFromDecryptedJson(json: JSONObject, mode: RestoreMode) {
        val db = checkNotNull(database) { "Database required for restoring backup" }
        db.withTransaction {
            if (mode == RestoreMode.REPLACE) {
                // Clear existing records cleanly (reverse foreign key order)
                db.savingsGoalDao().deleteAllContributions()
                db.savingsGoalDao().deleteAllGoals()
                db.recurringExpenseDao().deleteAllRecurring()
                db.incomeDao().deleteAllIncome()
                db.accountDao().deleteAllAccounts()
                db.budgetDao().deleteAllBudgets()
                db.expenseDao().deleteAllExpenses()
                db.paymentMethodDao().deleteAllPaymentMethods()
                db.categoryDao().deleteAllCategories()
                db.profileDao().deleteAllProfiles()
            }

            // 1. Profiles
            val profilesArr = json.optJSONArray("profiles") ?: JSONArray()
            for (i in 0 until profilesArr.length()) {
                val obj = profilesArr.getJSONObject(i)
                db.profileDao().insertProfile(
                    ProfileEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        currencyCode = obj.getString("currencyCode"),
                        themeMode = obj.optString("themeMode", "SYSTEM"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // 2. Categories
            val categoriesArr = json.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until categoriesArr.length()) {
                val obj = categoriesArr.getJSONObject(i)
                db.categoryDao().insertCategory(
                    CategoryEntity(
                        id = obj.getString("id"),
                        profileId = obj.getString("profileId"),
                        name = obj.getString("name"),
                        iconName = obj.getString("iconName"),
                        colorHex = obj.getString("colorHex"),
                        isDefault = obj.optBoolean("isDefault", false),
                        displayOrder = obj.optInt("displayOrder", 0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // 3. Payment Methods
            val pmArr = json.optJSONArray("paymentMethods") ?: JSONArray()
            for (i in 0 until pmArr.length()) {
                val obj = pmArr.getJSONObject(i)
                db.paymentMethodDao().insertPaymentMethod(
                    PaymentMethodEntity(
                        id = obj.getString("id"),
                        profileId = obj.getString("profileId"),
                        name = obj.getString("name"),
                        type = obj.optString("type", "CASH"),
                        isDefault = obj.optBoolean("isDefault", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // 4. Accounts
            val accountsArr = json.optJSONArray("accounts") ?: JSONArray()
            for (i in 0 until accountsArr.length()) {
                val obj = accountsArr.getJSONObject(i)
                db.accountDao().insertAccount(
                    AccountEntity(
                        id = obj.getString("id"),
                        profileId = obj.getString("profileId"),
                        name = obj.getString("name"),
                        type = obj.optString("type", "BANK_ACCOUNT"),
                        initialBalanceMinorUnits = obj.optLong("initialBalanceMinorUnits", 0L),
                        colorHex = obj.optString("colorHex", "#1E88E5"),
                        iconName = obj.optString("iconName", "AccountBalance"),
                        isDefault = obj.optBoolean("isDefault", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // 5. Expenses
            val expensesArr = json.optJSONArray("expenses") ?: JSONArray()
            for (i in 0 until expensesArr.length()) {
                val obj = expensesArr.getJSONObject(i)
                db.expenseDao().insertExpense(
                    ExpenseEntity(
                        id = obj.getString("id"),
                        profileId = obj.getString("profileId"),
                        amountMinorUnits = obj.getLong("amountMinorUnits"),
                        currencyCode = obj.optString("currencyCode", "INR"),
                        categoryId = obj.getString("categoryId"),
                        paymentMethodId = if (obj.isNull("paymentMethodId")) null else obj.getString("paymentMethodId"),
                        expenseDate = obj.getLong("expenseDate"),
                        title = if (obj.isNull("title")) null else obj.getString("title"),
                        notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                        attachmentUri = if (obj.isNull("attachmentUri")) null else obj.getString("attachmentUri"),
                        source = obj.optString("source", "MANUAL"),
                        isRecurring = obj.optBoolean("isRecurring", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // 6. Budgets
            val budgetsArr = json.optJSONArray("budgets") ?: JSONArray()
            for (i in 0 until budgetsArr.length()) {
                val obj = budgetsArr.getJSONObject(i)
                db.budgetDao().upsertBudget(
                    BudgetEntity(
                        id = obj.getString("id"),
                        profileId = obj.getString("profileId"),
                        yearMonth = obj.getString("yearMonth"),
                        categoryId = if (obj.isNull("categoryId")) null else obj.getString("categoryId"),
                        amountMinorUnits = obj.getLong("amountMinorUnits"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // 7. Income
            val incomeArr = json.optJSONArray("income") ?: JSONArray()
            for (i in 0 until incomeArr.length()) {
                val obj = incomeArr.getJSONObject(i)
                db.incomeDao().insertIncome(
                    IncomeEntity(
                        id = obj.getString("id"),
                        profileId = obj.getString("profileId"),
                        amountMinorUnits = obj.getLong("amountMinorUnits"),
                        currencyCode = obj.optString("currencyCode", "INR"),
                        source = obj.getString("source"),
                        incomeDate = obj.getLong("incomeDate"),
                        accountId = if (obj.isNull("accountId")) null else obj.getString("accountId"),
                        notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // 8. Recurring Expenses
            val recurringArr = json.optJSONArray("recurring") ?: JSONArray()
            for (i in 0 until recurringArr.length()) {
                val obj = recurringArr.getJSONObject(i)
                db.recurringExpenseDao().insertRecurring(
                    RecurringExpenseEntity(
                        id = obj.getString("id"),
                        profileId = obj.getString("profileId"),
                        title = obj.getString("title"),
                        amountMinorUnits = obj.getLong("amountMinorUnits"),
                        currencyCode = obj.optString("currencyCode", "INR"),
                        categoryId = obj.getString("categoryId"),
                        frequency = obj.getString("frequency"),
                        startDate = obj.getLong("startDate"),
                        nextDueDate = obj.getLong("nextDueDate"),
                        accountId = if (obj.isNull("accountId")) null else obj.getString("accountId"),
                        isActive = obj.optBoolean("isActive", true),
                        autoLog = obj.optBoolean("autoLog", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // 9. Savings Goals
            val savingsGoalsArr = json.optJSONArray("savingsGoals") ?: JSONArray()
            for (i in 0 until savingsGoalsArr.length()) {
                val obj = savingsGoalsArr.getJSONObject(i)
                db.savingsGoalDao().insertGoal(
                    SavingsGoalEntity(
                        id = obj.getString("id"),
                        profileId = obj.getString("profileId"),
                        name = obj.getString("name"),
                        targetAmountMinorUnits = obj.getLong("targetAmountMinorUnits"),
                        currencyCode = obj.optString("currencyCode", "INR"),
                        targetDate = if (obj.isNull("targetDate")) null else obj.getLong("targetDate"),
                        templateType = obj.optString("templateType", "CUSTOM"),
                        colorHex = obj.optString("colorHex", "#4CAF50"),
                        iconName = obj.optString("iconName", "Savings"),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // 10. Savings Contributions
            val contributionsArr = json.optJSONArray("savingsContributions") ?: JSONArray()
            for (i in 0 until contributionsArr.length()) {
                val obj = contributionsArr.getJSONObject(i)
                db.savingsGoalDao().insertContribution(
                    SavingsContributionEntity(
                        id = obj.getString("id"),
                        goalId = obj.getString("goalId"),
                        profileId = obj.getString("profileId"),
                        amountMinorUnits = obj.getLong("amountMinorUnits"),
                        contributionDate = obj.getLong("contributionDate"),
                        type = obj.optString("type", "DEPOSIT"),
                        notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }
}
