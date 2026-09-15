package com.finora.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finora.android.data.local.entity.SavingsContributionEntity
import com.finora.android.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    @Query("SELECT * FROM savings_goals WHERE profileId = :profileId ORDER BY isCompleted ASC, createdAt DESC")
    fun getGoalsFlow(profileId: String): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun getGoalById(id: String, profileId: String): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoalEntity)

    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id AND profileId = :profileId")
    suspend fun deleteGoalById(id: String, profileId: String)

    // Contributions
    @Query("SELECT * FROM savings_contributions WHERE goalId = :goalId ORDER BY contributionDate DESC")
    fun getContributionsForGoalFlow(goalId: String): Flow<List<SavingsContributionEntity>>

    @Query("""
        SELECT COALESCE(
            SUM(CASE WHEN type = 'DEPOSIT' THEN amountMinorUnits ELSE -amountMinorUnits END),
            0
        )
        FROM savings_contributions WHERE goalId = :goalId
    """)
    fun getCurrentSavedAmountFlow(goalId: String): Flow<Long>

    @Query("""
        SELECT COALESCE(
            SUM(CASE WHEN type = 'DEPOSIT' THEN amountMinorUnits ELSE -amountMinorUnits END),
            0
        )
        FROM savings_contributions WHERE goalId = :goalId
    """)
    suspend fun getCurrentSavedAmount(goalId: String): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: SavingsContributionEntity)

    @Delete
    suspend fun deleteContribution(contribution: SavingsContributionEntity)

    // Backup & Restore helpers
    @Query("SELECT * FROM savings_goals")
    suspend fun getAllGoals(): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goals WHERE profileId = :profileId")
    suspend fun getGoalsByProfileId(profileId: String): List<SavingsGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<SavingsGoalEntity>)

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAllGoals()

    @Query("SELECT * FROM savings_contributions")
    suspend fun getAllContributions(): List<SavingsContributionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContributions(contributions: List<SavingsContributionEntity>)

    @Query("DELETE FROM savings_contributions")
    suspend fun deleteAllContributions()
}
