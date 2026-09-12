package com.finora.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finora.android.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("""
        SELECT * FROM budgets 
        WHERE profileId = :profileId AND yearMonth = :yearMonth
    """)
    fun getBudgetsForMonthFlow(profileId: String, yearMonth: String): Flow<List<BudgetEntity>>

    @Query("""
        SELECT * FROM budgets 
        WHERE profileId = :profileId AND yearMonth = :yearMonth AND categoryId IS NULL 
        LIMIT 1
    """)
    fun getOverallBudgetFlow(profileId: String, yearMonth: String): Flow<BudgetEntity?>

    @Query("""
        SELECT * FROM budgets 
        WHERE profileId = :profileId AND yearMonth = :yearMonth AND categoryId IS NULL 
        LIMIT 1
    """)
    suspend fun getOverallBudget(profileId: String, yearMonth: String): BudgetEntity?

    @Query("""
        SELECT * FROM budgets 
        WHERE profileId = :profileId AND yearMonth = :yearMonth AND categoryId = :categoryId 
        LIMIT 1
    """)
    suspend fun getCategoryBudget(profileId: String, yearMonth: String, categoryId: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id AND profileId = :profileId")
    suspend fun deleteBudgetById(id: String, profileId: String)
}
