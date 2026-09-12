package com.finora.android.data.repository

import com.finora.android.data.local.StarterData
import com.finora.android.data.local.dao.CategoryDao
import com.finora.android.data.local.dao.PaymentMethodDao
import com.finora.android.data.local.dao.ProfileDao
import com.finora.android.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface ProfileRepository {
    fun getActiveProfileFlow(): Flow<ProfileEntity?>
    suspend fun getActiveProfile(): ProfileEntity?
    suspend fun createProfile(name: String, currencyCode: String, themeMode: String = "SYSTEM"): ProfileEntity
    suspend fun updateThemeMode(themeMode: String)
    suspend fun updateCurrencyCode(currencyCode: String)
    suspend fun updateProfileName(name: String) {}
}

class ProfileRepositoryImpl(
    private val profileDao: ProfileDao,
    private val categoryDao: CategoryDao,
    private val paymentMethodDao: PaymentMethodDao
) : ProfileRepository {

    override fun getActiveProfileFlow(): Flow<ProfileEntity?> = profileDao.getActiveProfileFlow()

    override suspend fun getActiveProfile(): ProfileEntity? = profileDao.getActiveProfile()

    override suspend fun createProfile(
        name: String,
        currencyCode: String,
        themeMode: String
    ): ProfileEntity {
        val now = System.currentTimeMillis()
        val profileId = UUID.randomUUID().toString()
        val profile = ProfileEntity(
            id = profileId,
            name = name,
            currencyCode = currencyCode,
            themeMode = themeMode,
            createdAt = now,
            updatedAt = now
        )
        profileDao.insertProfile(profile)

        // Prepopulate starter categories & payment methods for this profile
        val starterCategories = StarterData.createStarterCategories(profileId)
        categoryDao.insertCategories(starterCategories)

        val starterPaymentMethods = StarterData.createStarterPaymentMethods(profileId)
        paymentMethodDao.insertPaymentMethods(starterPaymentMethods)

        return profile
    }

    override suspend fun updateThemeMode(themeMode: String) {
        val activeProfile = profileDao.getActiveProfile() ?: return
        profileDao.updateThemeMode(activeProfile.id, themeMode)
    }

    override suspend fun updateCurrencyCode(currencyCode: String) {
        val activeProfile = profileDao.getActiveProfile() ?: return
        profileDao.updateCurrencyCode(activeProfile.id, currencyCode)
    }

    override suspend fun updateProfileName(name: String) {
        val activeProfile = profileDao.getActiveProfile() ?: return
        profileDao.updateProfileName(activeProfile.id, name)
    }
}
