package com.finora.android.ui.screens.categories

import com.finora.android.core.model.Amount
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.ExpenseEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
import com.finora.android.data.local.entity.ProfileEntity
import com.finora.android.data.local.relation.ExpenseWithDetails
import com.finora.android.data.repository.CategoryRepository
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ProfileRepository
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryManagementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakeExpenseRepository: FakeExpenseRepository
    private lateinit var viewModel: CategoryManagementViewModel

    private val testProfile = ProfileEntity("p1", "Arjun", "INR")
    private val catFood = CategoryEntity("cat-food", "p1", "Food & Dining", "restaurant", "#046B5E", isDefault = true)
    private val catOther = CategoryEntity("cat-other", "p1", "Other", "more_horiz", "#707975", isDefault = true)
    private val catCustom = CategoryEntity("cat-custom", "p1", "Freelance Tools", "laptop_mac", "#29695B", isDefault = false)
    private val methodCash = PaymentMethodEntity("pm-cash", "p1", "Cash", "CASH", isDefault = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeProfileRepository = FakeProfileRepository(testProfile)
        fakeCategoryRepository = FakeCategoryRepository(listOf(catFood, catOther, catCustom))
        fakeExpenseRepository = FakeExpenseRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads categories and aggregates monthly spend`() = runTest {
        val now = System.currentTimeMillis()
        val expFood = createExpenseWithDetails("e1", 100000L, now, catFood)
        val expCustom = createExpenseWithDetails("e2", 50000L, now, catCustom)
        fakeExpenseRepository.setExpenses(listOf(expFood, expCustom))

        viewModel = CategoryManagementViewModel(fakeProfileRepository, fakeCategoryRepository, fakeExpenseRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.allCount)
        assertEquals(2, state.defaultCount)
        assertEquals(1, state.customCount)
        assertEquals(Amount(150000L), state.totalMonthlySpend)
        assertEquals(2, state.totalMonthlyTransactions)

        val foodItem = state.allCategories.first { it.category.id == "cat-food" }
        assertEquals(Amount(100000L), foodItem.amountSpentThisMonth)
        assertEquals(66, foodItem.percentageShare)

        val customItem = state.allCategories.first { it.category.id == "cat-custom" }
        assertEquals(Amount(50000L), customItem.amountSpentThisMonth)
        assertEquals(33, customItem.percentageShare)
    }

    @Test
    fun `filtering by category tab works correctly`() = runTest {
        viewModel = CategoryManagementViewModel(fakeProfileRepository, fakeCategoryRepository, fakeExpenseRepository)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.filteredCategories.size)

        viewModel.setFilterTab(CategoryFilterTab.DEFAULT)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.filteredCategories.size)
        assertTrue(viewModel.uiState.value.filteredCategories.all { it.category.isDefault })

        viewModel.setFilterTab(CategoryFilterTab.CUSTOM)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.filteredCategories.size)
        assertFalse(viewModel.uiState.value.filteredCategories.first().category.isDefault)
    }

    @Test
    fun `create category inserts new custom category`() = runTest {
        viewModel = CategoryManagementViewModel(fakeProfileRepository, fakeCategoryRepository, fakeExpenseRepository)
        advanceUntilIdle()

        viewModel.saveCategory("Gaming", "movie", "#0F6F62")
        advanceUntilIdle()

        assertEquals(4, fakeCategoryRepository.currentCategories.size)
        val created = fakeCategoryRepository.currentCategories.find { it.name == "Gaming" }
        assertNotNull(created)
        assertFalse(created!!.isDefault)
        assertEquals("movie", created.iconName)
    }

    @Test
    fun `deleting category with zero expenses triggers direct delete`() = runTest {
        viewModel = CategoryManagementViewModel(fakeProfileRepository, fakeCategoryRepository, fakeExpenseRepository)
        advanceUntilIdle()

        viewModel.requestDeleteCategory(catCustom)
        advanceUntilIdle()

        val dialogState = viewModel.uiState.value.deleteDialogState
        assertNotNull(dialogState)
        assertEquals(0, dialogState!!.expenseCount)
        assertFalse(dialogState.requiresReassignment)

        viewModel.confirmDeleteCategory()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.deleteDialogState)
        assertNull(fakeCategoryRepository.currentCategories.find { it.id == "cat-custom" })
    }

    @Test
    fun `deleting category with active expenses enforces reassignment`() = runTest {
        val now = System.currentTimeMillis()
        val exp = createExpenseWithDetails("e1", 50000L, now, catCustom)
        fakeExpenseRepository.setExpenses(listOf(exp))

        viewModel = CategoryManagementViewModel(fakeProfileRepository, fakeCategoryRepository, fakeExpenseRepository)
        advanceUntilIdle()

        fakeCategoryRepository.expenseCountMap[catCustom.id] = 1

        viewModel.requestDeleteCategory(catCustom)
        advanceUntilIdle()

        val dialogState = viewModel.uiState.value.deleteDialogState
        assertNotNull(dialogState)
        assertEquals(1, dialogState!!.expenseCount)
        assertTrue(dialogState.requiresReassignment)
        assertNotNull(dialogState.selectedReassignCategoryId)

        viewModel.confirmDeleteCategory()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.deleteDialogState)
        assertNull(fakeCategoryRepository.currentCategories.find { it.id == "cat-custom" })
        assertEquals("cat-other", fakeCategoryRepository.lastReassignedToId)
    }

    private fun createExpenseWithDetails(
        id: String,
        amountMinorUnits: Long,
        expenseDate: Long,
        category: CategoryEntity
    ): ExpenseWithDetails {
        return ExpenseWithDetails(
            expense = ExpenseEntity(
                id = id,
                profileId = "p1",
                amountMinorUnits = amountMinorUnits,
                currencyCode = "INR",
                categoryId = category.id,
                paymentMethodId = methodCash.id,
                expenseDate = expenseDate,
                title = "Expense $id"
            ),
            category = category,
            paymentMethod = methodCash
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

    private class FakeCategoryRepository(
        initialCategories: List<CategoryEntity>
    ) : CategoryRepository {
        var currentCategories = initialCategories.toMutableList()
        private val flow = MutableStateFlow<List<CategoryEntity>>(currentCategories)
        var lastReassignedToId: String? = null

        var expenseCountMap = mutableMapOf<String, Int>()

        override fun getCategoriesFlow(profileId: String): Flow<List<CategoryEntity>> = flow
        override suspend fun getCategories(profileId: String): List<CategoryEntity> = currentCategories
        override suspend fun getCategoryById(id: String, profileId: String): CategoryEntity? =
            currentCategories.find { it.id == id }

        override suspend fun countExpensesForCategory(categoryId: String, profileId: String): Int =
            expenseCountMap[categoryId] ?: 0

        override suspend fun createCategory(
            profileId: String,
            name: String,
            iconName: String,
            colorHex: String
        ): CategoryEntity {
            val cat = CategoryEntity(UUID.randomUUID().toString(), profileId, name, iconName, colorHex, isDefault = false)
            currentCategories.add(cat)
            flow.value = currentCategories.toList()
            return cat
        }

        override suspend fun updateCategory(category: CategoryEntity) {
            val idx = currentCategories.indexOfFirst { it.id == category.id }
            if (idx >= 0) {
                currentCategories[idx] = category
                flow.value = currentCategories.toList()
            }
        }

        override suspend fun deleteCategory(category: CategoryEntity, reassignToCategoryId: String?) {
            lastReassignedToId = reassignToCategoryId
            currentCategories.removeAll { it.id == category.id }
            flow.value = currentCategories.toList()
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
