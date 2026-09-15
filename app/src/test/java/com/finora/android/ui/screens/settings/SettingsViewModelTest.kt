package com.finora.android.ui.screens.settings

import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
import com.finora.android.data.local.entity.ProfileEntity
import com.finora.android.data.repository.CategoryRepository
import com.finora.android.data.repository.PaymentMethodRepository
import com.finora.android.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakePaymentMethodRepository: FakePaymentMethodRepository
    private lateinit var viewModel: SettingsViewModel

    private val testProfile = ProfileEntity("p1", "Arjun Sharma", "INR", themeMode = "SYSTEM")
    private val catFood = CategoryEntity("cat-food", "p1", "Food & Dining", "restaurant", "#046B5E", isDefault = true)
    private val catCustom = CategoryEntity("cat-custom", "p1", "Gaming", "movie", "#0F6F62", isDefault = false)
    private val methodCash = PaymentMethodEntity("pm-cash", "p1", "Cash", "CASH", isDefault = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeProfileRepository = FakeProfileRepository(testProfile)
        fakeCategoryRepository = FakeCategoryRepository(listOf(catFood, catCustom))
        fakePaymentMethodRepository = FakePaymentMethodRepository(listOf(methodCash))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads settings and computes initials correctly`() = runTest {
        viewModel = SettingsViewModel(fakeProfileRepository, fakeCategoryRepository, fakePaymentMethodRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Arjun Sharma", state.profileName)
        assertEquals("AS", state.profileInitials)
        assertEquals("INR", state.currencyCode)
        assertEquals("₹", state.currencySymbol)
        assertEquals("SYSTEM", state.themeMode)
        assertEquals(2, state.totalCategoriesCount)
        assertEquals(1, state.customCategoriesCount)
        assertEquals(1, state.totalPaymentMethodsCount)
    }

    @Test
    fun `update base currency updates repository and state`() = runTest {
        viewModel = SettingsViewModel(fakeProfileRepository, fakeCategoryRepository, fakePaymentMethodRepository)
        advanceUntilIdle()

        viewModel.selectCurrency("USD")
        advanceUntilIdle()

        assertEquals("USD", fakeProfileRepository.lastCurrencyCode)
    }

    @Test
    fun `update theme mode updates repository and state`() = runTest {
        viewModel = SettingsViewModel(fakeProfileRepository, fakeCategoryRepository, fakePaymentMethodRepository)
        advanceUntilIdle()

        viewModel.setThemeMode("DARK")
        advanceUntilIdle()

        assertEquals("DARK", fakeProfileRepository.lastThemeMode)
    }

    @Test
    fun `update profile name updates repository and state`() = runTest {
        viewModel = SettingsViewModel(fakeProfileRepository, fakeCategoryRepository, fakePaymentMethodRepository)
        advanceUntilIdle()

        viewModel.saveProfileName("Vikram Seth")
        advanceUntilIdle()

        assertEquals("Vikram Seth", fakeProfileRepository.lastName)
    }

    private class FakeProfileRepository(
        private var profile: ProfileEntity
    ) : ProfileRepository {
        private val flow = MutableStateFlow<ProfileEntity?>(profile)
        var lastCurrencyCode: String? = null
        var lastThemeMode: String? = null
        var lastName: String? = null

        override fun getActiveProfileFlow(): Flow<ProfileEntity?> = flow
        override suspend fun getActiveProfile(): ProfileEntity? = profile
        override suspend fun createProfile(name: String, currencyCode: String, themeMode: String): ProfileEntity {
            val p = ProfileEntity("p1", name, currencyCode, themeMode = themeMode)
            profile = p
            flow.value = p
            return p
        }

        override suspend fun updateThemeMode(themeMode: String) {
            lastThemeMode = themeMode
            val updated = profile.copy(themeMode = themeMode)
            profile = updated
            flow.value = updated
        }

        override suspend fun updateCurrencyCode(currencyCode: String) {
            lastCurrencyCode = currencyCode
            val updated = profile.copy(currencyCode = currencyCode)
            profile = updated
            flow.value = updated
        }

        override suspend fun updateProfileName(name: String) {
            lastName = name
            val updated = profile.copy(name = name)
            profile = updated
            flow.value = updated
        }

        override fun getAllProfilesFlow(): Flow<List<ProfileEntity>> = flow.map { listOfNotNull(it) }
        override suspend fun getAllProfiles(): List<ProfileEntity> = listOf(profile)
        override suspend fun switchActiveProfile(profileId: String) {}
        override suspend fun deleteProfile(profileId: String) {}
    }

    private class FakeCategoryRepository(
        private val categories: List<CategoryEntity>
    ) : CategoryRepository {
        override fun getCategoriesFlow(profileId: String): Flow<List<CategoryEntity>> = flowOf(categories)
        override suspend fun getCategories(profileId: String): List<CategoryEntity> = categories
        override suspend fun getCategoryById(id: String, profileId: String): CategoryEntity? = null
        override suspend fun countExpensesForCategory(categoryId: String, profileId: String): Int = 0
        override suspend fun createCategory(profileId: String, name: String, iconName: String, colorHex: String): CategoryEntity = throw NotImplementedError()
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategory(category: CategoryEntity, reassignToCategoryId: String?) {}
    }

    private class FakePaymentMethodRepository(
        private val methods: List<PaymentMethodEntity>
    ) : PaymentMethodRepository {
        override fun getPaymentMethodsFlow(profileId: String): Flow<List<PaymentMethodEntity>> = flowOf(methods)
        override suspend fun getPaymentMethods(profileId: String): List<PaymentMethodEntity> = methods
        override suspend fun getPaymentMethodById(id: String, profileId: String): PaymentMethodEntity? = null
        override suspend fun createPaymentMethod(profileId: String, name: String, type: String, isDefault: Boolean): PaymentMethodEntity = throw NotImplementedError()
        override suspend fun updatePaymentMethod(paymentMethod: PaymentMethodEntity) {}
        override suspend fun deletePaymentMethod(paymentMethod: PaymentMethodEntity) {}
    }
}
