package com.finora.android.data.repository

import com.finora.android.core.model.Amount
import com.finora.android.data.local.dao.AccountDao
import com.finora.android.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface AccountRepository {
    fun getActiveAccountsFlow(profileId: String): Flow<List<AccountEntity>>
    fun getAllAccountsFlow(profileId: String): Flow<List<AccountEntity>>
    suspend fun getAccountById(id: String, profileId: String): AccountEntity?
    suspend fun createAccount(
        profileId: String,
        name: String,
        type: String,
        initialBalance: Amount,
        colorHex: String = "#1E88E5",
        iconName: String = "AccountBalance",
        isDefault: Boolean = false
    ): AccountEntity
    suspend fun updateAccount(account: AccountEntity)
    suspend fun deleteAccount(account: AccountEntity)
    suspend fun setDefaultAccount(id: String, profileId: String)
}

class AccountRepositoryImpl(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getActiveAccountsFlow(profileId: String): Flow<List<AccountEntity>> =
        accountDao.getActiveAccountsFlow(profileId)

    override fun getAllAccountsFlow(profileId: String): Flow<List<AccountEntity>> =
        accountDao.getAllAccountsFlow(profileId)

    override suspend fun getAccountById(id: String, profileId: String): AccountEntity? =
        accountDao.getAccountById(id, profileId)

    override suspend fun createAccount(
        profileId: String,
        name: String,
        type: String,
        initialBalance: Amount,
        colorHex: String,
        iconName: String,
        isDefault: Boolean
    ): AccountEntity {
        require(name.isNotBlank()) { "Account name cannot be blank." }

        if (isDefault) {
            accountDao.clearDefault(profileId)
        }

        val account = AccountEntity(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            name = name.trim(),
            type = type,
            initialBalanceMinorUnits = initialBalance.minorUnits,
            colorHex = colorHex,
            iconName = iconName,
            isDefault = isDefault,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        accountDao.insertAccount(account)
        return account
    }

    override suspend fun updateAccount(account: AccountEntity) {
        if (account.isDefault) {
            accountDao.clearDefault(account.profileId)
        }
        accountDao.updateAccount(account.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteAccount(account: AccountEntity) {
        accountDao.deleteAccount(account)
    }

    override suspend fun setDefaultAccount(id: String, profileId: String) {
        accountDao.clearDefault(profileId)
        accountDao.setDefault(id, profileId)
    }
}
