package com.finora.android.ui.screens.detail

import com.finora.android.core.model.Amount
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.ExpenseEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
import com.finora.android.data.local.entity.ProfileEntity
import com.finora.android.data.local.relation.ExpenseWithDetails
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ProfileRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeExpenseRepository: FakeExpenseRepository
    private lateinit var viewModel: ExpenseDetailViewModel

    private val testProfile = ProfileEntity("p1", "Alice", "INR")
    private val catFood = CategoryEntity("cat-food", "p1", "Food & Dining", "restaurant", "#046B5E", isDefault = true)
    private val pmUpi = PaymentMethodEntity("pm-upi", "p1", "UPI", "UPI", isDefault = true)
    private val testExpenseWithDetails = ExpenseWithDetails(
        expense = ExpenseEntity(
            id = "exp-123",
            profileId = "p1",
            amountMinorUnits = 75000L, // 750.00
            currencyCode = "INR",
            categoryId = catFood.id,
            paymentMethodId = pmUpi.id,
            expenseDate = 1700000000000L,
            title = "Weekend Dinner",
            notes = "Family get-together",
            attachmentUri = null,
            source = "MANUAL",
            createdAt = 1700000000000L
        ),
        category = catFood,
        paymentMethod = pmUpi
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeProfileRepository = FakeProfileRepository(testProfile)
        fakeExpenseRepository = FakeExpenseRepository(testExpenseWithDetails)

        viewModel = ExpenseDetailViewModel(
            expenseId = "exp-123",
            profileRepository = fakeProfileRepository,
            expenseRepository = fakeExpenseRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadExpenseDetails_success() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.expenseDetails)
        assertEquals("exp-123", state.expenseDetails?.expense?.id)
        assertEquals(75000L, state.expenseDetails?.expense?.amountMinorUnits)
        assertEquals("Weekend Dinner", state.expenseDetails?.expense?.title)
        assertEquals("Food & Dining", state.expenseDetails?.category?.name)
        assertEquals("UPI", state.expenseDetails?.paymentMethod?.name)
        assertEquals("₹", state.currencySymbol)
    }

    @Test
    fun testDeleteDialogToggle() = runTest {
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDeleteDialog)
        viewModel.showDeleteDialog(true)
        assertTrue(viewModel.uiState.value.showDeleteDialog)
        viewModel.showDeleteDialog(false)
        assertFalse(viewModel.uiState.value.showDeleteDialog)
    }

    @Test
    fun testDeleteExpense_emitsDeletedEventAndRemovesFromRepository() = runTest {
        advanceUntilIdle()

        var deletedEventReceived = false
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { event ->
                if (event is ExpenseDetailEvent.ExpenseDeleted) {
                    deletedEventReceived = true
                }
            }
        }

        viewModel.deleteExpense()
        advanceUntilIdle()

        assertTrue(deletedEventReceived)
        assertTrue(fakeExpenseRepository.deletedIds.contains("exp-123"))
        job.cancel()
    }

    // Fakes
    private class FakeProfileRepository(val profile: ProfileEntity) : ProfileRepository {
        override fun getActiveProfileFlow(): Flow<ProfileEntity?> = flowOf(profile)
        override suspend fun getActiveProfile(): ProfileEntity? = profile
        override suspend fun createProfile(name: String, currencyCode: String, themeMode: String): ProfileEntity = profile
        override suspend fun updateThemeMode(themeMode: String) {}
        override suspend fun updateCurrencyCode(currencyCode: String) {}
    }

    private class FakeExpenseRepository(var item: ExpenseWithDetails?) : ExpenseRepository {
        val deletedIds = mutableListOf<String>()

        override fun getAllExpensesFlow(profileId: String): Flow<List<ExpenseWithDetails>> = flowOf(emptyList())
        override fun getExpenseByIdFlow(id: String, profileId: String): Flow<ExpenseWithDetails?> =
            flowOf(if (id == item?.expense?.id) item else null)

        override suspend fun getExpenseById(id: String, profileId: String): ExpenseWithDetails? =
            if (id == item?.expense?.id) item else null

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
        override suspend fun deleteExpenseById(id: String, profileId: String) {
            deletedIds.add(id)
            if (item?.expense?.id == id) {
                item = null
            }
        }
    }
}
