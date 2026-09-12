package com.finora.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finora.android.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles LIMIT 1")
    fun getActiveProfileFlow(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles LIMIT 1")
    suspend fun getActiveProfile(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("UPDATE profiles SET themeMode = :themeMode WHERE id = :id")
    suspend fun updateThemeMode(id: String, themeMode: String)

    @Query("UPDATE profiles SET currencyCode = :currencyCode WHERE id = :id")
    suspend fun updateCurrencyCode(id: String, currencyCode: String)
}
