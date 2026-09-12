package com.finora.android.ui.screens.expenses

import com.finora.android.core.model.Amount
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.ExpenseEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
import com.finora.android.data.local.entity.ProfileEntity
import com.finora.android.data.local.relation.ExpenseWithDetails
import com.finora.android.data.repository.CategoryRepository
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.PaymentMethodRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.domain.model.DatePreset
import com.finora.android.domain.model.ExpenseFilterState
import com.finora.android.domain.model.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakePaymentMethodRepository: FakePaymentMethodRepository
    private lateinit var fakeExpenseRepository: FakeExpenseRepository

    private lateinit var viewModel: ExpensesViewModel

    private val testProfile = ProfileEntity("p1", "Alice", "INR")
    private val catFood = CategoryEntity("cat-food", "p1", "Food & Dining", "restaurant", "#046B5E", isDefault = true)
    private val catTransport = CategoryEntity("cat-transit", "p1", "Transportation", "directions_car", "#D97706", isDefault = true)
    private val pmUpi = PaymentMethodEntity("pm-upi", "p1", "UPI", "UPI", isDefault = true)
    private val pmCash = PaymentMethodEntity("pm-cash", "p1", "Cash", "CASH", isDefault = false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeProfileRepository = FakeProfileRepository(testProfile)
        fakeCategoryRepository = FakeCategoryRepository(listOf(catFood, catTransport))
        fakePaymentMethodRepository = FakePaymentMethodRepository(listOf(pmUpi, pmCash))
        fakeExpenseRepository = FakeExpenseRepository()

        // Seed 3 expenses
        val now = System.currentTimeMillis()
        val e1 = ExpenseWithDetails(
            expense = ExpenseEntity(
                id = "e1",
                profileId = "p1",
                amountMinorUnits = 25000L, // 250.00
                currencyCode = "INR",
                categoryId = catFood.id,
                paymentMethodId = pmUpi.id,
                expenseDate = now,
                title = "Dinner with team",
                notes = "Great meal",
                attachmentUri = null,
                source = "MANUAL",
                createdAt = now
            ),
            category = catFood,
            paymentMethod = pmUpi
        )
        val e2 = ExpenseWithDetails(
            expense = ExpenseEntity(
                id = "e2",
                profileId = "p1",
                amountMinorUnits = 5000L, // 50.00
                currencyCode = "INR",
                categoryId = catTransport.id,
                paymentMethodId = pmCash.id,
                expenseDate = now - 1000L,
                title = "Metro ticket",
                notes = null,
                attachmentUri = null,
                source = "MANUAL",
                createdAt = now - 1000L
            ),
            category = catTransport,
            paymentMethod = pmCash
        )
        val e3 = ExpenseWithDetails(
            expense = ExpenseEntity(
                id = "e3",
                profileId = "p1",
                amountMinorUnits = 120000L, // 1200.00
                currencyCode = "INR",
                categoryId = catFood.id,
                paymentMethodId = pmUpi.id,
                expenseDate = now - 2000L,
                title = "Grocery shopping",
                notes = "Weekly supply",
                attachmentUri = null,
                source = "MANUAL",
                createdAt = now - 2000L
            ),
            category = catFood,
            paymentMethod = pmUpi
        )

        fakeExpenseRepository.setExpenses(listOf(e1, e2, e3))

        viewModel = ExpensesViewModel(
            profileRepository = fakeProfileRepository,
            categoryRepository = fakeCategoryRepository,
            paymentMethodRepository = fakePaymentMethodRepository,
            expenseRepository = fakeExpenseRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoad_displaysAllExpensesAndCalculatesTotalSpend() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("p1", state.profileId)
        assertEquals("₹", state.currencySymbol)
        assertEquals(3, state.totalCount)
        assertEquals(150000L, state.totalSpend.minorUnits) // 250 + 50 + 1200 = 1500.00 -> 150000
        assertFalse(state.isLoading)
        assertTrue(state.groupedExpenses.isNotEmpty())
    }

    @Test
    fun testSearchQueryFilter_filtersMatchingExpenses() = runTest {
        advanceUntilIdle()

        // Search for "metro"
        viewModel.onSearchQueryChanged("metro")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.totalCount)
        assertEquals("e2", state.groupedExpenses.first().expenses.first().expense.id)
        assertEquals(5000L, state.totalSpend.minorUnits)

        // Clear search
        viewModel.onSearchQueryChanged("")
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.totalCount)
    }

    @Test
    fun testCategoryFilter_filtersByCategory() = runTest {
        advanceUntilIdle()

        viewModel.onApplyFilters(
            ExpenseFilterState(selectedCategoryIds = setOf(catTransport.id))
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.totalCount)
        assertEquals(catTransport.id, state.groupedExpenses.first().expenses.first().category.id)

        // Remove category filter
        viewModel.onRemoveCategoryFilter(catTransport.id)
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.totalCount)
    }

    @Test
    fun testSorting_byHighestAmount() = runTest {
        advanceUntilIdle()

        viewModel.onApplyFilters(
            ExpenseFilterState(sortOrder = SortOrder.HIGHEST_AMOUNT)
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val allDisplayed = state.groupedExpenses.flatMap { it.expenses }
        assertEquals("e3", allDisplayed.first().expense.id) // 1200 is highest
        assertEquals(120000L, allDisplayed.first().expense.amountMinorUnits)
        assertEquals("e2", allDisplayed.last().expense.id) // 50 is lowest
    }

    @Test
    fun testSorting_byLowestAmount() = runTest {
        advanceUntilIdle()

        viewModel.onApplyFilters(
            ExpenseFilterState(sortOrder = SortOrder.LOWEST_AMOUNT)
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val allDisplayed = state.groupedExpenses.flatMap { it.expenses }
        assertEquals("e2", allDisplayed.first().expense.id) // 50 is lowest
        assertEquals("e3", allDisplayed.last().expense.id) // 1200 is highest
    }

    @Test
    fun testClearAllFilters() = runTest {
        advanceUntilIdle()

        viewModel.onApplyFilters(
            ExpenseFilterState(
                searchQuery = "team",
                datePreset = DatePreset.THIS_MONTH,
                selectedCategoryIds = setOf(catFood.id)
            )
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.filterState.activeFilterCount > 0)

        viewModel.onClearAllFilters()
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.filterState.activeFilterCount)
        assertEquals(3, viewModel.uiState.value.totalCount)
    }

    // Fakes
    private class FakeProfileRepository(val profile: ProfileEntity) : ProfileRepository {
        override fun getActiveProfileFlow(): Flow<ProfileEntity?> = flowOf(profile)
        override suspend fun getActiveProfile(): ProfileEntity? = profile
        override suspend fun createProfile(name: String, currencyCode: String, themeMode: String): ProfileEntity = profile
        override suspend fun updateThemeMode(themeMode: String) {}
        override suspend fun updateCurrencyCode(currencyCode: String) {}
    }

    private class FakeCategoryRepository(val categories: List<CategoryEntity>) : CategoryRepository {
        override fun getCategoriesFlow(profileId: String): Flow<List<CategoryEntity>> = flowOf(categories)
        override suspend fun getCategories(profileId: String): List<CategoryEntity> = categories
        override suspend fun getCategoryById(id: String, profileId: String): CategoryEntity? = categories.find { it.id == id }
        override suspend fun countExpensesForCategory(categoryId: String, profileId: String): Int = 0
        override suspend fun createCategory(profileId: String, name: String, iconName: String, colorHex: String): CategoryEntity = categories.first()
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategory(category: CategoryEntity, reassignToCategoryId: String?) {}
    }

    private class FakePaymentMethodRepository(val methods: List<PaymentMethodEntity>) : PaymentMethodRepository {
        override fun getPaymentMethodsFlow(profileId: String): Flow<List<PaymentMethodEntity>> = flowOf(methods)
        override suspend fun getPaymentMethods(profileId: String): List<PaymentMethodEntity> = methods
        override suspend fun getPaymentMethodById(id: String, profileId: String): PaymentMethodEntity? = methods.first()
        override suspend fun createPaymentMethod(profileId: String, name: String, type: String, isDefault: Boolean): PaymentMethodEntity = methods.first()
        override suspend fun updatePaymentMethod(paymentMethod: PaymentMethodEntity) {}
        override suspend fun deletePaymentMethod(paymentMethod: PaymentMethodEntity) {}
    }

    private class FakeExpenseRepository : ExpenseRepository {
        private val _expensesFlow = MutableStateFlow<List<ExpenseWithDetails>>(emptyList())

        fun setExpenses(expenses: List<ExpenseWithDetails>) {
            _expensesFlow.value = expenses
        }

        override fun getAllExpensesFlow(profileId: String): Flow<List<ExpenseWithDetails>> = _expensesFlow
        override fun getExpenseByIdFlow(id: String, profileId: String): Flow<ExpenseWithDetails?> =
            flowOf(_expensesFlow.value.find { it.expense.id == id })

        override suspend fun getExpenseById(id: String, profileId: String): ExpenseWithDetails? =
            _expensesFlow.value.find { it.expense.id == id }

        override fun getExpensesByDateRangeFlow(profileId: String, startDate: Long, endDate: Long): Flow<List<ExpenseWithDetails>> = flowOf(emptyList())
        override fun getRecentExpensesFlow(profileId: String, limit: Int): Flow<List<ExpenseWithDetails>> = flowOf(emptyList())
        override fun getTotalSpendInDateRangeFlow(profileId: String, startDate: Long, endDate: Long): Flow<Long> = flowOf(0L)
        override suspend fun getTotalSpendInDateRange(profileId: String, startDate: Long, endDate: Long): Long = 0L
        override suspend fun getCategorySpendInDateRange(profileId: String, categoryId: String, startDate: Long, endDate: Long): Long = 0L
        override fun getCategorySpendBreakdownFlow(profileId: String, startDate: Long, endDate: Long): Flow<List<com.finora.android.data.local.dao.CategorySpend>> = flowOf(emptyList())
        override suspend fun getHighestExpenseInDateRange(profileId: String, startDate: Long, endDate: Long): ExpenseWithDetails? = null

        override suspend fun createExpense(
            profileId: String,
            amount: Amount,
            currencyCode: String,
            categoryId: String,
            expenseDate: Long,
            paymentMethodId: String?,
            title: String?,
            notes: String?,
            attachmentUri: String?,
            source: String
        ): ExpenseEntity = throw UnsupportedOperationException()

        override suspend fun updateExpense(expense: ExpenseEntity) {}
        override suspend fun deleteExpense(expense: ExpenseEntity) {}
        override suspend fun deleteExpenseById(id: String, profileId: String) {}
    }
}
