package com.finora.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finora.android.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts WHERE profileId = :profileId AND isArchived = 0 ORDER BY isDefault DESC, name ASC")
    fun getActiveAccountsFlow(profileId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE profileId = :profileId ORDER BY isArchived ASC, isDefault DESC, name ASC")
    fun getAllAccountsFlow(profileId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE profileId = :profileId AND isArchived = 0 ORDER BY isDefault DESC, name ASC")
    suspend fun getActiveAccounts(profileId: String): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun getAccountById(id: String, profileId: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE profileId = :profileId AND isDefault = 1 LIMIT 1")
    suspend fun getDefaultAccount(profileId: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id AND profileId = :profileId")
    suspend fun deleteAccountById(id: String, profileId: String)

    @Query("UPDATE accounts SET isDefault = 0 WHERE profileId = :profileId")
    suspend fun clearDefault(profileId: String)

    @Query("UPDATE accounts SET isDefault = 1 WHERE id = :id AND profileId = :profileId")
    suspend fun setDefault(id: String, profileId: String)

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccounts(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
}
