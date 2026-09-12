package com.finora.android.ui.screens.onboarding

import com.finora.android.data.local.entity.ProfileEntity
import com.finora.android.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeProfileRepository = FakeProfileRepository(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has defaults and onboarding is incomplete when no profile exists`() = runTest {
        viewModel = OnboardingViewModel(fakeProfileRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Arjun Sharma", state.name)
        assertEquals("AS", state.initials)
        assertEquals("INR", state.selectedCurrencyCode)
        assertEquals("SYSTEM", state.selectedThemeMode)
        assertTrue(state.preloadCategories)
        assertFalse(state.isOnboardingComplete)
    }

    @Test
    fun `when profile already exists, onboarding marks complete automatically`() = runTest {
        fakeProfileRepository.setProfile(ProfileEntity("p1", "Existing User", "USD"))
        viewModel = OnboardingViewModel(fakeProfileRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isOnboardingComplete)
    }

    @Test
    fun `updating display name recalculates initials`() = runTest {
        viewModel = OnboardingViewModel(fakeProfileRepository)
        advanceUntilIdle()

        viewModel.updateName("Kavita Rao")
        assertEquals("Kavita Rao", viewModel.uiState.value.name)
        assertEquals("KR", viewModel.uiState.value.initials)
    }

    @Test
    fun `complete onboarding creates profile and marks complete`() = runTest {
        viewModel = OnboardingViewModel(fakeProfileRepository)
        advanceUntilIdle()

        viewModel.updateName("Priya")
        viewModel.updateCurrency("EUR")
        viewModel.updateTheme("DARK")

        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isOnboardingComplete)
        assertFalse(viewModel.uiState.value.isSubmitting)
        val created = fakeProfileRepository.createdProfile
        assertNotNull(created)
        assertEquals("Priya", created?.name)
        assertEquals("EUR", created?.currencyCode)
        assertEquals("DARK", created?.themeMode)
    }

    @Test
    fun `complete onboarding with blank name fails with error`() = runTest {
        viewModel = OnboardingViewModel(fakeProfileRepository)
        advanceUntilIdle()

        viewModel.updateName("   ")
        viewModel.completeOnboarding()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isOnboardingComplete)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    private class FakeProfileRepository(
        private var profile: ProfileEntity?
    ) : ProfileRepository {
        private val flow = MutableStateFlow(profile)
        var createdProfile: ProfileEntity? = null

        fun setProfile(p: ProfileEntity) {
            profile = p
            flow.value = p
        }

        override fun getActiveProfileFlow(): Flow<ProfileEntity?> = flow
        override suspend fun getActiveProfile(): ProfileEntity? = profile

        override suspend fun createProfile(name: String, currencyCode: String, themeMode: String): ProfileEntity {
            val p = ProfileEntity("p1", name, currencyCode, themeMode = themeMode)
            profile = p
            createdProfile = p
            flow.value = p
            return p
        }

        override suspend fun updateThemeMode(themeMode: String) {}
        override suspend fun updateCurrencyCode(currencyCode: String) {}
    }
}
