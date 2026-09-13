package com.finora.android.data.repository

import com.finora.android.core.model.Amount
import com.finora.android.data.local.dao.ExpenseDao
import com.finora.android.data.local.dao.RecurringExpenseDao
import com.finora.android.data.local.entity.ExpenseEntity
import com.finora.android.data.local.entity.RecurringExpenseEntity
import com.finora.android.domain.model.RecurringCalculator
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface RecurringRepository {
    fun getAllRecurringFlow(profileId: String): Flow<List<RecurringExpenseEntity>>
    fun getActiveRecurringFlow(profileId: String): Flow<List<RecurringExpenseEntity>>
    suspend fun getRecurringById(id: String, profileId: String): RecurringExpenseEntity?
    suspend fun createRecurring(
        profileId: String,
        title: String,
        amount: Amount,
        currencyCode: String,
        categoryId: String,
        frequency: String,
        startDate: Long,
        nextDueDate: Long,
        accountId: String? = null
    ): RecurringExpenseEntity
    suspend fun updateRecurring(recurring: RecurringExpenseEntity)
    suspend fun deleteRecurring(recurring: RecurringExpenseEntity)
    suspend fun logOccurrence(recurring: RecurringExpenseEntity, defaultPaymentMethodId: String): ExpenseEntity
}

class RecurringRepositoryImpl(
    private val recurringDao: RecurringExpenseDao,
    private val expenseDao: ExpenseDao
) : RecurringRepository {

    override fun getAllRecurringFlow(profileId: String): Flow<List<RecurringExpenseEntity>> =
        recurringDao.getAllRecurringFlow(profileId)

    override fun getActiveRecurringFlow(profileId: String): Flow<List<RecurringExpenseEntity>> =
        recurringDao.getActiveRecurringFlow(profileId)

    override suspend fun getRecurringById(id: String, profileId: String): RecurringExpenseEntity? =
        recurringDao.getRecurringById(id, profileId)

    override suspend fun createRecurring(
        profileId: String,
        title: String,
        amount: Amount,
        currencyCode: String,
        categoryId: String,
        frequency: String,
        startDate: Long,
        nextDueDate: Long,
        accountId: String?
    ): RecurringExpenseEntity {
        require(title.isNotBlank()) { "Subscription title cannot be blank." }
        require(amount.minorUnits > 0L) { "Amount must be strictly greater than zero." }

        val now = System.currentTimeMillis()
        val item = RecurringExpenseEntity(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            title = title.trim(),
            amountMinorUnits = amount.minorUnits,
            currencyCode = currencyCode,
            categoryId = categoryId,
            frequency = frequency.uppercase(),
            startDate = startDate,
            nextDueDate = nextDueDate,
            accountId = accountId,
            isActive = true,
            autoLog = false,
            createdAt = now,
            updatedAt = now
        )
        recurringDao.insertRecurring(item)
        return item
    }

    override suspend fun updateRecurring(recurring: RecurringExpenseEntity) {
        recurringDao.updateRecurring(recurring.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteRecurring(recurring: RecurringExpenseEntity) {
        recurringDao.deleteRecurring(recurring)
    }

    override suspend fun logOccurrence(
        recurring: RecurringExpenseEntity,
        defaultPaymentMethodId: String
    ): ExpenseEntity {
        val now = System.currentTimeMillis()
        val expense = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            profileId = recurring.profileId,
            amountMinorUnits = recurring.amountMinorUnits,
            currencyCode = recurring.currencyCode,
            categoryId = recurring.categoryId,
            paymentMethodId = defaultPaymentMethodId,
            notes = "Recurring: ${recurring.title}",
            expenseDate = recurring.nextDueDate,
            createdAt = now,
            updatedAt = now
        )
        expenseDao.insertExpense(expense)

        // Advance next due date
        val nextDue = RecurringCalculator.computeNextDueDate(recurring.nextDueDate, recurring.frequency)
        recurringDao.updateRecurring(
            recurring.copy(
                nextDueDate = nextDue,
                updatedAt = now
            )
        )

        return expense
    }
}
