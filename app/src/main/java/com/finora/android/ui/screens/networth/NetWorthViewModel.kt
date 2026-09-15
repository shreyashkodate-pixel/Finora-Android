package com.finora.android.ui.screens.networth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.data.repository.AccountRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.data.repository.SavingsRepository
import com.finora.android.domain.model.AccountBalanceItem
import com.finora.android.domain.model.NetWorthCalculation
import com.finora.android.domain.model.NetWorthSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NetWorthUiState(
    val summary: NetWorthSummary? = null,
    val isLoading: Boolean = true
)

class NetWorthViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val accountRepository: AccountRepository = DatabaseModule.accountRepository,
    private val savingsRepository: SavingsRepository = DatabaseModule.savingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetWorthUiState())
    val uiState: StateFlow<NetWorthUiState> = _uiState.asStateFlow()

    init {
        loadNetWorthData()
    }

    fun loadNetWorthData() {
        viewModelScope.launch {
            val profile = profileRepository.getActiveProfile() ?: return@launch
            val accounts = accountRepository.getActiveAccountsFlow(profile.id).firstOrNull() ?: emptyList()
            val savingsGoals = savingsRepository.getGoalsFlow(profile.id).firstOrNull() ?: emptyList()

            val balanceItems = accounts.map {
                AccountBalanceItem(it.id, it.name, it.type, it.initialBalanceMinorUnits)
            }
            val savingsTotal = savingsGoals.sumOf { goal -> savingsRepository.getCurrentSavedAmount(goal.id) }

            val summary = NetWorthCalculation.calculate(balanceItems, savingsTotal)
            _uiState.update {
                it.copy(summary = summary, isLoading = false)
            }
        }
    }
}
