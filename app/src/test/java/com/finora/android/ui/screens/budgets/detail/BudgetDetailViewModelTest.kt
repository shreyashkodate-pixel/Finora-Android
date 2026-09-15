package com.finora.android.ui.screens.budgets.detail

import com.finora.android.core.model.Amount
import com.finora.android.data.local.entity.BudgetEntity
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.ExpenseEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeBudgetRepository: FakeBudgetRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakeExpenseRepository: FakeExpenseRepository
    private lateinit var viewModel: BudgetDetailViewModel

    private val testProfile = ProfileEntity("p1", "Arjun", "INR")
    private val catShopping = CategoryEntity("cat-shop", "p1", "Shopping", "shopping_bag", "#9333EA", isDefault = true)
    private val pmCard = PaymentMethodEntity("pm-card", "p1", "Credit Card", "CREDIT_CARD", isDefault = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeProfileRepository = FakeProfileRepository(testProfile)
        fakeCategoryRepository = FakeCategoryRepository(listOf(catShopping))
        fakeBudgetRepository = FakeBudgetRepository()
        fakeExpenseRepository = FakeExpenseRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadBudgetDetail_categoryBudget_exceededLimit() = runTest {
        val now = System.currentTimeMillis()
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(now))

        // Cap: 2,000.00 (200,000 paise)
        val shoppingBudget = BudgetEntity("b-shop", "p1", currentYearMonth, catShopping.id, 200_000L)
        fakeBudgetRepository.setBudgets(listOf(shoppingBudget))

        // Expense: 2,499.00 (249,900 paise) -> 124.9% (Over Budget by 499.00)
        val exp1 = ExpenseWithDetails(
            expense = ExpenseEntity(
                id = "exp-1",
                profileId = "p1",
                amountMinorUnits = 249_900L,
                currencyCode = "INR",
                categoryId = catShopping.id,
                paymentMethodId = pmCard.id,
                expenseDate = now,
                title = "Amazon India",
                notes = "External SSD",
                attachmentUri = null,
                source = "MANUAL",
                createdAt = now
            ),
            category = catShopping,
            paymentMethod = pmCard
        )

        fakeExpenseRepository.setExpenses(listOf(exp1))

        viewModel = BudgetDetailViewModel(
            budgetId = "b-shop",
            profileRepository = fakeProfileRepository,
            budgetRepository = fakeBudgetRepository,
            categoryRepository = fakeCategoryRepository,
            expenseRepository = fakeExpenseRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Shopping Budget", state.budgetTitle)
        assertTrue(state.isCategoryBudget)
        assertEquals(200_000L, state.budgetAmount.minorUnits)
        assertEquals(249_900L, state.spentAmount.minorUnits)
        assertEquals(49_900L, state.overBudgetAmount.minorUnits)
        assertEquals(BudgetStatus.OVER_BUDGET, state.status)

        // Contributing transactions
        assertEquals(1, state.contributingExpenses.size)
        assertEquals("exp-1", state.contributingExpenses.first().expense.id)

        // Average transaction spend = 249_900L
        assertEquals(249_900L, state.averageTransactionSpend.minorUnits)
    }

    @Test
    fun testUpdateBudgetCap() = runTest {
        val now = System.currentTimeMillis()
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(now))

        val shoppingBudget = BudgetEntity("b-shop", "p1", currentYearMonth, catShopping.id, 200_000L)
        fakeBudgetRepository.setBudgets(listOf(shoppingBudget))

        viewModel = BudgetDetailViewModel(
            budgetId = "b-shop",
            profileRepository = fakeProfileRepository,
            budgetRepository = fakeBudgetRepository,
            categoryRepository = fakeCategoryRepository,
            expenseRepository = fakeExpenseRepository
        )
        advanceUntilIdle()

        // Update cap to 3,500.00
        viewModel.updateBudgetCap(Amount(350_000L))
        advanceUntilIdle()

        val updated = fakeBudgetRepository.getCategoryBudget("p1", currentYearMonth, catShopping.id)
        assertNotNull(updated)
        assertEquals(350_000L, updated?.amountMinorUnits)
    }

    @Test
    fun testDeleteBudget() = runTest {
        val now = System.currentTimeMillis()
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(now))

        val shoppingBudget = BudgetEntity("b-shop", "p1", currentYearMonth, catShopping.id, 200_000L)
        fakeBudgetRepository.setBudgets(listOf(shoppingBudget))

        viewModel = BudgetDetailViewModel(
            budgetId = "b-shop",
            profileRepository = fakeProfileRepository,
            budgetRepository = fakeBudgetRepository,
            categoryRepository = fakeCategoryRepository,
            expenseRepository = fakeExpenseRepository
        )
        advanceUntilIdle()

        var deletedEventReceived = false
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { event ->
                if (event is BudgetDetailEvent.BudgetDeleted) {
                    deletedEventReceived = true
                }
            }
        }

        viewModel.deleteBudget()
        advanceUntilIdle()

        assertTrue(deletedEventReceived)
        assertTrue(fakeBudgetRepository.deletedIds.contains("b-shop"))
        job.cancel()
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
        val deletedIds = mutableListOf<String>()

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
                id = "b-overall",
                profileId = profileId,
                yearMonth = yearMonth,
                categoryId = null,
                amountMinorUnits = amount.minorUnits
            )
            _budgetsFlow.value = _budgetsFlow.value.filter { it.categoryId != null } + entity
            return entity
        }

        override suspend fun setCategoryBudget(profileId: String, yearMonth: String, categoryId: String, amount: Amount): BudgetEntity {
            val entity = BudgetEntity(
                id = "b-shop",
                profileId = profileId,
                yearMonth = yearMonth,
                categoryId = categoryId,
                amountMinorUnits = amount.minorUnits
            )
            _budgetsFlow.value = _budgetsFlow.value.filter { it.categoryId != categoryId } + entity
            return entity
        }

        override suspend fun deleteBudget(id: String, profileId: String) {
            deletedIds.add(id)
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
