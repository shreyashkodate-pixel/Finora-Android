package com.finora.android.core.di

import android.content.Context
import com.finora.android.data.local.FinoraDatabase
import com.finora.android.data.repository.AccountRepository
import com.finora.android.data.repository.AccountRepositoryImpl
import com.finora.android.data.repository.BudgetRepository
import com.finora.android.data.repository.BudgetRepositoryImpl
import com.finora.android.data.repository.CategoryRepository
import com.finora.android.data.repository.CategoryRepositoryImpl
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ExpenseRepositoryImpl
import com.finora.android.data.repository.IncomeRepository
import com.finora.android.data.repository.IncomeRepositoryImpl
import com.finora.android.data.repository.PaymentMethodRepository
import com.finora.android.data.repository.PaymentMethodRepositoryImpl
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.data.repository.ProfileRepositoryImpl
import com.finora.android.data.repository.RecurringRepository
import com.finora.android.data.repository.RecurringRepositoryImpl
import com.finora.android.data.repository.SavingsRepository
import com.finora.android.data.repository.SavingsRepositoryImpl

/**
 * Service locator providing repository singletons across the app.
 */
object DatabaseModule {

    @Volatile
    private var database: FinoraDatabase? = null

    val profileRepository: ProfileRepository by lazy {
        val db = checkNotNull(database) { "DatabaseModule must be initialized with Context first" }
        ProfileRepositoryImpl(db.profileDao(), db.categoryDao(), db.paymentMethodDao())
    }

    val categoryRepository: CategoryRepository by lazy {
        val db = checkNotNull(database) { "DatabaseModule must be initialized with Context first" }
        CategoryRepositoryImpl(db.categoryDao(), db.expenseDao())
    }

    val paymentMethodRepository: PaymentMethodRepository by lazy {
        val db = checkNotNull(database) { "DatabaseModule must be initialized with Context first" }
        PaymentMethodRepositoryImpl(db.paymentMethodDao())
    }

    val expenseRepository: ExpenseRepository by lazy {
        val db = checkNotNull(database) { "DatabaseModule must be initialized with Context first" }
        ExpenseRepositoryImpl(db.expenseDao())
    }

    val budgetRepository: BudgetRepository by lazy {
        val db = checkNotNull(database) { "DatabaseModule must be initialized with Context first" }
        BudgetRepositoryImpl(db.budgetDao())
    }

    val accountRepository: AccountRepository by lazy {
        val db = checkNotNull(database) { "DatabaseModule must be initialized with Context first" }
        AccountRepositoryImpl(db.accountDao())
    }

    val incomeRepository: IncomeRepository by lazy {
        val db = checkNotNull(database) { "DatabaseModule must be initialized with Context first" }
        IncomeRepositoryImpl(db.incomeDao())
    }

    val recurringRepository: RecurringRepository by lazy {
        val db = checkNotNull(database) { "DatabaseModule must be initialized with Context first" }
        RecurringRepositoryImpl(db.recurringExpenseDao(), db.expenseDao())
    }

    val savingsRepository: SavingsRepository by lazy {
        val db = checkNotNull(database) { "DatabaseModule must be initialized with Context first" }
        SavingsRepositoryImpl(db.savingsGoalDao())
    }

    fun getDatabase(): FinoraDatabase {
        return checkNotNull(database) { "DatabaseModule must be initialized with Context first" }
    }

    fun initialize(context: Context) {
        if (database == null) {
            synchronized(this) {
                if (database == null) {
                    database = FinoraDatabase.getInstance(context)
                }
            }
        }
    }
}
