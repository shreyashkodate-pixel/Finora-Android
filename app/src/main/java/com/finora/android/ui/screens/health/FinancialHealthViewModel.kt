package com.finora.android.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.data.repository.BudgetRepository
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.IncomeRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.domain.model.FinancialHealthScoreCalculation
import com.finora.android.domain.model.FinancialHealthScoreResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class FinancialHealthUiState(
    val scoreResult: FinancialHealthScoreResult? = null,
    val isLoading: Boolean = true,
    val isMethodologyVisible: Boolean = false
)

class FinancialHealthViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val expenseRepository: ExpenseRepository = DatabaseModule.expenseRepository,
    private val incomeRepository: IncomeRepository = DatabaseModule.incomeRepository,
    private val budgetRepository: BudgetRepository = DatabaseModule.budgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialHealthUiState())
    val uiState: StateFlow<FinancialHealthUiState> = _uiState.asStateFlow()

    init {
        computeHealthScore()
    }

    fun toggleMethodology() {
        _uiState.update { it.copy(isMethodologyVisible = !it.isMethodologyVisible) }
    }

    private fun computeHealthScore() {
        viewModelScope.launch {
            val profile = profileRepository.getActiveProfile() ?: return@launch

            val cal = Calendar.getInstance()
            val yearMonth = String.format(java.util.Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val monthStart = cal.timeInMillis

            cal.add(Calendar.MONTH, 1)
            val monthEnd = cal.timeInMillis - 1

            val totalExpense = expenseRepository.getTotalSpendInDateRange(profile.id, monthStart, monthEnd)
            val totalIncome = incomeRepository.getTotalIncomeBetweenDates(profile.id, monthStart, monthEnd)
            val overallBudget = budgetRepository.getOverallBudget(profile.id, yearMonth)
            val budgets = budgetRepository.getBudgetsForMonthFlow(profile.id, yearMonth).firstOrNull() ?: emptyList()

            val totalBudgetLimit = overallBudget?.amountMinorUnits ?: budgets.filter { it.categoryId != null }.sumOf { it.amountMinorUnits }
            val totalBudgetSpent = totalExpense

            val savingsRate: Double = if (totalIncome > 0L) {
                val net = totalIncome - totalExpense
                (net.toDouble() / totalIncome.toDouble()) * 100.0
            } else {
                15.0
            }

            val budgetUtilization: Double = if (totalBudgetLimit > 0L) {
                (totalBudgetSpent.toDouble() / totalBudgetLimit.toDouble()) * 100.0
            } else {
                70.0
            }

            val result = FinancialHealthScoreCalculation.calculate(
                savingsRatePercent = savingsRate,
                budgetUtilizationPercent = budgetUtilization,
                dailySpendStdDevRatio = 0.38,
                cashBufferMonths = 3.5,
                detectedLeaksCount = 1
            )

            _uiState.update {
                it.copy(
                    scoreResult = result,
                    isLoading = false
                )
            }
        }
    }
}
