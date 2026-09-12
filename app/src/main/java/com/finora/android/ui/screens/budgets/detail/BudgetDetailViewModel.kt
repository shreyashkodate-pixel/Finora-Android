package com.finora.android.ui.screens.budgets.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.Amount
import com.finora.android.core.model.AppCurrency
import com.finora.android.data.local.entity.BudgetEntity
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.relation.ExpenseWithDetails
import com.finora.android.data.repository.BudgetRepository
import com.finora.android.data.repository.CategoryRepository
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.domain.model.BudgetCalculator
import com.finora.android.domain.model.BudgetStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BudgetDetailUiState(
    val budgetId: String = "",
    val budgetTitle: String = "Monthly Budget",
    val isCategoryBudget: Boolean = false,
    val category: CategoryEntity? = null,
    val currencySymbol: String = "₹",
    val cycleMonthLabel: String = "",
    val daysRemaining: Int = 0,
    val budgetAmount: Amount = Amount.ZERO,
    val spentAmount: Amount = Amount.ZERO,
    val remainingAmount: Amount = Amount.ZERO,
    val overBudgetAmount: Amount = Amount.ZERO,
    val percentageUsed: Float = 0f,
    val status: BudgetStatus = BudgetStatus.ON_TRACK,
    val averageTransactionSpend: Amount = Amount.ZERO,
    val recommendedDailyBurn: Amount = Amount.ZERO,
    val contributingExpenses: List<ExpenseWithDetails> = emptyList(),
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isLoading: Boolean = true
)

sealed interface BudgetDetailEvent {
    data object BudgetDeleted : BudgetDetailEvent
    data class ShowMessage(val message: String) : BudgetDetailEvent
}

class BudgetDetailViewModel(
    private val budgetId: String,
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val budgetRepository: BudgetRepository = DatabaseModule.budgetRepository,
    private val categoryRepository: CategoryRepository = DatabaseModule.categoryRepository,
    private val expenseRepository: ExpenseRepository = DatabaseModule.expenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetDetailUiState(budgetId = budgetId))
    val uiState: StateFlow<BudgetDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BudgetDetailEvent>()
    val events: SharedFlow<BudgetDetailEvent> = _events.asSharedFlow()

    private var currentProfileId: String = ""
    private var currentYearMonth: String = ""
    private var activeBudgetEntity: BudgetEntity? = null

    init {
        loadBudgetDetail()
    }

    private fun loadBudgetDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val profile = profileRepository.getActiveProfile()
            if (profile == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            currentProfileId = profile.id
            val currency = AppCurrency.fromCode(profile.currencyCode)

            val calendar = Calendar.getInstance()
            currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
            val monthDisplay = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val remainingDays = (totalDays - dayOfMonth + 1).coerceAtLeast(1)

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
                    currencySymbol = currency.symbol,
                    cycleMonthLabel = "$monthDisplay Cycle",
                    daysRemaining = remainingDays
                )
            }

            launch {
                combine(
                    budgetRepository.getBudgetsForMonthFlow(profile.id, currentYearMonth),
                    categoryRepository.getCategoriesFlow(profile.id),
                    expenseRepository.getExpensesByDateRangeFlow(profile.id, startOfMonth, endOfMonth)
                ) { budgets, categories, monthExpenses ->
                    val budget = budgets.find { it.id == budgetId } ?: budgets.find { it.categoryId == null }
                    activeBudgetEntity = budget

                    if (budget == null) {
                        return@combine null
                    }

                    val isCategory = budget.categoryId != null
                    val category = if (isCategory) categories.find { it.id == budget.categoryId } else null
                    val title = if (isCategory) "${category?.name ?: "Category"} Budget" else "Overall Monthly Budget"

                    // Filter contributing transactions
                    val contributing = if (isCategory) {
                        monthExpenses.filter { it.expense.categoryId == budget.categoryId }
                    } else {
                        monthExpenses
                    }.sortedByDescending { it.expense.expenseDate }

                    val totalSpentUnits = contributing.sumOf { it.expense.amountMinorUnits }
                    val summary = BudgetCalculator.calculate(
                        budgetAmount = Amount(budget.amountMinorUnits),
                        spentAmount = Amount(totalSpentUnits)
                    )

                    // Average transaction spend
                    val avgUnits = if (contributing.isNotEmpty()) totalSpentUnits / contributing.size else 0L
                    val avgSpend = Amount(avgUnits)

                    // Recommended daily burn
                    val recommendedUnits = if (remainingDays > 0) {
                        summary.remainingAmount.minorUnits / remainingDays
                    } else 0L
                    val recommendedBurn = Amount(recommendedUnits)

                    BudgetDetailCalculations(
                        budget = budget,
                        title = title,
                        isCategory = isCategory,
                        category = category,
                        summary = summary,
                        avgSpend = avgSpend,
                        recommendedBurn = recommendedBurn,
                        contributing = contributing
                    )
                }.collect { result ->
                    if (result != null) {
                        _uiState.update {
                            it.copy(
                                budgetTitle = result.title,
                                isCategoryBudget = result.isCategory,
                                category = result.category,
                                budgetAmount = result.summary.budgetAmount,
                                spentAmount = result.summary.spentAmount,
                                remainingAmount = result.summary.remainingAmount,
                                overBudgetAmount = result.summary.overBudgetAmount,
                                percentageUsed = result.summary.percentageUsed,
                                status = result.summary.status,
                                averageTransactionSpend = result.avgSpend,
                                recommendedDailyBurn = result.recommendedBurn,
                                contributingExpenses = result.contributing,
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun updateBudgetCap(newAmount: Amount) {
        val budget = activeBudgetEntity ?: return
        viewModelScope.launch {
            if (budget.categoryId != null) {
                budgetRepository.setCategoryBudget(currentProfileId, currentYearMonth, budget.categoryId, newAmount)
            } else {
                budgetRepository.setOverallBudget(currentProfileId, currentYearMonth, newAmount)
            }
            showEditDialog(false)
            _events.emit(BudgetDetailEvent.ShowMessage("Budget limit updated successfully"))
        }
    }

    fun deleteBudget() {
        val budget = activeBudgetEntity ?: return
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget.id, currentProfileId)
            showDeleteDialog(false)
            _events.emit(BudgetDetailEvent.BudgetDeleted)
        }
    }

    fun showEditDialog(show: Boolean) {
        _uiState.update { it.copy(showEditDialog = show) }
    }

    fun showDeleteDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteDialog = show) }
    }

    private data class BudgetDetailCalculations(
        val budget: BudgetEntity,
        val title: String,
        val isCategory: Boolean,
        val category: CategoryEntity?,
        val summary: com.finora.android.domain.model.BudgetSummary,
        val avgSpend: Amount,
        val recommendedBurn: Amount,
        val contributing: List<ExpenseWithDetails>
    )
}
