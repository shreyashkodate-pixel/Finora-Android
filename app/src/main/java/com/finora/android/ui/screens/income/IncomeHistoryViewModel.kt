package com.finora.android.ui.screens.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.Amount
import com.finora.android.core.model.AppCurrency
import com.finora.android.data.local.entity.IncomeEntity
import com.finora.android.data.repository.IncomeRepository
import com.finora.android.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IncomeHistoryUiState(
    val incomeList: List<IncomeEntity> = emptyList(),
    val totalIncome: Amount = Amount(0L),
    val currencySymbol: String = "₹",
    val isLoading: Boolean = true
)

class IncomeHistoryViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val incomeRepository: IncomeRepository = DatabaseModule.incomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncomeHistoryUiState())
    val uiState: StateFlow<IncomeHistoryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val profile = profileRepository.getActiveProfile() ?: return@launch
            val symbol = AppCurrency.fromCode(profile.currencyCode).symbol
            _uiState.update { it.copy(currencySymbol = symbol) }

            incomeRepository.getIncomeForProfileFlow(profile.id).collect { list ->
                val totalMinor = list.sumOf { it.amountMinorUnits }
                _uiState.update {
                    it.copy(
                        incomeList = list,
                        totalIncome = Amount(totalMinor),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun deleteIncome(income: IncomeEntity) {
        viewModelScope.launch {
            incomeRepository.deleteIncome(income)
        }
    }
}
