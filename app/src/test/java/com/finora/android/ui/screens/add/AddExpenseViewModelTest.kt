package com.finora.android.ui.screens.add

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
import com.finora.android.ui.components.KeypadAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class AddExpenseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeCategoryRepository: FakeCategoryRepository
    private lateinit var fakePaymentMethodRepository: FakePaymentMethodRepository
    private lateinit var fakeExpenseRepository: FakeExpenseRepository

    private lateinit var viewModel: AddExpenseViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeProfileRepository = FakeProfileRepository()
        fakeCategoryRepository = FakeCategoryRepository()
        fakePaymentMethodRepository = FakePaymentMethodRepository()
        fakeExpenseRepository = FakeExpenseRepository()

        viewModel = AddExpenseViewModel(
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
    fun testKeypadInputSequence() = runTest {
        advanceUntilIdle()

        // Type '4', '5', '0'
        viewModel.onKeypadAction(KeypadAction.Digit('4'))
        viewModel.onKeypadAction(KeypadAction.Digit('5'))
        viewModel.onKeypadAction(KeypadAction.Digit('0'))
        assertEquals("450", viewModel.uiState.value.amountInput)

        // Type '.', '5', '0'
        viewModel.onKeypadAction(KeypadAction.Decimal)
        viewModel.onKeypadAction(KeypadAction.Digit('5'))
        viewModel.onKeypadAction(KeypadAction.Digit('0'))
        assertEquals("450.50", viewModel.uiState.value.amountInput)

        // Backspace removes last digit
        viewModel.onKeypadAction(KeypadAction.Backspace)
        assertEquals("450.5", viewModel.uiState.value.amountInput)
    }

    @Test
    fun testValidation_zeroAmountCannotSave() = runTest {
        advanceUntilIdle()

        // Default amount is "0"
        assertEquals("0", viewModel.uiState.value.amountInput)
        assertFalse(viewModel.uiState.value.isValid)

        viewModel.saveExpense()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(fakeExpenseRepository.savedExpenses.isEmpty())
    }

    @Test
    fun testSuccessfulSave() = runTest {
        advanceUntilIdle()

        // Type amount 250
        viewModel.onKeypadAction(KeypadAction.Digit('2'))
        viewModel.onKeypadAction(KeypadAction.Digit('5'))
        viewModel.onKeypadAction(KeypadAction.Digit('0'))

        assertTrue(viewModel.uiState.value.isValid)

        viewModel.saveExpense()
        advanceUntilIdle()

        assertEquals(1, fakeExpenseRepository.savedExpenses.size)
        val saved = fakeExpenseRepository.savedExpenses.first()
        assertEquals(25000L, saved.amountMinorUnits)
        assertEquals("cat-food", saved.categoryId)
    }

    @Test
    fun testEditExpense_prepopulatesAndUpdates() = runTest {
        val existingExpense = ExpenseEntity(
            id = "exp-123",
            profileId = "p1",
            amountMinorUnits = 7550L,
            currencyCode = "INR",
            categoryId = "cat-food",
            paymentMethodId = "pm-cash",
            expenseDate = 1700000000000L,
            title = "Lunch",
            notes = "Tasty burger"
        )
        fakeExpenseRepository.expenseToReturn = ExpenseWithDetails(
            expense = existingExpense,
            category = fakeCategoryRepository.categories.first(),
            paymentMethod = fakePaymentMethodRepository.methods.first()
        )

        val editViewModel = AddExpenseViewModel(
            expenseId = "exp-123",
            profileRepository = fakeProfileRepository,
            categoryRepository = fakeCategoryRepository,
            paymentMethodRepository = fakePaymentMethodRepository,
            expenseRepository = fakeExpenseRepository
        )
        advanceUntilIdle()

        assertTrue(editViewModel.uiState.value.isEditing)
        assertEquals("75.50", editViewModel.uiState.value.amountInput)
        assertEquals("Lunch", editViewModel.uiState.value.title)
        assertEquals("Tasty burger", editViewModel.uiState.value.notes)

        // Modify title
        editViewModel.onTitleChanged("Grand Lunch")
        editViewModel.saveExpense()
        advanceUntilIdle()

        assertEquals(1, fakeExpenseRepository.updatedExpenses.size)
        val updated = fakeExpenseRepository.updatedExpenses.first()
        assertEquals("exp-123", updated.id)
        assertEquals("Grand Lunch", updated.title)
        assertEquals(7550L, updated.amountMinorUnits)
    }

    // Fakes
    private class FakeProfileRepository : ProfileRepository {
        val profile = ProfileEntity("p1", "Alice", "INR")
        override fun getActiveProfileFlow(): Flow<ProfileEntity?> = flowOf(profile)
        override suspend fun getActiveProfile(): ProfileEntity? = profile
        override suspend fun createProfile(name: String, currencyCode: String, themeMode: String): ProfileEntity = profile
        override suspend fun updateThemeMode(themeMode: String) {}
        override suspend fun updateCurrencyCode(currencyCode: String) {}
    }

    private class FakeCategoryRepository : CategoryRepository {
        val categories = listOf(
            CategoryEntity("cat-food", "p1", "Food", "restaurant", "#046B5E", isDefault = true)
        )
        override fun getCategoriesFlow(profileId: String): Flow<List<CategoryEntity>> = flowOf(categories)
        override suspend fun getCategories(profileId: String): List<CategoryEntity> = categories
        override suspend fun getCategoryById(id: String, profileId: String): CategoryEntity? = categories.find { it.id == id }
        override suspend fun countExpensesForCategory(categoryId: String, profileId: String): Int = 0
        override suspend fun createCategory(profileId: String, name: String, iconName: String, colorHex: String): CategoryEntity = categories.first()
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategory(category: CategoryEntity, reassignToCategoryId: String?) {}
    }

    private class FakePaymentMethodRepository : PaymentMethodRepository {
        val methods = listOf(
            PaymentMethodEntity("pm-cash", "p1", "Cash", "CASH", isDefault = true)
        )
        override fun getPaymentMethodsFlow(profileId: String): Flow<List<PaymentMethodEntity>> = flowOf(methods)
        override suspend fun getPaymentMethods(profileId: String): List<PaymentMethodEntity> = methods
        override suspend fun getPaymentMethodById(id: String, profileId: String): PaymentMethodEntity? = methods.first()
        override suspend fun createPaymentMethod(profileId: String, name: String, type: String, isDefault: Boolean): PaymentMethodEntity = methods.first()
        override suspend fun updatePaymentMethod(paymentMethod: PaymentMethodEntity) {}
        override suspend fun deletePaymentMethod(paymentMethod: PaymentMethodEntity) {}
    }

    private class FakeExpenseRepository : ExpenseRepository {
        var expenseToReturn: ExpenseWithDetails? = null
        val savedExpenses = mutableListOf<ExpenseEntity>()
        val updatedExpenses = mutableListOf<ExpenseEntity>()
        override fun getAllExpensesFlow(profileId: String): Flow<List<ExpenseWithDetails>> = flowOf(emptyList())
        override fun getExpenseByIdFlow(id: String, profileId: String): Flow<ExpenseWithDetails?> = flowOf(expenseToReturn)
        override suspend fun getExpenseById(id: String, profileId: String): ExpenseWithDetails? = expenseToReturn
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
        ): ExpenseEntity {
            val entity = ExpenseEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                amountMinorUnits = amount.minorUnits,
                currencyCode = currencyCode,
                categoryId = categoryId,
                paymentMethodId = paymentMethodId,
                expenseDate = expenseDate,
                title = title,
                notes = notes,
                attachmentUri = attachmentUri,
                source = source
            )
            savedExpenses.add(entity)
            return entity
        }

        override suspend fun updateExpense(expense: ExpenseEntity) {
            updatedExpenses.add(expense)
        }
        override suspend fun deleteExpense(expense: ExpenseEntity) {}
        override suspend fun deleteExpenseById(id: String, profileId: String) {}
    }
}
