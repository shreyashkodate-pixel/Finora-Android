package com.finora.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.finora.android.data.local.dao.BudgetDao
import com.finora.android.data.local.dao.CategoryDao
import com.finora.android.data.local.dao.ExpenseDao
import com.finora.android.data.local.dao.PaymentMethodDao
import com.finora.android.data.local.dao.ProfileDao
import com.finora.android.data.local.entity.BudgetEntity
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.ExpenseEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
import com.finora.android.data.local.entity.ProfileEntity

/**
 * Room Database for Finora — single local source of truth per SRS DC-1.
 */
@Database(
    entities = [
        ProfileEntity::class,
        CategoryEntity::class,
        PaymentMethodEntity::class,
        ExpenseEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FinoraDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        const val DATABASE_NAME = "finora.db"

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
            ).build()
        }
    }
}
