package com.finora.android.data.repository

import com.finora.android.core.model.Amount
import com.finora.android.data.local.dao.CategorySpend
import com.finora.android.data.local.dao.ExpenseDao
import com.finora.android.data.local.entity.ExpenseEntity
import com.finora.android.data.local.relation.ExpenseWithDetails
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface ExpenseRepository {
    fun getAllExpensesFlow(profileId: String): Flow<List<ExpenseWithDetails>>
    fun getExpenseByIdFlow(id: String, profileId: String): Flow<ExpenseWithDetails?>
    suspend fun getExpenseById(id: String, profileId: String): ExpenseWithDetails?
    fun getExpensesByDateRangeFlow(profileId: String, startDate: Long, endDate: Long): Flow<List<ExpenseWithDetails>>
    fun getRecentExpensesFlow(profileId: String, limit: Int = 5): Flow<List<ExpenseWithDetails>>

    fun getTotalSpendInDateRangeFlow(profileId: String, startDate: Long, endDate: Long): Flow<Long>
    suspend fun getTotalSpendInDateRange(profileId: String, startDate: Long, endDate: Long): Long
    suspend fun getCategorySpendInDateRange(profileId: String, categoryId: String, startDate: Long, endDate: Long): Long
    fun getCategorySpendBreakdownFlow(profileId: String, startDate: Long, endDate: Long): Flow<List<CategorySpend>>
    suspend fun getHighestExpenseInDateRange(profileId: String, startDate: Long, endDate: Long): ExpenseWithDetails?

    suspend fun createExpense(
        profileId: String,
        amount: Amount,
        currencyCode: String,
        categoryId: String,
        expenseDate: Long,
        paymentMethodId: String? = null,
        title: String? = null,
        notes: String? = null,
        attachmentUri: String? = null,
        source: String = "MANUAL"
    ): ExpenseEntity

    suspend fun updateExpense(expense: ExpenseEntity)
    suspend fun deleteExpense(expense: ExpenseEntity)
    suspend fun deleteExpenseById(id: String, profileId: String)
}

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {

    override fun getAllExpensesFlow(profileId: String): Flow<List<ExpenseWithDetails>> =
        expenseDao.getAllExpensesWithDetailsFlow(profileId)

    override fun getExpenseByIdFlow(id: String, profileId: String): Flow<ExpenseWithDetails?> =
        expenseDao.getExpenseWithDetailsFlow(id, profileId)

    override suspend fun getExpenseById(id: String, profileId: String): ExpenseWithDetails? =
        expenseDao.getExpenseWithDetails(id, profileId)

    override fun getExpensesByDateRangeFlow(
        profileId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<ExpenseWithDetails>> =
        expenseDao.getExpensesByDateRangeFlow(profileId, startDate, endDate)

    override fun getRecentExpensesFlow(profileId: String, limit: Int): Flow<List<ExpenseWithDetails>> =
        expenseDao.getRecentExpensesFlow(profileId, limit)

    override fun getTotalSpendInDateRangeFlow(profileId: String, startDate: Long, endDate: Long): Flow<Long> =
        expenseDao.getTotalSpendInDateRangeFlow(profileId, startDate, endDate)

    override suspend fun getTotalSpendInDateRange(profileId: String, startDate: Long, endDate: Long): Long =
        expenseDao.getTotalSpendInDateRange(profileId, startDate, endDate)

    override suspend fun getCategorySpendInDateRange(
        profileId: String,
        categoryId: String,
        startDate: Long,
        endDate: Long
    ): Long = expenseDao.getCategorySpendInDateRange(profileId, categoryId, startDate, endDate)

    override fun getCategorySpendBreakdownFlow(
        profileId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<CategorySpend>> =
        expenseDao.getCategorySpendBreakdownFlow(profileId, startDate, endDate)

    override suspend fun getHighestExpenseInDateRange(
        profileId: String,
        startDate: Long,
        endDate: Long
    ): ExpenseWithDetails? =
        expenseDao.getHighestExpenseInDateRange(profileId, startDate, endDate)

    override suspend fun createExpense(
        profileId: String,
        amount: Amount,
        currencyCode: String,
        categoryId: String,
        expenseDate: Long,
        paymentMethodId: String?,
        title: String?,
        notes: String?,
        attachmentUri: String?,
        source: String
    ): ExpenseEntity {
        // Enforce SRS FR-EXP-V1.0-003: reject zero or negative amounts
        require(amount.minorUnits > 0L) {
            "Expense amount must be strictly greater than zero."
        }
        // Enforce SRS FR-EXP-V1.0-004: reject invalid dates
        require(expenseDate > 0L) {
            "Expense date must be a valid timestamp."
        }
        // Enforce SRS FR-EXP-V1.0-001: category ID and currency must not be blank
        require(categoryId.isNotBlank()) {
            "Expense category is required."
        }
        require(currencyCode.isNotBlank()) {
            "Transaction currency is required."
        }

        val now = System.currentTimeMillis()
        val expense = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            amountMinorUnits = amount.minorUnits,
            currencyCode = currencyCode.uppercase().trim(),
            categoryId = categoryId,
            paymentMethodId = paymentMethodId?.takeIf { it.isNotBlank() },
            expenseDate = expenseDate,
            title = title?.trim()?.takeIf { it.isNotEmpty() },
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            attachmentUri = attachmentUri,
            source = source,
            createdAt = now,
            updatedAt = now
        )
        expenseDao.insertExpense(expense)
        return expense
    }

    override suspend fun updateExpense(expense: ExpenseEntity) {
        require(expense.amountMinorUnits > 0L) { "Expense amount must be strictly greater than zero." }
        require(expense.expenseDate > 0L) { "Expense date must be a valid timestamp." }
        expenseDao.updateExpense(expense.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
    }

    override suspend fun deleteExpenseById(id: String, profileId: String) {
        expenseDao.deleteExpenseById(id, profileId)
    }
}
