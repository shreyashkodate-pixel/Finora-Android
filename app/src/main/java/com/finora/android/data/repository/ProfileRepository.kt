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
    fun getAllProfilesFlow(): Flow<List<ProfileEntity>>
    suspend fun getAllProfiles(): List<ProfileEntity>
    suspend fun createProfile(name: String, currencyCode: String, themeMode: String = "SYSTEM"): ProfileEntity
    suspend fun switchActiveProfile(profileId: String)
    suspend fun deleteProfile(profileId: String)
    suspend fun updateThemeMode(themeMode: String)
    suspend fun updateCurrencyCode(currencyCode: String)
    suspend fun updateProfileName(name: String)
}

class ProfileRepositoryImpl(
    private val profileDao: ProfileDao,
    private val categoryDao: CategoryDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val prefs: android.content.SharedPreferences? = null
) : ProfileRepository {

    private val activeProfileIdFlow = kotlinx.coroutines.flow.MutableStateFlow(
        prefs?.getString("active_profile_id", null)
    )

    override fun getActiveProfileFlow(): Flow<ProfileEntity?> {
        return kotlinx.coroutines.flow.combine(
            profileDao.getAllProfilesFlow(),
            activeProfileIdFlow
        ) { profiles, activeId ->
            if (profiles.isEmpty()) return@combine null
            val matched = if (activeId != null) profiles.find { it.id == activeId } else null
            val active = matched ?: profiles.first()
            if (activeProfileIdFlow.value != active.id) {
                activeProfileIdFlow.value = active.id
                prefs?.edit()?.putString("active_profile_id", active.id)?.apply()
            }
            active
        }
    }

    override suspend fun getActiveProfile(): ProfileEntity? {
        val profiles = profileDao.getAllProfiles()
        if (profiles.isEmpty()) return null
        val activeId = activeProfileIdFlow.value ?: prefs?.getString("active_profile_id", null)
        val matched = if (activeId != null) profiles.find { it.id == activeId } else null
        val active = matched ?: profiles.first()
        if (activeProfileIdFlow.value != active.id) {
            activeProfileIdFlow.value = active.id
            prefs?.edit()?.putString("active_profile_id", active.id)?.apply()
        }
        return active
    }

    override fun getAllProfilesFlow(): Flow<List<ProfileEntity>> = profileDao.getAllProfilesFlow()

    override suspend fun getAllProfiles(): List<ProfileEntity> = profileDao.getAllProfiles()

    override suspend fun switchActiveProfile(profileId: String) {
        val target = profileDao.getProfileById(profileId) ?: return
        activeProfileIdFlow.value = target.id
        prefs?.edit()?.putString("active_profile_id", target.id)?.apply()
    }

    override suspend fun deleteProfile(profileId: String) {
        val profiles = profileDao.getAllProfiles()
        require(profiles.size > 1) { "Cannot delete the only profile." }
        profileDao.deleteProfileById(profileId)
        if (activeProfileIdFlow.value == profileId) {
            val remaining = profiles.filter { it.id != profileId }
            val next = remaining.firstOrNull()
            activeProfileIdFlow.value = next?.id
            if (next != null) {
                prefs?.edit()?.putString("active_profile_id", next.id)?.apply()
            }
        }
    }

    override suspend fun createProfile(
        name: String,
        currencyCode: String,
        themeMode: String
    ): ProfileEntity {
        val now = System.currentTimeMillis()
        val profileId = UUID.randomUUID().toString()
        val profile = ProfileEntity(
            id = profileId,
            name = name.trim(),
            currencyCode = currencyCode.uppercase().trim(),
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

        // Automatically activate new profile
        activeProfileIdFlow.value = profileId
        prefs?.edit()?.putString("active_profile_id", profileId)?.apply()

        return profile
    }

    override suspend fun updateThemeMode(themeMode: String) {
        val activeProfile = getActiveProfile() ?: return
        profileDao.updateThemeMode(activeProfile.id, themeMode)
    }

    override suspend fun updateCurrencyCode(currencyCode: String) {
        val activeProfile = getActiveProfile() ?: return
        profileDao.updateCurrencyCode(activeProfile.id, currencyCode)
    }

    override suspend fun updateProfileName(name: String) {
        val activeProfile = getActiveProfile() ?: return
        profileDao.updateProfileName(activeProfile.id, name)
    }
}
