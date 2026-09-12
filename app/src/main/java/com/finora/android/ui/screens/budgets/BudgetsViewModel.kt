package com.finora.android.ui.screens.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.Amount
import com.finora.android.core.model.AppCurrency
import com.finora.android.data.local.entity.BudgetEntity
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.repository.BudgetRepository
import com.finora.android.data.repository.CategoryRepository
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.domain.model.BudgetCalculator
import com.finora.android.domain.model.BudgetStatus
import com.finora.android.domain.model.BudgetSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategoryBudgetItem(
    val budgetId: String,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: String,
    val budgetAmount: Amount,
    val spentAmount: Amount,
    val remainingAmount: Amount,
    val overBudgetAmount: Amount,
    val percentageUsed: Float,
    val status: BudgetStatus
)

data class BudgetsUiState(
    val profileId: String = "",
    val currencySymbol: String = "₹",
    val cycleMonthLabel: String = "",
    val daysRemainingInMonth: Int = 0,
    val overallBudget: BudgetEntity? = null,
    val overallSummary: BudgetSummary? = null,
    val safeBurnRatePerDay: Amount = Amount.ZERO,
    val categoryBudgets: List<CategoryBudgetItem> = emptyList(),
    val availableCategoriesForBudget: List<CategoryEntity> = emptyList(),
    val showSetOverallDialog: Boolean = false,
    val showAddCategoryDialog: Boolean = false,
    val isLoading: Boolean = true
)

class BudgetsViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val budgetRepository: BudgetRepository = DatabaseModule.budgetRepository,
    private val categoryRepository: CategoryRepository = DatabaseModule.categoryRepository,
    private val expenseRepository: ExpenseRepository = DatabaseModule.expenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    private var currentProfileId: String = ""
    private var currentYearMonth: String = ""

    init {
        loadBudgets()
    }

    private fun loadBudgets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val profile = profileRepository.getActiveProfile()
                ?: profileRepository.createProfile("Personal", AppCurrency.DEFAULT.code)
            currentProfileId = profile.id

            val currency = AppCurrency.fromCode(profile.currencyCode)

            val calendar = Calendar.getInstance()
            currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
            val monthDisplay = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(calendar.time)

            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val remainingDays = (totalDays - dayOfMonth + 1).coerceAtLeast(1)

            // Calculate start and end of month millis
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.set(Calendar.DAY_OF_MONTH, totalDays)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfMonth = calendar.timeInMillis

            _uiState.update {
                it.copy(
                    profileId = profile.id,
                    currencySymbol = currency.symbol,
                    cycleMonthLabel = monthDisplay,
                    daysRemainingInMonth = remainingDays
                )
            }

            // Combine reactive flows: budgets, categories, month expenses
            launch {
                combine(
                    budgetRepository.getBudgetsForMonthFlow(profile.id, currentYearMonth),
                    categoryRepository.getCategoriesFlow(profile.id),
                    expenseRepository.getExpensesByDateRangeFlow(profile.id, startOfMonth, endOfMonth)
                ) { allBudgets, categories, monthExpenses ->
                    val overallBudgetEntity = allBudgets.find { it.categoryId == null }
                    val categoryBudgetsEntities = allBudgets.filter { it.categoryId != null }

                    // Total month spend
                    val totalMonthUnits = monthExpenses.sumOf { it.expense.amountMinorUnits }
                    val totalMonthAmount = Amount(totalMonthUnits)

                    // Overall summary
                    val overallSummary = overallBudgetEntity?.let { budget ->
                        BudgetCalculator.calculate(
                            budgetAmount = Amount(budget.amountMinorUnits),
                            spentAmount = totalMonthAmount
                        )
                    }

                    // Safe daily burn rate
                    val safeDailyBurn = if (overallSummary != null && remainingDays > 0) {
                        Amount(overallSummary.remainingAmount.minorUnits / remainingDays)
                    } else {
                        Amount.ZERO
                    }

                    // Map category budgets
                    val categoryBudgetItems = categoryBudgetsEntities.mapNotNull { bEntity ->
                        val cat = categories.find { it.id == bEntity.categoryId } ?: return@mapNotNull null
                        val catSpentUnits = monthExpenses
                            .filter { it.expense.categoryId == cat.id }
                            .sumOf { it.expense.amountMinorUnits }

                        val summary = BudgetCalculator.calculate(
                            budgetAmount = Amount(bEntity.amountMinorUnits),
                            spentAmount = Amount(catSpentUnits)
                        )

                        CategoryBudgetItem(
                            budgetId = bEntity.id,
                            categoryId = cat.id,
                            categoryName = cat.name,
                            categoryIcon = cat.iconName,
                            categoryColorHex = cat.colorHex,
                            budgetAmount = summary.budgetAmount,
                            spentAmount = summary.spentAmount,
                            remainingAmount = summary.remainingAmount,
                            overBudgetAmount = summary.overBudgetAmount,
                            percentageUsed = summary.percentageUsed,
                            status = summary.status
                        )
                    }

                    // Available categories that do not have a budget yet
                    val allocatedCategoryIds = categoryBudgetsEntities.mapNotNull { it.categoryId }.toSet()
                    val availableCats = categories.filter { it.id !in allocatedCategoryIds }

                    BudgetsData(
                        overallBudget = overallBudgetEntity,
                        overallSummary = overallSummary,
                        safeBurnRatePerDay = safeDailyBurn,
                        categoryBudgets = categoryBudgetItems,
                        availableCategories = availableCats
                    )
                }.collect { data ->
                    _uiState.update {
                        it.copy(
                            overallBudget = data.overallBudget,
                            overallSummary = data.overallSummary,
                            safeBurnRatePerDay = data.safeBurnRatePerDay,
                            categoryBudgets = data.categoryBudgets,
                            availableCategoriesForBudget = data.availableCategories,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun setOverallBudget(amount: Amount) {
        viewModelScope.launch {
            budgetRepository.setOverallBudget(currentProfileId, currentYearMonth, amount)
            showSetOverallDialog(false)
        }
    }

    fun setCategoryBudget(categoryId: String, amount: Amount) {
        viewModelScope.launch {
            budgetRepository.setCategoryBudget(currentProfileId, currentYearMonth, categoryId, amount)
            showAddCategoryDialog(false)
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(id, currentProfileId)
        }
    }

    fun showSetOverallDialog(show: Boolean) {
        _uiState.update { it.copy(showSetOverallDialog = show) }
    }

    fun showAddCategoryDialog(show: Boolean) {
        _uiState.update { it.copy(showAddCategoryDialog = show) }
    }

    private data class BudgetsData(
        val overallBudget: BudgetEntity?,
        val overallSummary: BudgetSummary?,
        val safeBurnRatePerDay: Amount,
        val categoryBudgets: List<CategoryBudgetItem>,
        val availableCategories: List<CategoryEntity>
    )
}
