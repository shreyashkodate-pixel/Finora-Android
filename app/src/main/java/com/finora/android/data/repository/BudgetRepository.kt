package com.finora.android.data.repository

import com.finora.android.core.model.Amount
import com.finora.android.data.local.dao.BudgetDao
import com.finora.android.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface BudgetRepository {
    fun getBudgetsForMonthFlow(profileId: String, yearMonth: String): Flow<List<BudgetEntity>>
    fun getOverallBudgetFlow(profileId: String, yearMonth: String): Flow<BudgetEntity?>
    suspend fun getOverallBudget(profileId: String, yearMonth: String): BudgetEntity?
    suspend fun getCategoryBudget(profileId: String, yearMonth: String, categoryId: String): BudgetEntity?
    suspend fun setOverallBudget(profileId: String, yearMonth: String, amount: Amount): BudgetEntity
    suspend fun setCategoryBudget(profileId: String, yearMonth: String, categoryId: String, amount: Amount): BudgetEntity
    suspend fun deleteBudget(id: String, profileId: String)
}

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getBudgetsForMonthFlow(profileId: String, yearMonth: String): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonthFlow(profileId, yearMonth)

    override fun getOverallBudgetFlow(profileId: String, yearMonth: String): Flow<BudgetEntity?> =
        budgetDao.getOverallBudgetFlow(profileId, yearMonth)

    override suspend fun getOverallBudget(profileId: String, yearMonth: String): BudgetEntity? =
        budgetDao.getOverallBudget(profileId, yearMonth)

    override suspend fun getCategoryBudget(
        profileId: String,
        yearMonth: String,
        categoryId: String
    ): BudgetEntity? = budgetDao.getCategoryBudget(profileId, yearMonth, categoryId)

    override suspend fun setOverallBudget(
        profileId: String,
        yearMonth: String,
        amount: Amount
    ): BudgetEntity {
        require(amount.minorUnits > 0L) { "Budget amount must be strictly greater than zero." }
        val existing = budgetDao.getOverallBudget(profileId, yearMonth)
        val now = System.currentTimeMillis()
        val budget = BudgetEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            profileId = profileId,
            yearMonth = yearMonth,
            categoryId = null,
            amountMinorUnits = amount.minorUnits,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        budgetDao.upsertBudget(budget)
        return budget
    }

    override suspend fun setCategoryBudget(
        profileId: String,
        yearMonth: String,
        categoryId: String,
        amount: Amount
    ): BudgetEntity {
        require(amount.minorUnits > 0L) { "Budget amount must be strictly greater than zero." }
        val existing = budgetDao.getCategoryBudget(profileId, yearMonth, categoryId)
        val now = System.currentTimeMillis()
        val budget = BudgetEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            profileId = profileId,
            yearMonth = yearMonth,
            categoryId = categoryId,
            amountMinorUnits = amount.minorUnits,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        budgetDao.upsertBudget(budget)
        return budget
    }

    override suspend fun deleteBudget(id: String, profileId: String) {
        budgetDao.deleteBudgetById(id, profileId)
    }
}
