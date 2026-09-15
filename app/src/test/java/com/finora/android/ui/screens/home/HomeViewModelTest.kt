package com.finora.android.ui.screens.home

import com.finora.android.core.model.Amount
import com.finora.android.data.local.entity.BudgetEntity
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.ExpenseEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
import com.finora.android.data.local.entity.ProfileEntity
import com.finora.android.data.local.relation.ExpenseWithDetails
import com.finora.android.data.local.entity.IncomeEntity
import com.finora.android.data.repository.BudgetRepository
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.IncomeRepository
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeExpenseRepository: FakeExpenseRepository
    private lateinit var fakeBudgetRepository: FakeBudgetRepository
    private lateinit var fakeIncomeRepository: FakeIncomeRepository
    private lateinit var viewModel: HomeViewModel

    private val testProfile = ProfileEntity("p1", "Arjun", "INR")
    private val catFood = CategoryEntity("cat-food", "p1", "Food & Dining", "restaurant", "#046B5E", isDefault = true)
    private val catTransit = CategoryEntity("cat-transit", "p1", "Transit & Fuel", "directions_car", "#D97706", isDefault = true)
    private val catRent = CategoryEntity("cat-rent", "p1", "Rent & Utilities", "home", "#2563EB", isDefault = true)
    private val pmUpi = PaymentMethodEntity("pm-upi", "p1", "UPI", "UPI", isDefault = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeProfileRepository = FakeProfileRepository(testProfile)
        fakeExpenseRepository = FakeExpenseRepository()
        fakeBudgetRepository = FakeBudgetRepository()
        fakeIncomeRepository = FakeIncomeRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testEmptyState_whenNoExpensesExist() = runTest {
        viewModel = HomeViewModel(
            profileRepository = fakeProfileRepository,
            expenseRepository = fakeExpenseRepository,
            budgetRepository = fakeBudgetRepository,
            incomeRepository = fakeIncomeRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Arjun", state.profileName)
        assertEquals("A", state.avatarLetter)
        assertFalse(state.hasExpenses)
        assertEquals(0L, state.monthOutflow.minorUnits)
        assertEquals(0L, state.todaySpend.minorUnits)
        assertNull(state.peakOutflow)
        assertTrue(state.topCategories.isEmpty())
        assertTrue(state.recentExpenses.isEmpty())
    }

    @Test
    fun testPopulatedDashboard_calculationsAndMetrics() = runTest {
        val now = System.currentTimeMillis()

        val e1 = ExpenseWithDetails(
            expense = ExpenseEntity(
                id = "exp-1",
                profileId = "p1",
                amountMinorUnits = 1420000L, // 14,200.00
                currencyCode = "INR",
                categoryId = catFood.id,
                paymentMethodId = pmUpi.id,
                expenseDate = now,
                title = "Groceries & Food",
                notes = null,
                attachmentUri = null,
                source = "MANUAL",
                createdAt = now
            ),
            category = catFood,
            paymentMethod = pmUpi
        )

        val e2 = ExpenseWithDetails(
            expense = ExpenseEntity(
                id = "exp-2",
                profileId = "p1",
                amountMinorUnits = 1200000L, // 12,000.00
                currencyCode = "INR",
                categoryId = catRent.id,
                paymentMethodId = pmUpi.id,
                expenseDate = now - 1000L,
                title = "Rent & Utilities",
                notes = null,
                attachmentUri = null,
                source = "MANUAL",
                createdAt = now - 1000L
            ),
            category = catRent,
            paymentMethod = pmUpi
        )

        val e3 = ExpenseWithDetails(
            expense = ExpenseEntity(
                id = "exp-3",
                profileId = "p1",
                amountMinorUnits = 435000L, // 4,350.00
                currencyCode = "INR",
                categoryId = catTransit.id,
                paymentMethodId = pmUpi.id,
                expenseDate = now - 2000L,
                title = "Transit & Fuel",
                notes = null,
                attachmentUri = null,
                source = "MANUAL",
                createdAt = now - 2000L
            ),
            category = catTransit,
            paymentMethod = pmUpi
        )

        fakeExpenseRepository.setExpenses(listOf(e1, e2, e3))

        // Set monthly budget: 50,000.00 = 5,000,000 paise
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        fakeBudgetRepository.setBudget(
            BudgetEntity(
                id = "b1",
                profileId = "p1",
                yearMonth = currentYearMonth,
                categoryId = null,
                amountMinorUnits = 5_000_000L
            )
        )

        viewModel = HomeViewModel(
            profileRepository = fakeProfileRepository,
            expenseRepository = fakeExpenseRepository,
            budgetRepository = fakeBudgetRepository,
            incomeRepository = fakeIncomeRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasExpenses)
        // Total = 14200 + 12000 + 4350 = 30550 -> 3055000L
        assertEquals(3055000L, state.monthOutflow.minorUnits)

        // Peak outflow is e1 (14,200.00)
        assertNotNull(state.peakOutflow)
        assertEquals("exp-1", state.peakOutflow?.expense?.id)
        assertEquals(1420000L, state.peakOutflow?.expense?.amountMinorUnits)

        // Budget calculations
        assertNotNull(state.budgetSummary)
        val budget = state.budgetSummary!!
        assertEquals(5_000_000L, budget.budgetAmount.minorUnits)
        assertEquals(3_055_000L, budget.spentAmount.minorUnits)
        assertEquals(1_945_000L, budget.remainingAmount.minorUnits)
        assertEquals(BudgetStatus.ON_TRACK, budget.status) // 30550 / 50000 = 61.1% < 80%

        // Top categories
        assertEquals(3, state.topCategories.size)
        assertEquals(catFood.name, state.topCategories[0].categoryName)
        assertEquals(catRent.name, state.topCategories[1].categoryName)
        assertEquals(catTransit.name, state.topCategories[2].categoryName)

        // Weekly rhythm has 7 days, with today as last
        assertEquals(7, state.weeklyRhythm.size)
        assertTrue(state.weeklyRhythm.last().isToday)
        assertFalse(state.weeklyRhythm.first().isToday)

        // Recent expenses
        assertEquals(3, state.recentExpenses.size)
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

    private class FakeBudgetRepository : BudgetRepository {
        private val _budgetFlow = MutableStateFlow<BudgetEntity?>(null)

        fun setBudget(budget: BudgetEntity?) {
            _budgetFlow.value = budget
        }

        override fun getBudgetsForMonthFlow(profileId: String, yearMonth: String): Flow<List<BudgetEntity>> =
            flowOf(_budgetFlow.value?.let { listOf(it) } ?: emptyList())

        override fun getOverallBudgetFlow(profileId: String, yearMonth: String): Flow<BudgetEntity?> = _budgetFlow
        override suspend fun getOverallBudget(profileId: String, yearMonth: String): BudgetEntity? = _budgetFlow.value
        override suspend fun getCategoryBudget(profileId: String, yearMonth: String, categoryId: String): BudgetEntity? = null
        override suspend fun setOverallBudget(profileId: String, yearMonth: String, amount: Amount): BudgetEntity = throw UnsupportedOperationException()
        override suspend fun setCategoryBudget(profileId: String, yearMonth: String, categoryId: String, amount: Amount): BudgetEntity = throw UnsupportedOperationException()
        override suspend fun deleteBudget(id: String, profileId: String) {}
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

        override fun getExpensesByDateRangeFlow(profileId: String, startDate: Long, endDate: Long): Flow<List<ExpenseWithDetails>> =
            flowOf(_expensesFlow.value.filter { it.expense.expenseDate in startDate..endDate })

        override fun getRecentExpensesFlow(profileId: String, limit: Int): Flow<List<ExpenseWithDetails>> =
            flowOf(_expensesFlow.value.take(limit))

        override fun getTotalSpendInDateRangeFlow(profileId: String, startDate: Long, endDate: Long): Flow<Long> =
            flowOf(_expensesFlow.value.filter { it.expense.expenseDate in startDate..endDate }.sumOf { it.expense.amountMinorUnits })

        override suspend fun getTotalSpendInDateRange(profileId: String, startDate: Long, endDate: Long): Long =
            _expensesFlow.value.filter { it.expense.expenseDate in startDate..endDate }.sumOf { it.expense.amountMinorUnits }

        override suspend fun getCategorySpendInDateRange(profileId: String, categoryId: String, startDate: Long, endDate: Long): Long = 0L
        override fun getCategorySpendBreakdownFlow(profileId: String, startDate: Long, endDate: Long): Flow<List<com.finora.android.data.local.dao.CategorySpend>> = flowOf(emptyList())
        override suspend fun getHighestExpenseInDateRange(profileId: String, startDate: Long, endDate: Long): ExpenseWithDetails? =
            _expensesFlow.value.filter { it.expense.expenseDate in startDate..endDate }.maxByOrNull { it.expense.amountMinorUnits }

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

    private class FakeIncomeRepository : IncomeRepository {
        private val _incomeFlow = MutableStateFlow<List<IncomeEntity>>(emptyList())
        override fun getIncomeForProfileFlow(profileId: String): Flow<List<IncomeEntity>> = _incomeFlow
        override fun getIncomeBetweenDatesFlow(profileId: String, startDate: Long, endDate: Long): Flow<List<IncomeEntity>> = _incomeFlow
        override fun getTotalIncomeBetweenDatesFlow(profileId: String, startDate: Long, endDate: Long): Flow<Long> = flowOf(0L)
        override suspend fun getIncomeBetweenDates(profileId: String, startDate: Long, endDate: Long): List<IncomeEntity> = emptyList()
        override suspend fun getTotalIncomeBetweenDates(profileId: String, startDate: Long, endDate: Long): Long = 0L
        override suspend fun getIncomeById(id: String, profileId: String): IncomeEntity? = null
        override suspend fun createIncome(profileId: String, amount: Amount, currencyCode: String, source: String, incomeDate: Long, accountId: String?, notes: String?): IncomeEntity = throw UnsupportedOperationException()
        override suspend fun updateIncome(income: IncomeEntity) {}
        override suspend fun deleteIncome(income: IncomeEntity) {}
        override suspend fun deleteIncomeById(id: String, profileId: String) {}
    }
}
