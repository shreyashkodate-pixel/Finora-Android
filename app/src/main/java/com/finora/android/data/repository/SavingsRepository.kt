package com.finora.android.data.repository

import com.finora.android.core.model.Amount
import com.finora.android.data.local.dao.SavingsGoalDao
import com.finora.android.data.local.entity.SavingsContributionEntity
import com.finora.android.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface SavingsRepository {
    fun getGoalsFlow(profileId: String): Flow<List<SavingsGoalEntity>>
    suspend fun getGoalById(id: String, profileId: String): SavingsGoalEntity?
    fun getContributionsForGoalFlow(goalId: String): Flow<List<SavingsContributionEntity>>
    fun getCurrentSavedAmountFlow(goalId: String): Flow<Long>
    suspend fun getCurrentSavedAmount(goalId: String): Long
    suspend fun createGoal(
        profileId: String,
        name: String,
        targetAmount: Amount,
        currencyCode: String,
        targetDate: Long? = null,
        templateType: String = "CUSTOM",
        colorHex: String = "#4CAF50",
        iconName: String = "Savings"
    ): SavingsGoalEntity
    suspend fun addContribution(
        goalId: String,
        profileId: String,
        amount: Amount,
        type: String = "DEPOSIT", // DEPOSIT or WITHDRAWAL
        notes: String? = null
    ): SavingsContributionEntity
    suspend fun updateGoal(goal: SavingsGoalEntity)
    suspend fun deleteGoal(goal: SavingsGoalEntity)
}

class SavingsRepositoryImpl(
    private val savingsDao: SavingsGoalDao
) : SavingsRepository {

    override fun getGoalsFlow(profileId: String): Flow<List<SavingsGoalEntity>> =
        savingsDao.getGoalsFlow(profileId)

    override suspend fun getGoalById(id: String, profileId: String): SavingsGoalEntity? =
        savingsDao.getGoalById(id, profileId)

    override fun getContributionsForGoalFlow(goalId: String): Flow<List<SavingsContributionEntity>> =
        savingsDao.getContributionsForGoalFlow(goalId)

    override fun getCurrentSavedAmountFlow(goalId: String): Flow<Long> =
        savingsDao.getCurrentSavedAmountFlow(goalId)

    override suspend fun getCurrentSavedAmount(goalId: String): Long =
        savingsDao.getCurrentSavedAmount(goalId)

    override suspend fun createGoal(
        profileId: String,
        name: String,
        targetAmount: Amount,
        currencyCode: String,
        targetDate: Long?,
        templateType: String,
        colorHex: String,
        iconName: String
    ): SavingsGoalEntity {
        require(name.isNotBlank()) { "Goal name cannot be blank." }
        require(targetAmount.minorUnits > 0L) { "Target amount must be strictly greater than zero." }

        val now = System.currentTimeMillis()
        val goal = SavingsGoalEntity(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            name = name.trim(),
            targetAmountMinorUnits = targetAmount.minorUnits,
            currencyCode = currencyCode,
            targetDate = targetDate,
            templateType = templateType,
            colorHex = colorHex,
            iconName = iconName,
            isCompleted = false,
            createdAt = now,
            updatedAt = now
        )
        savingsDao.insertGoal(goal)
        return goal
    }

    override suspend fun addContribution(
        goalId: String,
        profileId: String,
        amount: Amount,
        type: String,
        notes: String?
    ): SavingsContributionEntity {
        require(amount.minorUnits > 0L) { "Contribution amount must be strictly greater than zero." }

        val contribution = SavingsContributionEntity(
            id = UUID.randomUUID().toString(),
            goalId = goalId,
            profileId = profileId,
            amountMinorUnits = amount.minorUnits,
            contributionDate = System.currentTimeMillis(),
            type = type.uppercase(),
            notes = notes?.trim()?.ifBlank { null }
        )
        savingsDao.insertContribution(contribution)

        // Check if target reached after deposit
        val currentSaved = savingsDao.getCurrentSavedAmount(goalId)
        val goal = savingsDao.getGoalById(goalId, profileId)
        if (goal != null && currentSaved >= goal.targetAmountMinorUnits && !goal.isCompleted) {
            savingsDao.updateGoal(goal.copy(isCompleted = true, updatedAt = System.currentTimeMillis()))
        }

        return contribution
    }

    override suspend fun updateGoal(goal: SavingsGoalEntity) {
        savingsDao.updateGoal(goal.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteGoal(goal: SavingsGoalEntity) {
        savingsDao.deleteGoal(goal)
    }
}
