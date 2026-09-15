package com.finora.android.ui.screens.anomaly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.domain.model.LeakHunterCalculation
import com.finora.android.domain.model.LeakHunterSummary
import com.finora.android.domain.model.SimpleExpenseRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnomalyGuardUiState(
    val summary: LeakHunterSummary? = null,
    val isLoading: Boolean = true
)

class AnomalyGuardViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val expenseRepository: ExpenseRepository = DatabaseModule.expenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnomalyGuardUiState())
    val uiState: StateFlow<AnomalyGuardUiState> = _uiState.asStateFlow()

    init {
        runHeuristicScan()
    }

    fun runHeuristicScan() {
        viewModelScope.launch {
            val profile = profileRepository.getActiveProfile() ?: return@launch
            val expenses = expenseRepository.getAllExpensesFlow(profile.id).firstOrNull() ?: emptyList()

            val simpleList = if (expenses.isNotEmpty()) {
                expenses.map {
                    SimpleExpenseRecord(
                        id = it.expense.id,
                        amountMinorUnits = it.expense.amountMinorUnits,
                        title = it.expense.title ?: "Expense",
                        timestampMillis = it.expense.expenseDate,
                        categoryId = it.expense.categoryId
                    )
                }
            } else {
                // Seed mock scan transactions if empty to display realistic analytics
                listOf(
                    SimpleExpenseRecord("1", 45000L, "Starbucks Coffee", System.currentTimeMillis() - 100000),
                    SimpleExpenseRecord("2", 52000L, "Starbucks Coffee", System.currentTimeMillis() - 200000),
                    SimpleExpenseRecord("3", 48000L, "Starbucks Coffee", System.currentTimeMillis() - 300000),
                    SimpleExpenseRecord("4", 45000L, "Starbucks Coffee", System.currentTimeMillis() - 400000),
                    SimpleExpenseRecord("5", 55000L, "Starbucks Coffee", System.currentTimeMillis() - 500000),
                    SimpleExpenseRecord("6", 1250000L, "Unusual Electronics Store", System.currentTimeMillis() - 600000),
                    SimpleExpenseRecord("7", 12000L, "Quick Snack", System.currentTimeMillis() - 700000),
                    SimpleExpenseRecord("8", 15000L, "Quick Snack", System.currentTimeMillis() - 800000),
                    SimpleExpenseRecord("9", 14000L, "Quick Snack", System.currentTimeMillis() - 900000),
                    SimpleExpenseRecord("10", 11000L, "Quick Snack", System.currentTimeMillis() - 1000000)
                )
            }

            val summary = LeakHunterCalculation.analyze(simpleList)
            _uiState.update {
                it.copy(
                    summary = summary,
                    isLoading = false
                )
            }
        }
    }
}
