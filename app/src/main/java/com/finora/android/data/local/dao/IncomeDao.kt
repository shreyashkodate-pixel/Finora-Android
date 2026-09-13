package com.finora.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finora.android.data.local.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {

    @Query("SELECT * FROM income WHERE profileId = :profileId ORDER BY incomeDate DESC")
    fun getIncomeForProfileFlow(profileId: String): Flow<List<IncomeEntity>>

    @Query("""
        SELECT * FROM income 
        WHERE profileId = :profileId AND incomeDate >= :startDate AND incomeDate <= :endDate 
        ORDER BY incomeDate DESC
    """)
    fun getIncomeBetweenDatesFlow(profileId: String, startDate: Long, endDate: Long): Flow<List<IncomeEntity>>

    @Query("""
        SELECT * FROM income 
        WHERE profileId = :profileId AND incomeDate >= :startDate AND incomeDate <= :endDate 
        ORDER BY incomeDate DESC
    """)
    suspend fun getIncomeBetweenDates(profileId: String, startDate: Long, endDate: Long): List<IncomeEntity>

    @Query("""
        SELECT COALESCE(SUM(amountMinorUnits), 0) FROM income 
        WHERE profileId = :profileId AND incomeDate >= :startDate AND incomeDate <= :endDate
    """)
    fun getTotalIncomeBetweenDatesFlow(profileId: String, startDate: Long, endDate: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amountMinorUnits), 0) FROM income 
        WHERE profileId = :profileId AND incomeDate >= :startDate AND incomeDate <= :endDate
    """)
    suspend fun getTotalIncomeBetweenDates(profileId: String, startDate: Long, endDate: Long): Long

    @Query("SELECT * FROM income WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun getIncomeById(id: String, profileId: String): IncomeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity)

    @Update
    suspend fun updateIncome(income: IncomeEntity)

    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    @Query("DELETE FROM income WHERE id = :id AND profileId = :profileId")
    suspend fun deleteIncomeById(id: String, profileId: String)

    @Query("SELECT * FROM income")
    suspend fun getAllIncome(): List<IncomeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeList(incomeList: List<IncomeEntity>)

    @Query("DELETE FROM income")
    suspend fun deleteAllIncome()
}
