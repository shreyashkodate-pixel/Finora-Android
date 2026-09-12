package com.finora.android.data.repository

import com.finora.android.data.local.dao.PaymentMethodDao
import com.finora.android.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface PaymentMethodRepository {
    fun getPaymentMethodsFlow(profileId: String): Flow<List<PaymentMethodEntity>>
    suspend fun getPaymentMethods(profileId: String): List<PaymentMethodEntity>
    suspend fun getPaymentMethodById(id: String, profileId: String): PaymentMethodEntity?
    suspend fun createPaymentMethod(
        profileId: String,
        name: String,
        type: String,
        isDefault: Boolean = false
    ): PaymentMethodEntity
    suspend fun updatePaymentMethod(paymentMethod: PaymentMethodEntity)
    suspend fun deletePaymentMethod(paymentMethod: PaymentMethodEntity)
}

class PaymentMethodRepositoryImpl(
    private val paymentMethodDao: PaymentMethodDao
) : PaymentMethodRepository {

    override fun getPaymentMethodsFlow(profileId: String): Flow<List<PaymentMethodEntity>> =
        paymentMethodDao.getPaymentMethodsFlow(profileId)

    override suspend fun getPaymentMethods(profileId: String): List<PaymentMethodEntity> =
        paymentMethodDao.getPaymentMethods(profileId)

    override suspend fun getPaymentMethodById(id: String, profileId: String): PaymentMethodEntity? =
        paymentMethodDao.getPaymentMethodById(id, profileId)

    override suspend fun createPaymentMethod(
        profileId: String,
        name: String,
        type: String,
        isDefault: Boolean
    ): PaymentMethodEntity {
        val now = System.currentTimeMillis()
        val paymentMethod = PaymentMethodEntity(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            name = name.trim(),
            type = type,
            isDefault = isDefault,
            createdAt = now,
            updatedAt = now
        )
        paymentMethodDao.insertPaymentMethod(paymentMethod)
        return paymentMethod
    }

    override suspend fun updatePaymentMethod(paymentMethod: PaymentMethodEntity) {
        paymentMethodDao.updatePaymentMethod(paymentMethod.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deletePaymentMethod(paymentMethod: PaymentMethodEntity) {
        paymentMethodDao.deletePaymentMethod(paymentMethod)
    }
}
