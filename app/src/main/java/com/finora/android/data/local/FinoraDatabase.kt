package com.finora.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finora.android.data.local.dao.AccountDao
import com.finora.android.data.local.dao.BudgetDao
import com.finora.android.data.local.dao.CategoryDao
import com.finora.android.data.local.dao.ExpenseDao
import com.finora.android.data.local.dao.IncomeDao
import com.finora.android.data.local.dao.PaymentMethodDao
import com.finora.android.data.local.dao.ProfileDao
import com.finora.android.data.local.dao.RecurringExpenseDao
import com.finora.android.data.local.dao.SavingsGoalDao
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

/**
 * Room Database for Finora — single local source of truth per SRS DC-1.
 * Version 2 introduces V1.2 schemas: Accounts, Income, Recurring Expenses, and Savings Goals.
 */
@Database(
    entities = [
        ProfileEntity::class,
        CategoryEntity::class,
        PaymentMethodEntity::class,
        ExpenseEntity::class,
        BudgetEntity::class,
        AccountEntity::class,
        IncomeEntity::class,
        RecurringExpenseEntity::class,
        SavingsGoalEntity::class,
        SavingsContributionEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class FinoraDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun accountDao(): AccountDao
    abstract fun incomeDao(): IncomeDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun savingsGoalDao(): SavingsGoalDao

    companion object {
        const val DATABASE_NAME = "finora.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. accounts
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `accounts` (
                        `id` TEXT NOT NULL,
                        `profileId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `initialBalanceMinorUnits` INTEGER NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `iconName` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_profileId` ON `accounts` (`profileId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounts_profileId_name` ON `accounts` (`profileId`, `name`)")

                // 2. income
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `income` (
                        `id` TEXT NOT NULL,
                        `profileId` TEXT NOT NULL,
                        `amountMinorUnits` INTEGER NOT NULL,
                        `currencyCode` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `incomeDate` INTEGER NOT NULL,
                        `accountId` TEXT,
                        `notes` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_income_profileId` ON `income` (`profileId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_income_incomeDate` ON `income` (`incomeDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_income_accountId` ON `income` (`accountId`)")

                // 3. recurring_expenses
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recurring_expenses` (
                        `id` TEXT NOT NULL,
                        `profileId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amountMinorUnits` INTEGER NOT NULL,
                        `currencyCode` TEXT NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `nextDueDate` INTEGER NOT NULL,
                        `accountId` TEXT,
                        `isActive` INTEGER NOT NULL,
                        `autoLog` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_expenses_profileId` ON `recurring_expenses` (`profileId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_expenses_categoryId` ON `recurring_expenses` (`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_expenses_accountId` ON `recurring_expenses` (`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_expenses_nextDueDate` ON `recurring_expenses` (`nextDueDate`)")

                // 4. savings_goals
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` TEXT NOT NULL,
                        `profileId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `targetAmountMinorUnits` INTEGER NOT NULL,
                        `currencyCode` TEXT NOT NULL,
                        `targetDate` INTEGER,
                        `templateType` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `iconName` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_goals_profileId` ON `savings_goals` (`profileId`)")

                // 5. savings_contributions
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `savings_contributions` (
                        `id` TEXT NOT NULL,
                        `goalId` TEXT NOT NULL,
                        `profileId` TEXT NOT NULL,
                        `amountMinorUnits` INTEGER NOT NULL,
                        `contributionDate` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `notes` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`goalId`) REFERENCES `savings_goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_contributions_goalId` ON `savings_contributions` (`goalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_contributions_profileId` ON `savings_contributions` (`profileId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_contributions_contributionDate` ON `savings_contributions` (`contributionDate`)")
            }
        }

        @Volatile
        private var INSTANCE: FinoraDatabase? = null

        fun getInstance(context: Context): FinoraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): FinoraDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                FinoraDatabase::class.java,
                DATABASE_NAME
            ).addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
