package com.finora.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finora.android.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods WHERE profileId = :profileId ORDER BY isDefault DESC, name ASC")
    fun getPaymentMethodsFlow(profileId: String): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods WHERE profileId = :profileId ORDER BY isDefault DESC, name ASC")
    suspend fun getPaymentMethods(profileId: String): List<PaymentMethodEntity>

    @Query("SELECT * FROM payment_methods WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun getPaymentMethodById(id: String, profileId: String): PaymentMethodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethod(paymentMethod: PaymentMethodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethods(paymentMethods: List<PaymentMethodEntity>)

    @Update
    suspend fun updatePaymentMethod(paymentMethod: PaymentMethodEntity)

    @Delete
    suspend fun deletePaymentMethod(paymentMethod: PaymentMethodEntity)
}
