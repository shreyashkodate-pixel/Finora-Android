package com.finora.android.ui.screens.budgets

import com.finora.android.core.model.Amount
import com.finora.android.data.local.entity.BudgetEntity
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.ExpenseEntity
import com.finora.android.data.local.entity.ProfileEntity
import com.finora.android.data.local.relation.ExpenseWithDetails
import com.finora.android.data.repository.BudgetRepository
import com.finora.android.data.repository.CategoryRepository
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.domain.model.BudgetStatus
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeBudgetRepository: FakeBudgetRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakeExpenseRepository: FakeExpenseRepository
    private lateinit var viewModel: BudgetsViewModel

    private val testProfile = ProfileEntity("p1", "Arjun", "INR")
    private val catFood = CategoryEntity("cat-food", "p1", "Food & Dining", "restaurant", "#046B5E", isDefault = true)
    private val catRent = CategoryEntity("cat-rent", "p1", "Housing & Rent", "home", "#2563EB", isDefault = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeProfileRepository = FakeProfileRepository(testProfile)
        fakeBudgetRepository = FakeBudgetRepository()
        fakeCategoryRepository = FakeCategoryRepository(listOf(catFood, catRent))
        fakeExpenseRepository = FakeExpenseRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoad_whenNoBudgetsSet() = runTest {
        viewModel = BudgetsViewModel(
            profileRepository = fakeProfileRepository,
            budgetRepository = fakeBudgetRepository,
            categoryRepository = fakeCategoryRepository,
            expenseRepository = fakeExpenseRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.overallBudget)
        assertNull(state.overallSummary)
        assertTrue(state.categoryBudgets.isEmpty())
        assertEquals(2, state.availableCategoriesForBudget.size)
    }

    @Test
    fun testInitialLoad_withOverallAndCategoryBudgets() = runTest {
        val now = System.currentTimeMillis()
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(now))

        // Set overall budget: 50,000 (5,000,000 paise)
        val overallBudget = BudgetEntity("b-overall", "p1", currentYearMonth, null, 5_000_000L)
        // Set category budget: Food 15,000 (1,500,000 paise)
        val foodBudget = BudgetEntity("b-food", "p1", currentYearMonth, catFood.id, 1_500_000L)

        fakeBudgetRepository.setBudgets(listOf(overallBudget, foodBudget))

        // Food expense: 14,200 (1,420,000 paise) -> 94.6% (Near Limit)
        val expFood = ExpenseWithDetails(
            expense = ExpenseEntity(
                id = "e1",
                profileId = "p1",
                amountMinorUnits = 1_420_000L,
                currencyCode = "INR",
                categoryId = catFood.id,
                paymentMethodId = null,
                expenseDate = now,
                title = "Dinner",
                notes = null,
                attachmentUri = null,
                source = "MANUAL",
                createdAt = now
            ),
            category = catFood,
            paymentMethod = null
        )

        fakeExpenseRepository.setExpenses(listOf(expFood))

        viewModel = BudgetsViewModel(
            profileRepository = fakeProfileRepository,
            budgetRepository = fakeBudgetRepository,
            categoryRepository = fakeCategoryRepository,
            expenseRepository = fakeExpenseRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.overallBudget)
        assertNotNull(state.overallSummary)

        val overallSummary = state.overallSummary!!
        assertEquals(5_000_000L, overallSummary.budgetAmount.minorUnits)
        assertEquals(1_420_000L, overallSummary.spentAmount.minorUnits)
        assertEquals(3_580_000L, overallSummary.remainingAmount.minorUnits)
        assertEquals(BudgetStatus.ON_TRACK, overallSummary.status)

        // Safe burn rate per day is > 0
        assertTrue(state.safeBurnRatePerDay.minorUnits > 0L)

        // Category budget check
        assertEquals(1, state.categoryBudgets.size)
        val catItem = state.categoryBudgets.first()
        assertEquals(catFood.name, catItem.categoryName)
        assertEquals(1_500_000L, catItem.budgetAmount.minorUnits)
        assertEquals(1_420_000L, catItem.spentAmount.minorUnits)
        assertEquals(80_000L, catItem.remainingAmount.minorUnits)
        assertEquals(BudgetStatus.NEAR_LIMIT, catItem.status) // 94.6% >= 80%

        // Available category for new budget: catRent
        assertEquals(1, state.availableCategoriesForBudget.size)
        assertEquals(catRent.id, state.availableCategoriesForBudget.first().id)
    }

    @Test
    fun testSetOverallBudget() = runTest {
        viewModel = BudgetsViewModel(
            profileRepository = fakeProfileRepository,
            budgetRepository = fakeBudgetRepository,
            categoryRepository = fakeCategoryRepository,
            expenseRepository = fakeExpenseRepository
        )
        advanceUntilIdle()

        viewModel.setOverallBudget(Amount(6_000_000L)) // 60,000.00
        advanceUntilIdle()

        val created = fakeBudgetRepository.getOverallBudget("p1", "")
        assertNotNull(created)
        assertEquals(6_000_000L, created?.amountMinorUnits)
        assertNull(created?.categoryId)
    }

    @Test
    fun testSetCategoryBudget() = runTest {
        viewModel = BudgetsViewModel(
            profileRepository = fakeProfileRepository,
            budgetRepository = fakeBudgetRepository,
            categoryRepository = fakeCategoryRepository,
            expenseRepository = fakeExpenseRepository
        )
        advanceUntilIdle()

        viewModel.setCategoryBudget(catRent.id, Amount(2_500_000L)) // 25,000.00
        advanceUntilIdle()

        val created = fakeBudgetRepository.getCategoryBudget("p1", "", catRent.id)
        assertNotNull(created)
        assertEquals(2_500_000L, created?.amountMinorUnits)
        assertEquals(catRent.id, created?.categoryId)
    }

    // Fakes
    private class FakeProfileRepository(val profile: ProfileEntity) : ProfileRepository {
        override fun getActiveProfileFlow(): Flow<ProfileEntity?> = flowOf(profile)
        override suspend fun getActiveProfile(): ProfileEntity? = profile
        override fun getAllProfilesFlow(): Flow<List<ProfileEntity>> = flowOf(listOf(profile))
        override suspend fun getAllProfiles(): List<ProfileEntity> = listOf(profile)
        override suspend fun createProfile(name: String, currencyCode: String, themeMode: String): ProfileEntity = profile
        override suspend fun switchActiveProfile(profileId: String) {}
        override suspend fun deleteProfile(profileId: String) {}
        override suspend fun updateThemeMode(themeMode: String) {}
        override suspend fun updateCurrencyCode(currencyCode: String) {}
        override suspend fun updateProfileName(name: String) {}
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

    private class FakeBudgetRepository : BudgetRepository {
        private val _budgetsFlow = MutableStateFlow<List<BudgetEntity>>(emptyList())

        fun setBudgets(budgets: List<BudgetEntity>) {
            _budgetsFlow.value = budgets
        }

        override fun getBudgetsForMonthFlow(profileId: String, yearMonth: String): Flow<List<BudgetEntity>> = _budgetsFlow
        override fun getOverallBudgetFlow(profileId: String, yearMonth: String): Flow<BudgetEntity?> =
            flowOf(_budgetsFlow.value.find { it.categoryId == null })

        override suspend fun getOverallBudget(profileId: String, yearMonth: String): BudgetEntity? =
            _budgetsFlow.value.find { it.categoryId == null }

        override suspend fun getCategoryBudget(profileId: String, yearMonth: String, categoryId: String): BudgetEntity? =
            _budgetsFlow.value.find { it.categoryId == categoryId }

        override suspend fun setOverallBudget(profileId: String, yearMonth: String, amount: Amount): BudgetEntity {
            val entity = BudgetEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                yearMonth = yearMonth,
                categoryId = null,
                amountMinorUnits = amount.minorUnits
            )
            val updated = _budgetsFlow.value.filter { it.categoryId != null } + entity
            _budgetsFlow.value = updated
            return entity
        }

        override suspend fun setCategoryBudget(profileId: String, yearMonth: String, categoryId: String, amount: Amount): BudgetEntity {
            val entity = BudgetEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                yearMonth = yearMonth,
                categoryId = categoryId,
                amountMinorUnits = amount.minorUnits
            )
            val updated = _budgetsFlow.value.filter { it.categoryId != categoryId } + entity
            _budgetsFlow.value = updated
            return entity
        }

        override suspend fun deleteBudget(id: String, profileId: String) {
            _budgetsFlow.value = _budgetsFlow.value.filter { it.id != id }
        }
    }

    private class FakeExpenseRepository : ExpenseRepository {
        private val _expensesFlow = MutableStateFlow<List<ExpenseWithDetails>>(emptyList())

        fun setExpenses(expenses: List<ExpenseWithDetails>) {
            _expensesFlow.value = expenses
        }

        override fun getAllExpensesFlow(profileId: String): Flow<List<ExpenseWithDetails>> = _expensesFlow
        override fun getExpenseByIdFlow(id: String, profileId: String): Flow<ExpenseWithDetails?> = flowOf(null)
        override suspend fun getExpenseById(id: String, profileId: String): ExpenseWithDetails? = null

        override fun getExpensesByDateRangeFlow(profileId: String, startDate: Long, endDate: Long): Flow<List<ExpenseWithDetails>> =
            flowOf(_expensesFlow.value.filter { it.expense.expenseDate in startDate..endDate })

        override fun getRecentExpensesFlow(profileId: String, limit: Int): Flow<List<ExpenseWithDetails>> = flowOf(emptyList())
        override fun getTotalSpendInDateRangeFlow(profileId: String, startDate: Long, endDate: Long): Flow<Long> =
            flowOf(_expensesFlow.value.filter { it.expense.expenseDate in startDate..endDate }.sumOf { it.expense.amountMinorUnits })

        override suspend fun getTotalSpendInDateRange(profileId: String, startDate: Long, endDate: Long): Long =
            _expensesFlow.value.filter { it.expense.expenseDate in startDate..endDate }.sumOf { it.expense.amountMinorUnits }

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
