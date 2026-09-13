package com.finora.android.data.repository

import com.finora.android.core.model.Amount
import com.finora.android.data.local.dao.IncomeDao
import com.finora.android.data.local.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface IncomeRepository {
    fun getIncomeForProfileFlow(profileId: String): Flow<List<IncomeEntity>>
    fun getIncomeBetweenDatesFlow(profileId: String, startDate: Long, endDate: Long): Flow<List<IncomeEntity>>
    fun getTotalIncomeBetweenDatesFlow(profileId: String, startDate: Long, endDate: Long): Flow<Long>
    suspend fun getIncomeBetweenDates(profileId: String, startDate: Long, endDate: Long): List<IncomeEntity>
    suspend fun getTotalIncomeBetweenDates(profileId: String, startDate: Long, endDate: Long): Long
    suspend fun getIncomeById(id: String, profileId: String): IncomeEntity?
    suspend fun createIncome(
        profileId: String,
        amount: Amount,
        currencyCode: String,
        source: String,
        incomeDate: Long,
        accountId: String? = null,
        notes: String? = null
    ): IncomeEntity
    suspend fun updateIncome(income: IncomeEntity)
    suspend fun deleteIncome(income: IncomeEntity)
    suspend fun deleteIncomeById(id: String, profileId: String)
}

class IncomeRepositoryImpl(
    private val incomeDao: IncomeDao
) : IncomeRepository {

    override fun getIncomeForProfileFlow(profileId: String): Flow<List<IncomeEntity>> =
        incomeDao.getIncomeForProfileFlow(profileId)

    override fun getIncomeBetweenDatesFlow(
        profileId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<IncomeEntity>> = incomeDao.getIncomeBetweenDatesFlow(profileId, startDate, endDate)

    override fun getTotalIncomeBetweenDatesFlow(
        profileId: String,
        startDate: Long,
        endDate: Long
    ): Flow<Long> = incomeDao.getTotalIncomeBetweenDatesFlow(profileId, startDate, endDate)

    override suspend fun getIncomeBetweenDates(
        profileId: String,
        startDate: Long,
        endDate: Long
    ): List<IncomeEntity> = incomeDao.getIncomeBetweenDates(profileId, startDate, endDate)

    override suspend fun getTotalIncomeBetweenDates(
        profileId: String,
        startDate: Long,
        endDate: Long
    ): Long = incomeDao.getTotalIncomeBetweenDates(profileId, startDate, endDate)

    override suspend fun getIncomeById(id: String, profileId: String): IncomeEntity? =
        incomeDao.getIncomeById(id, profileId)

    override suspend fun createIncome(
        profileId: String,
        amount: Amount,
        currencyCode: String,
        source: String,
        incomeDate: Long,
        accountId: String?,
        notes: String?
    ): IncomeEntity {
        require(amount.minorUnits > 0L) { "Income amount must be greater than zero." }
        require(source.isNotBlank()) { "Income source cannot be blank." }

        val now = System.currentTimeMillis()
        val income = IncomeEntity(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            amountMinorUnits = amount.minorUnits,
            currencyCode = currencyCode,
            source = source.trim(),
            incomeDate = incomeDate,
            accountId = accountId,
            notes = notes?.trim()?.ifBlank { null },
            createdAt = now,
            updatedAt = now
        )
        incomeDao.insertIncome(income)
        return income
    }

    override suspend fun updateIncome(income: IncomeEntity) {
        require(income.amountMinorUnits > 0L) { "Income amount must be greater than zero." }
        incomeDao.updateIncome(income.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteIncome(income: IncomeEntity) {
        incomeDao.deleteIncome(income)
    }

    override suspend fun deleteIncomeById(id: String, profileId: String) {
        incomeDao.deleteIncomeById(id, profileId)
    }
}
