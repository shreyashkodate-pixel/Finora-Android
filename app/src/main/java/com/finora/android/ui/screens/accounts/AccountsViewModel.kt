package com.finora.android.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.Amount
import com.finora.android.core.model.AppCurrency
import com.finora.android.data.local.entity.AccountEntity
import com.finora.android.data.repository.AccountRepository
import com.finora.android.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountsUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val totalBalance: Amount = Amount(0L),
    val currencySymbol: String = "₹",
    val isLoading: Boolean = true,
    val isAddDialogOpen: Boolean = false
)

class AccountsViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val accountRepository: AccountRepository = DatabaseModule.accountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

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

            accountRepository.getActiveAccountsFlow(profile.id).collect { list ->
                val totalMinor = list.sumOf { it.initialBalanceMinorUnits }
                _uiState.update {
                    it.copy(
                        accounts = list,
                        totalBalance = Amount(totalMinor),
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

    fun addAccount(name: String, type: String, initialBalance: Amount, isDefault: Boolean) {
        if (activeProfileId.isBlank() || name.isBlank()) return
        viewModelScope.launch {
            accountRepository.createAccount(
                profileId = activeProfileId,
                name = name,
                type = type,
                initialBalance = initialBalance,
                isDefault = isDefault
            )
            closeAddDialog()
        }
    }

    fun setDefaultAccount(accountId: String) {
        if (activeProfileId.isBlank()) return
        viewModelScope.launch {
            accountRepository.setDefaultAccount(accountId, activeProfileId)
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            accountRepository.deleteAccount(account)
        }
    }
}
