package com.finora.android.ui.screens.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.Amount
import com.finora.android.core.model.AppCurrency
import com.finora.android.data.local.entity.SavingsGoalEntity
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.data.repository.SavingsRepository
import com.finora.android.domain.model.SavingsCalculator
import com.finora.android.domain.model.SavingsGoalSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavingsUiState(
    val goalSummaries: List<SavingsGoalSummary> = emptyList(),
    val totalSavedAmount: Amount = Amount(0L),
    val currencySymbol: String = "₹",
    val isLoading: Boolean = true,
    val isAddDialogOpen: Boolean = false,
    val selectedGoalForContribution: SavingsGoalEntity? = null
)

class SavingsViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val savingsRepository: SavingsRepository = DatabaseModule.savingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavingsUiState())
    val uiState: StateFlow<SavingsUiState> = _uiState.asStateFlow()

    private var activeProfileId: String = ""

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val profile = profileRepository.getActiveProfile() ?: return@launch
            activeProfileId = profile.id
            val symbol = AppCurrency.fromCode(profile.currencyCode).symbol
            _uiState.update { it.copy(currencySymbol = symbol) }

            savingsRepository.getGoalsFlow(profile.id).collect { goals ->
                val summaries = goals.map { goal ->
                    val currentSaved = savingsRepository.getCurrentSavedAmount(goal.id)
                    SavingsCalculator.calculateGoalSummary(goal, currentSaved)
                }
                val totalSaved = summaries.sumOf { it.currentSavedAmount.minorUnits }
                _uiState.update {
                    it.copy(
                        goalSummaries = summaries,
                        totalSavedAmount = Amount(totalSaved),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun openAddDialog() {
        _uiState.update { it.copy(isAddDialogOpen = true) }
    }

    fun closeAddDialog() {
        _uiState.update { it.copy(isAddDialogOpen = false) }
    }

    fun openContributionSheet(goal: SavingsGoalEntity) {
        _uiState.update { it.copy(selectedGoalForContribution = goal) }
    }

    fun closeContributionSheet() {
        _uiState.update { it.copy(selectedGoalForContribution = null) }
    }

    fun addGoal(
        name: String,
        targetAmount: Amount,
        templateType: String,
        targetDate: Long?
    ) {
        if (activeProfileId.isBlank() || name.isBlank()) return
        viewModelScope.launch {
            savingsRepository.createGoal(
                profileId = activeProfileId,
                name = name,
                targetAmount = targetAmount,
                currencyCode = _uiState.value.currencySymbol,
                targetDate = targetDate,
                templateType = templateType
            )
            closeAddDialog()
        }
    }

    fun addContribution(amount: Amount, type: String, notes: String?) {
        val goal = _uiState.value.selectedGoalForContribution ?: return
        viewModelScope.launch {
            savingsRepository.addContribution(
                goalId = goal.id,
                profileId = activeProfileId,
                amount = amount,
                type = type,
                notes = notes
            )
            closeContributionSheet()
        }
    }

    fun deleteGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            savingsRepository.deleteGoal(goal)
        }
    }
}
