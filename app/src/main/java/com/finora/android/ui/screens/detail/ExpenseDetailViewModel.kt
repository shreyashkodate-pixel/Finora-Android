package com.finora.android.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.AppCurrency
import com.finora.android.data.local.relation.ExpenseWithDetails
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpenseDetailUiState(
    val expenseDetails: ExpenseWithDetails? = null,
    val currencySymbol: String = "₹",
    val isLoading: Boolean = true,
    val showDeleteDialog: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ExpenseDetailEvent {
    data object ExpenseDeleted : ExpenseDetailEvent
    data class ShowError(val message: String) : ExpenseDetailEvent
}

class ExpenseDetailViewModel(
    private val expenseId: String,
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val expenseRepository: ExpenseRepository = DatabaseModule.expenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseDetailUiState())
    val uiState: StateFlow<ExpenseDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExpenseDetailEvent>()
    val events: SharedFlow<ExpenseDetailEvent> = _events.asSharedFlow()

    private var profileId: String = ""

    init {
        loadExpense()
    }

    private fun loadExpense() {
        viewModelScope.launch {
            val profile = profileRepository.getActiveProfile()
            if (profile != null) {
                profileId = profile.id
                val currency = AppCurrency.fromCode(profile.currencyCode)
                _uiState.update { it.copy(currencySymbol = currency.symbol) }

                expenseRepository.getExpenseByIdFlow(expenseId, profile.id).collect { details ->
                    _uiState.update {
                        it.copy(
                            expenseDetails = details,
                            isLoading = false
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Profile not found") }
            }
        }
    }

    fun showDeleteDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteDialog = show) }
    }

    fun deleteExpense() {
        viewModelScope.launch {
            try {
                expenseRepository.deleteExpenseById(expenseId, profileId)
                _events.emit(ExpenseDetailEvent.ExpenseDeleted)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
                _events.emit(ExpenseDetailEvent.ShowError(e.message ?: "Failed to delete expense"))
            }
        }
    }
}
