package com.finora.android.ui.screens.simulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.data.repository.BudgetRepository
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.domain.model.PurchaseSimulationResult
import com.finora.android.domain.model.PurchaseSimulatorCalculation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class PurchaseSimulatorUiState(
    val itemName: String = "Sony 65\" OLED TV",
    val purchaseAmountText: String = "45000",
    val simulationResult: PurchaseSimulationResult? = null,
    val remainingDaysInMonth: Int = 15
)

class PurchaseSimulatorViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val expenseRepository: ExpenseRepository = DatabaseModule.expenseRepository,
    private val budgetRepository: BudgetRepository = DatabaseModule.budgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseSimulatorUiState())
    val uiState: StateFlow<PurchaseSimulatorUiState> = _uiState.asStateFlow()

    private var currentRemainingBudget: Long = 6500000L // Default 65,000 INR
    private var totalBudget: Long = 10000000L // Default 1,00,000 INR
    private var upcomingRecurring: Long = 1200000L // 12,000 INR

    init {
        loadDataAndSimulate()
    }

    private fun loadDataAndSimulate() {
        viewModelScope.launch {
            val profile = profileRepository.getActiveProfile() ?: return@launch
            val cal = Calendar.getInstance()
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val daysRemaining = maxDays - dayOfMonth + 1

            cal.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val monthEnd = cal.timeInMillis - 1

            val spent = expenseRepository.getTotalSpendInDateRange(profile.id, monthStart, monthEnd)
            val yearMonth = String.format(java.util.Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            val overallBudget = budgetRepository.getOverallBudget(profile.id, yearMonth)
            val budgets = budgetRepository.getBudgetsForMonthFlow(profile.id, yearMonth).firstOrNull() ?: emptyList()
            val budgetTotal = overallBudget?.amountMinorUnits ?: budgets.filter { it.categoryId != null }.sumOf { it.amountMinorUnits }

            if (budgetTotal > 0) {
                totalBudget = budgetTotal
                currentRemainingBudget = kotlin.math.max(0L, budgetTotal - spent)
            }

            _uiState.update { it.copy(remainingDaysInMonth = daysRemaining) }
            runSimulation(_uiState.value.purchaseAmountText)
        }
    }

    fun onItemNameChanged(name: String) {
        _uiState.update { it.copy(itemName = name) }
    }

    fun onAmountChanged(rawAmount: String) {
        _uiState.update { it.copy(purchaseAmountText = rawAmount) }
        runSimulation(rawAmount)
    }

    fun addMicroAdjustment(amountDelta: Long) {
        val current = _uiState.value.purchaseAmountText.toLongOrNull() ?: 0L
        val next = kotlin.math.max(0L, current + amountDelta)
        onAmountChanged(next.toString())
    }

    private fun runSimulation(rawAmount: String) {
        val amount = (rawAmount.toDoubleOrNull() ?: 0.0) * 100
        val res = PurchaseSimulatorCalculation.simulate(
            purchaseAmountMinorUnits = amount.toLong(),
            currentRemainingBudgetMinorUnits = currentRemainingBudget,
            totalBudgetMinorUnits = totalBudget,
            upcomingRecurringMinorUnits = upcomingRecurring,
            remainingDaysInCycle = _uiState.value.remainingDaysInMonth,
            totalLiquidSavingsMinorUnits = 25000000L
        )
        _uiState.update { it.copy(simulationResult = res) }
    }
}
