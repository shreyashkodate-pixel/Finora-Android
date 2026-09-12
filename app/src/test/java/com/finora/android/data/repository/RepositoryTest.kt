package com.finora.android.data.repository

import com.finora.android.core.model.Amount
import com.finora.android.data.local.dao.CategoryDao
import com.finora.android.data.local.dao.CategorySpend
import com.finora.android.data.local.dao.ExpenseDao
import com.finora.android.data.local.dao.PaymentMethodDao
import com.finora.android.data.local.dao.ProfileDao
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.ExpenseEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
import com.finora.android.data.local.entity.ProfileEntity
import com.finora.android.data.local.relation.ExpenseWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RepositoryTest {

    private lateinit var fakeExpenseDao: FakeExpenseDao
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var fakePaymentMethodDao: FakePaymentMethodDao
    private lateinit var fakeProfileDao: FakeProfileDao

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var profileRepository: ProfileRepository

    private val testProfileId = "test-profile-123"

    @Before
    fun setup() {
        fakeExpenseDao = FakeExpenseDao()
        fakeCategoryDao = FakeCategoryDao()
        fakePaymentMethodDao = FakePaymentMethodDao()
        fakeProfileDao = FakeProfileDao()

        expenseRepository = ExpenseRepositoryImpl(fakeExpenseDao)
        categoryRepository = CategoryRepositoryImpl(fakeCategoryDao, fakeExpenseDao)
        profileRepository = ProfileRepositoryImpl(fakeProfileDao, fakeCategoryDao, fakePaymentMethodDao)
    }

    @Test
    fun testCreateExpense_rejectsZeroOrNegativeAmount() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                expenseRepository.createExpense(
                    profileId = testProfileId,
                    amount = Amount.ZERO,
                    currencyCode = "INR",
                    categoryId = "cat-1",
                    expenseDate = System.currentTimeMillis()
                )
            }
        }

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                expenseRepository.createExpense(
                    profileId = testProfileId,
                    amount = Amount(-500L),
                    currencyCode = "INR",
                    categoryId = "cat-1",
                    expenseDate = System.currentTimeMillis()
                )
            }
        }
    }

    @Test
    fun testCreateExpense_rejectsInvalidDate() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                expenseRepository.createExpense(
                    profileId = testProfileId,
                    amount = Amount.fromMajor(100),
                    currencyCode = "INR",
                    categoryId = "cat-1",
                    expenseDate = 0L
                )
            }
        }
    }

    @Test
    fun testCreateExpense_success() = runBlocking {
        val expense = expenseRepository.createExpense(
            profileId = testProfileId,
            amount = Amount.fromMajor(250),
            currencyCode = "INR",
            categoryId = "cat-1",
            expenseDate = 1700000000000L,
            title = "Groceries"
        )

        assertNotNull(expense.id)
        assertEquals(25000L, expense.amountMinorUnits)
        assertEquals("INR", expense.currencyCode)
        assertEquals("Groceries", expense.title)
        assertEquals(1, fakeExpenseDao.expenses.size)
    }

    @Test
    fun testCategoryDeletion_withActiveExpensesRequiresReassignment() {
        val category = CategoryEntity(
            id = "cat-food",
            profileId = testProfileId,
            name = "Food",
            iconName = "restaurant",
            colorHex = "#046B5E"
        )
        fakeCategoryDao.categories.add(category)
        fakeCategoryDao.expenseCounts["cat-food"] = 5

        // Should throw when reassignToCategoryId is null per FR-CAT-V1.0-005
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                categoryRepository.deleteCategory(category, reassignToCategoryId = null)
            }
        }

        // Should succeed when reassignToCategoryId is provided
        runBlocking {
            categoryRepository.deleteCategory(category, reassignToCategoryId = "cat-other")
        }

        assertTrue(fakeExpenseDao.reassignedCategories.contains("cat-food" to "cat-other"))
        assertTrue(fakeCategoryDao.categories.isEmpty())
    }

    @Test
    fun testProfileCreation_seedsStarterData() = runBlocking {
        val profile = profileRepository.createProfile("Alice", "INR", "DARK")

        assertNotNull(profile.id)
        assertEquals("Alice", profile.name)
        assertEquals("INR", profile.currencyCode)

        // Starter categories and payment methods must be seeded
        assertEquals(9, fakeCategoryDao.categories.size)
        assertEquals(7, fakePaymentMethodDao.paymentMethods.size)
    }

    // Fakes for testing
    private class FakeExpenseDao : ExpenseDao {
        val expenses = mutableListOf<ExpenseEntity>()
        val reassignedCategories = mutableListOf<Pair<String, String>>()

        override fun getAllExpensesWithDetailsFlow(profileId: String): Flow<List<ExpenseWithDetails>> = flowOf(emptyList())
        override fun getExpenseWithDetailsFlow(id: String, profileId: String): Flow<ExpenseWithDetails?> = flowOf(null)
        override suspend fun getExpenseWithDetails(id: String, profileId: String): ExpenseWithDetails? = null
        override fun getExpensesByDateRangeFlow(profileId: String, startDate: Long, endDate: Long): Flow<List<ExpenseWithDetails>> = flowOf(emptyList())
        override fun getRecentExpensesFlow(profileId: String, limit: Int): Flow<List<ExpenseWithDetails>> = flowOf(emptyList())
        override fun getTotalSpendInDateRangeFlow(profileId: String, startDate: Long, endDate: Long): Flow<Long> = flowOf(0L)
        override suspend fun getTotalSpendInDateRange(profileId: String, startDate: Long, endDate: Long): Long = 0L
        override suspend fun getCategorySpendInDateRange(profileId: String, categoryId: String, startDate: Long, endDate: Long): Long = 0L
        override fun getCategorySpendBreakdownFlow(profileId: String, startDate: Long, endDate: Long): Flow<List<CategorySpend>> = flowOf(emptyList())
        override suspend fun getHighestExpenseInDateRange(profileId: String, startDate: Long, endDate: Long): ExpenseWithDetails? = null

        override suspend fun reassignCategory(oldCategoryId: String, newCategoryId: String, profileId: String, timestamp: Long) {
            reassignedCategories.add(oldCategoryId to newCategoryId)
        }

        override suspend fun insertExpense(expense: ExpenseEntity) {
            expenses.add(expense)
        }

        override suspend fun updateExpense(expense: ExpenseEntity) {
            val idx = expenses.indexOfFirst { it.id == expense.id }
            if (idx >= 0) expenses[idx] = expense
        }

        override suspend fun deleteExpense(expense: ExpenseEntity) {
            expenses.removeAll { it.id == expense.id }
        }

        override suspend fun deleteExpenseById(id: String, profileId: String) {
            expenses.removeAll { it.id == id && it.profileId == profileId }
        }
    }

    private class FakeCategoryDao : CategoryDao {
        val categories = mutableListOf<CategoryEntity>()
        val expenseCounts = mutableMapOf<String, Int>()

        override fun getCategoriesFlow(profileId: String): Flow<List<CategoryEntity>> = flowOf(categories)
        override suspend fun getCategories(profileId: String): List<CategoryEntity> = categories
        override suspend fun getCategoryById(id: String, profileId: String): CategoryEntity? = categories.find { it.id == id }
        override suspend fun countExpensesForCategory(categoryId: String, profileId: String): Int = expenseCounts[categoryId] ?: 0

        override suspend fun insertCategory(category: CategoryEntity) {
            categories.add(category)
        }

        override suspend fun insertCategories(categories: List<CategoryEntity>) {
            this.categories.addAll(categories)
        }

        override suspend fun updateCategory(category: CategoryEntity) {
            val idx = categories.indexOfFirst { it.id == category.id }
            if (idx >= 0) categories[idx] = category
        }

        override suspend fun deleteCategory(category: CategoryEntity) {
            categories.removeAll { it.id == category.id }
        }
    }

    private class FakePaymentMethodDao : PaymentMethodDao {
        val paymentMethods = mutableListOf<PaymentMethodEntity>()

        override fun getPaymentMethodsFlow(profileId: String): Flow<List<PaymentMethodEntity>> = flowOf(paymentMethods)
        override suspend fun getPaymentMethods(profileId: String): List<PaymentMethodEntity> = paymentMethods
        override suspend fun getPaymentMethodById(id: String, profileId: String): PaymentMethodEntity? = paymentMethods.find { it.id == id }
        override suspend fun insertPaymentMethod(paymentMethod: PaymentMethodEntity) { paymentMethods.add(paymentMethod) }
        override suspend fun insertPaymentMethods(paymentMethods: List<PaymentMethodEntity>) { this.paymentMethods.addAll(paymentMethods) }
        override suspend fun updatePaymentMethod(paymentMethod: PaymentMethodEntity) {
            val idx = this.paymentMethods.indexOfFirst { it.id == paymentMethod.id }
            if (idx >= 0) this.paymentMethods[idx] = paymentMethod
        }
        override suspend fun deletePaymentMethod(paymentMethod: PaymentMethodEntity) { this.paymentMethods.removeAll { it.id == paymentMethod.id } }
    }

    private class FakeProfileDao : ProfileDao {
        var profile: ProfileEntity? = null

        override fun getActiveProfileFlow(): Flow<ProfileEntity?> = flowOf(profile)
        override suspend fun getActiveProfile(): ProfileEntity? = profile
        override suspend fun getProfileById(id: String): ProfileEntity? = if (profile?.id == id) profile else null
        override suspend fun insertProfile(profile: ProfileEntity) { this.profile = profile }
        override suspend fun updateProfile(profile: ProfileEntity) { this.profile = profile }
        override suspend fun updateThemeMode(id: String, themeMode: String) { profile = profile?.copy(themeMode = themeMode) }
        override suspend fun updateCurrencyCode(id: String, currencyCode: String) { profile = profile?.copy(currencyCode = currencyCode) }
    }
}
