package com.finora.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finora.android.data.local.entity.RecurringExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {

    @Query("SELECT * FROM recurring_expenses WHERE profileId = :profileId ORDER BY nextDueDate ASC")
    fun getAllRecurringFlow(profileId: String): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE profileId = :profileId AND isActive = 1 ORDER BY nextDueDate ASC")
    fun getActiveRecurringFlow(profileId: String): Flow<List<RecurringExpenseEntity>>

    @Query("""
        SELECT * FROM recurring_expenses 
        WHERE profileId = :profileId AND isActive = 1 AND nextDueDate <= :beforeTimestamp 
        ORDER BY nextDueDate ASC
    """)
    fun getDueRecurringFlow(profileId: String, beforeTimestamp: Long): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE profileId = :profileId AND isActive = 1")
    suspend fun getActiveRecurring(profileId: String): List<RecurringExpenseEntity>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun getRecurringById(id: String, profileId: String): RecurringExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(recurring: RecurringExpenseEntity)

    @Update
    suspend fun updateRecurring(recurring: RecurringExpenseEntity)

    @Delete
    suspend fun deleteRecurring(recurring: RecurringExpenseEntity)

    @Query("DELETE FROM recurring_expenses WHERE id = :id AND profileId = :profileId")
    suspend fun deleteRecurringById(id: String, profileId: String)

    @Query("SELECT * FROM recurring_expenses")
    suspend fun getAllRecurring(): List<RecurringExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringList(items: List<RecurringExpenseEntity>)

    @Query("DELETE FROM recurring_expenses")
    suspend fun deleteAllRecurring()
}
