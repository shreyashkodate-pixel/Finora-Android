package com.finora.android.ui.screens.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.Amount
import com.finora.android.core.model.AppCurrency
import com.finora.android.data.local.entity.AccountEntity
import com.finora.android.data.repository.AccountRepository
import com.finora.android.data.repository.IncomeRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.ui.components.KeypadAction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddIncomeUiState(
    val profileId: String = "",
    val currencySymbol: String = "₹",
    val amountInput: String = "0",
    val sources: List<String> = listOf("Salary", "Freelance", "Investment", "Gift", "Refund", "Other"),
    val selectedSource: String = "Salary",
    val accounts: List<AccountEntity> = emptyList(),
    val selectedAccount: AccountEntity? = null,
    val selectedDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isValid: Boolean
        get() {
            val amount = Amount.fromDecimalString(amountInput)
            return amount.minorUnits > 0L && selectedSource.isNotBlank()
        }
}

sealed interface AddIncomeEvent {
    data object IncomeSaved : AddIncomeEvent
    data class ShowError(val message: String) : AddIncomeEvent
}

class AddIncomeViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val incomeRepository: IncomeRepository = DatabaseModule.incomeRepository,
    private val accountRepository: AccountRepository = DatabaseModule.accountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddIncomeUiState())
    val uiState: StateFlow<AddIncomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddIncomeEvent>()
    val events: SharedFlow<AddIncomeEvent> = _events.asSharedFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val profile = profileRepository.getActiveProfile()
            if (profile != null) {
                val symbol = AppCurrency.fromCode(profile.currencyCode).symbol
                _uiState.update {
                    it.copy(
                        profileId = profile.id,
                        currencySymbol = symbol
                    )
                }

                accountRepository.getActiveAccountsFlow(profile.id).collect { accs ->
                    _uiState.update { state ->
                        state.copy(
                            accounts = accs,
                            selectedAccount = state.selectedAccount ?: accs.find { it.isDefault } ?: accs.firstOrNull()
                        )
                    }
                }
            }
        }
    }

    fun onKeypadAction(action: KeypadAction) {
        val current = _uiState.value.amountInput
        val updated = when (action) {
            is KeypadAction.Digit -> {
                if (current == "0") action.value.toString()
                else if (current.contains(".") && current.substringAfter(".").length >= 2) current
                else current + action.value
            }
            is KeypadAction.Decimal -> {
                if (!current.contains(".")) "$current." else current
            }
            is KeypadAction.Backspace -> {
                if (current.length <= 1) "0" else current.dropLast(1)
            }
        }
        _uiState.update { it.copy(amountInput = updated) }
    }

    fun selectSource(source: String) {
        _uiState.update { it.copy(selectedSource = source) }
    }

    fun selectAccount(account: AccountEntity?) {
        _uiState.update { it.copy(selectedAccount = account) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun updateDate(dateMillis: Long) {
        _uiState.update { it.copy(selectedDate = dateMillis) }
    }

    fun saveIncome() {
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val amount = Amount.fromDecimalString(state.amountInput)
                incomeRepository.createIncome(
                    profileId = state.profileId,
                    amount = amount,
                    currencyCode = state.currencySymbol,
                    source = state.selectedSource,
                    incomeDate = state.selectedDate,
                    accountId = state.selectedAccount?.id,
                    notes = state.notes
                )
                _events.emit(AddIncomeEvent.IncomeSaved)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(AddIncomeEvent.ShowError(e.message ?: "Failed to save income"))
            }
        }
    }
}
