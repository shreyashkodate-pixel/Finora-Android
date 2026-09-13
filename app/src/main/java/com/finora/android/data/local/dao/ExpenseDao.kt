package com.finora.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.finora.android.data.local.entity.ExpenseEntity
import com.finora.android.data.local.relation.ExpenseWithDetails
import kotlinx.coroutines.flow.Flow

data class CategorySpend(
    val categoryId: String,
    val categoryName: String,
    val colorHex: String,
    val iconName: String,
    val totalAmount: Long,
    val transactionCount: Int
)

@Dao
interface ExpenseDao {

    @Transaction
    @Query("SELECT * FROM expenses WHERE profileId = :profileId ORDER BY expenseDate DESC, createdAt DESC")
    fun getAllExpensesWithDetailsFlow(profileId: String): Flow<List<ExpenseWithDetails>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :id AND profileId = :profileId LIMIT 1")
    fun getExpenseWithDetailsFlow(id: String, profileId: String): Flow<ExpenseWithDetails?>

    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun getExpenseWithDetails(id: String, profileId: String): ExpenseWithDetails?

    @Transaction
    @Query("""
        SELECT * FROM expenses 
        WHERE profileId = :profileId 
          AND expenseDate >= :startDate 
          AND expenseDate <= :endDate 
        ORDER BY expenseDate DESC, createdAt DESC
    """)
    fun getExpensesByDateRangeFlow(profileId: String, startDate: Long, endDate: Long): Flow<List<ExpenseWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM expenses 
        WHERE profileId = :profileId 
        ORDER BY expenseDate DESC, createdAt DESC 
        LIMIT :limit
    """)
    fun getRecentExpensesFlow(profileId: String, limit: Int = 5): Flow<List<ExpenseWithDetails>>

    @Query("""
        SELECT COALESCE(SUM(amountMinorUnits), 0) FROM expenses 
        WHERE profileId = :profileId 
          AND expenseDate >= :startDate 
          AND expenseDate <= :endDate
    """)
    fun getTotalSpendInDateRangeFlow(profileId: String, startDate: Long, endDate: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amountMinorUnits), 0) FROM expenses 
        WHERE profileId = :profileId 
          AND expenseDate >= :startDate 
          AND expenseDate <= :endDate
    """)
    suspend fun getTotalSpendInDateRange(profileId: String, startDate: Long, endDate: Long): Long

    @Query("""
        SELECT COALESCE(SUM(amountMinorUnits), 0) FROM expenses 
        WHERE profileId = :profileId 
          AND categoryId = :categoryId
          AND expenseDate >= :startDate 
          AND expenseDate <= :endDate
    """)
    suspend fun getCategorySpendInDateRange(profileId: String, categoryId: String, startDate: Long, endDate: Long): Long

    @Query("""
        SELECT 
            c.id AS categoryId,
            c.name AS categoryName,
            c.colorHex AS colorHex,
            c.iconName AS iconName,
            COALESCE(SUM(e.amountMinorUnits), 0) AS totalAmount,
            COUNT(e.id) AS transactionCount
        FROM categories c
        LEFT JOIN expenses e ON e.categoryId = c.id 
            AND e.profileId = :profileId 
            AND e.expenseDate >= :startDate 
            AND e.expenseDate <= :endDate
        WHERE c.profileId = :profileId
        GROUP BY c.id
        HAVING totalAmount > 0
        ORDER BY totalAmount DESC
    """)
    fun getCategorySpendBreakdownFlow(profileId: String, startDate: Long, endDate: Long): Flow<List<CategorySpend>>

    @Transaction
    @Query("""
        SELECT * FROM expenses 
        WHERE profileId = :profileId 
          AND expenseDate >= :startDate 
          AND expenseDate <= :endDate 
        ORDER BY amountMinorUnits DESC 
        LIMIT 1
    """)
    suspend fun getHighestExpenseInDateRange(profileId: String, startDate: Long, endDate: Long): ExpenseWithDetails?

    @Query("""
        UPDATE expenses 
        SET categoryId = :newCategoryId, updatedAt = :timestamp 
        WHERE categoryId = :oldCategoryId AND profileId = :profileId
    """)
    suspend fun reassignCategory(oldCategoryId: String, newCategoryId: String, profileId: String, timestamp: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id AND profileId = :profileId")
    suspend fun deleteExpenseById(id: String, profileId: String)

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpenses(): List<ExpenseEntity>

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)
}
