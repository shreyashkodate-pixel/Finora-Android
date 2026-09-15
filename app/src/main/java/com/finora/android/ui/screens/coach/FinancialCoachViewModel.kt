package com.finora.android.ui.screens.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.data.repository.BudgetRepository
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.IncomeRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.domain.model.CoachMessage
import com.finora.android.domain.model.FinancialCoachEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class FinancialCoachUiState(
    val messages: List<CoachMessage> = emptyList(),
    val isEngineRunning: Boolean = true,
    val activeQuery: String = "",
    val presetPrompts: List<String> = listOf(
        "What is my 50/30/20 breakdown?",
        "How much on food this month?",
        "What is my safe-to-spend today?",
        "Where can I cut ₹2,000?"
    )
)

class FinancialCoachViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val expenseRepository: ExpenseRepository = DatabaseModule.expenseRepository,
    private val incomeRepository: IncomeRepository = DatabaseModule.incomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialCoachUiState())
    val uiState: StateFlow<FinancialCoachUiState> = _uiState.asStateFlow()

    private var activeProfileId: String = ""
    private var totalIncome: Long = 0L
    private var totalExpense: Long = 0L
    private var categoryMap: Map<String, Long> = emptyMap()

    init {
        loadDataAndWelcome()
    }

    private fun loadDataAndWelcome() {
        viewModelScope.launch {
            val profile = profileRepository.getActiveProfile() ?: return@launch
            activeProfileId = profile.id

            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val monthEnd = cal.timeInMillis - 1

            totalExpense = expenseRepository.getTotalSpendInDateRange(profile.id, monthStart, monthEnd)
            totalIncome = incomeRepository.getTotalIncomeBetweenDates(profile.id, monthStart, monthEnd)

            val breakdown = expenseRepository.getCategorySpendBreakdownFlow(profile.id, monthStart, monthEnd).firstOrNull() ?: emptyList()
            categoryMap = breakdown.associate { it.categoryName to it.totalAmount }

            val initialResponse = FinancialCoachEngine.generateResponse(
                userQuery = "overview",
                totalIncomeMinorUnits = totalIncome,
                totalExpenseMinorUnits = totalExpense,
                categoryExpenses = categoryMap,
                safeToSpendTodayMinorUnits = 185000L
            )

            _uiState.update {
                it.copy(messages = listOf(initialResponse))
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(activeQuery = query) }
    }

    fun sendQuery(queryText: String? = null) {
        val query = queryText ?: _uiState.value.activeQuery
        if (query.isBlank()) return

        val userMsg = CoachMessage(
            id = "user_${System.currentTimeMillis()}",
            isUser = true,
            text = query,
            timestampMillis = System.currentTimeMillis()
        )

        val assistantMsg = FinancialCoachEngine.generateResponse(
            userQuery = query,
            totalIncomeMinorUnits = totalIncome,
            totalExpenseMinorUnits = totalExpense,
            categoryExpenses = categoryMap,
            safeToSpendTodayMinorUnits = 185000L
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMsg + assistantMsg,
                activeQuery = ""
            )
        }
    }
}
