package com.finora.android.ui.screens.analytics

import com.finora.android.core.model.Amount
import com.finora.android.data.local.dao.CategorySpend
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.ExpenseEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
import com.finora.android.data.local.entity.ProfileEntity
import com.finora.android.data.local.relation.ExpenseWithDetails
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.domain.model.AnalyticsTimeframe
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeExpenseRepository: FakeExpenseRepository
    private lateinit var viewModel: AnalyticsViewModel

    private val testProfile = ProfileEntity("p1", "Arjun", "INR")
    private val catFood = CategoryEntity("cat-food", "p1", "Food & Dining", "restaurant", "#046B5E", isDefault = true)
    private val catTransport = CategoryEntity("cat-transit", "p1", "Transport", "directions_car", "#004D40", isDefault = true)
    private val methodCash = PaymentMethodEntity("pm-cash", "p1", "Cash", "CASH", isDefault = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeProfileRepository = FakeProfileRepository(testProfile)
        fakeExpenseRepository = FakeExpenseRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty state when no expenses exist`() = runTest {
        viewModel = AnalyticsViewModel(fakeProfileRepository, fakeExpenseRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.hasData)
        assertEquals(Amount.ZERO, state.totalSpend)
        assertEquals(0, state.transactionCount)
        assertTrue(state.categoryDistribution.isEmpty())
        assertTrue(state.accessibleNarrative.contains("No expenses recorded"))
    }

    @Test
    fun `calculates analytics correctly for current month expenses`() = runTest {
        val now = System.currentTimeMillis()
        val exp1 = createExpenseWithDetails("e1", 100000L, now, catFood, methodCash) // 1000.00
        val exp2 = createExpenseWithDetails("e2", 50000L, now, catTransport, methodCash) // 500.00

        fakeExpenseRepository.setExpenses(listOf(exp1, exp2))

        viewModel = AnalyticsViewModel(fakeProfileRepository, fakeExpenseRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasData)
        assertEquals(Amount(150000L), state.totalSpend)
        assertEquals(2, state.transactionCount)
        assertEquals(2, state.categoryDistribution.size)

        val foodDist = state.categoryDistribution.first { it.categoryId == "cat-food" }
        assertEquals(Amount(100000L), foodDist.amount)
        assertEquals(66, foodDist.percentage) // 1000/1500 = 66.6% -> 66%

        val transportDist = state.categoryDistribution.first { it.categoryId == "cat-transit" }
        assertEquals(Amount(50000L), transportDist.amount)
        assertEquals(33, transportDist.percentage)

        assertNotNull(state.peakExpense)
        assertEquals("e1", state.peakExpense?.expense?.id)
        assertTrue(state.accessibleNarrative.contains("Food & Dining"))
    }

    @Test
    fun `switching timeframe updates date filter and recalculated aggregates`() = runTest {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -2)
        val twoMonthsAgo = cal.timeInMillis

        val expRecent = createExpenseWithDetails("e1", 100000L, now, catFood, methodCash)
        val expOld = createExpenseWithDetails("e2", 200000L, twoMonthsAgo, catTransport, methodCash)

        fakeExpenseRepository.setExpenses(listOf(expRecent, expOld))

        viewModel = AnalyticsViewModel(fakeProfileRepository, fakeExpenseRepository)
        advanceUntilIdle()

        // Month timeframe: only expRecent should be included
        assertEquals(Amount(100000L), viewModel.uiState.value.totalSpend)
        assertEquals(1, viewModel.uiState.value.transactionCount)

        // Switch to YEAR: both should be included if within current year
        viewModel.setTimeframe(AnalyticsTimeframe.YEAR)
        advanceUntilIdle()

        assertEquals(AnalyticsTimeframe.YEAR, viewModel.uiState.value.timeframe)
        assertEquals(Amount(300000L), viewModel.uiState.value.totalSpend)
        assertEquals(2, viewModel.uiState.value.transactionCount)
    }

    private fun createExpenseWithDetails(
        id: String,
        amountMinorUnits: Long,
        expenseDate: Long,
        category: CategoryEntity,
        paymentMethod: PaymentMethodEntity
    ): ExpenseWithDetails {
        return ExpenseWithDetails(
            expense = ExpenseEntity(
                id = id,
                profileId = "p1",
                amountMinorUnits = amountMinorUnits,
                currencyCode = "INR",
                categoryId = category.id,
                paymentMethodId = paymentMethod.id,
                expenseDate = expenseDate,
                title = "Expense $id",
                notes = "Test notes"
            ),
            category = category,
            paymentMethod = paymentMethod
        )
    }

    private class FakeProfileRepository(
        private var profile: ProfileEntity?
    ) : ProfileRepository {
        private val flow = MutableStateFlow(profile)

        override fun getActiveProfileFlow(): Flow<ProfileEntity?> = flow
        override suspend fun getActiveProfile(): ProfileEntity? = profile
        override suspend fun createProfile(name: String, currencyCode: String, themeMode: String): ProfileEntity {
            val p = ProfileEntity("p1", name, currencyCode, themeMode = themeMode)
            profile = p
            flow.value = p
            return p
        }
        override suspend fun updateThemeMode(themeMode: String) {}
        override suspend fun updateCurrencyCode(currencyCode: String) {}
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
        override fun getCategorySpendBreakdownFlow(profileId: String, startDate: Long, endDate: Long): Flow<List<CategorySpend>> = flowOf(emptyList())
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
