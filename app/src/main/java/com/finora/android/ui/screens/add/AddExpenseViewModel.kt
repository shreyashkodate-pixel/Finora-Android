package com.finora.android.ui.screens.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.Amount
import com.finora.android.core.model.AppCurrency
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
import com.finora.android.data.repository.CategoryRepository
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.PaymentMethodRepository
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

data class AddExpenseUiState(
    val profileId: String = "",
    val currencySymbol: String = "₹",
    val amountInput: String = "0",
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategory: CategoryEntity? = null,
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val selectedPaymentMethod: PaymentMethodEntity? = null,
    val selectedDate: Long = System.currentTimeMillis(),
    val title: String = "",
    val notes: String = "",
    val isExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isValid: Boolean
        get() {
            val amount = Amount.fromDecimalString(amountInput)
            return amount.minorUnits > 0L && selectedCategory != null
        }
}

sealed interface AddExpenseEvent {
    data object ExpenseSaved : AddExpenseEvent
    data class ShowError(val message: String) : AddExpenseEvent
}

class AddExpenseViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val categoryRepository: CategoryRepository = DatabaseModule.categoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository = DatabaseModule.paymentMethodRepository,
    private val expenseRepository: ExpenseRepository = DatabaseModule.expenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddExpenseEvent>()
    val events: SharedFlow<AddExpenseEvent> = _events.asSharedFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val profile = profileRepository.getActiveProfile()
                ?: profileRepository.createProfile(
                    name = "Personal",
                    currencyCode = AppCurrency.DEFAULT.code
                )

            val currency = AppCurrency.fromCode(profile.currencyCode)
            _uiState.update {
                it.copy(
                    profileId = profile.id,
                    currencySymbol = currency.symbol
                )
            }

            // Observe categories
            launch {
                categoryRepository.getCategoriesFlow(profile.id).collect { categories ->
                    _uiState.update { state ->
                        val selected = state.selectedCategory
                            ?: categories.firstOrNull()
                        state.copy(categories = categories, selectedCategory = selected)
                    }
                }
            }

            // Observe payment methods
            launch {
                paymentMethodRepository.getPaymentMethodsFlow(profile.id).collect { paymentMethods ->
                    _uiState.update { state ->
                        val selected = state.selectedPaymentMethod
                            ?: paymentMethods.find { it.isDefault } ?: paymentMethods.firstOrNull()
                        state.copy(paymentMethods = paymentMethods, selectedPaymentMethod = selected)
                    }
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onKeypadAction(action: KeypadAction) {
        val current = _uiState.value.amountInput
        val updated = when (action) {
            is KeypadAction.Backspace -> {
                if (current.length <= 1) "0" else current.dropLast(1)
            }
            is KeypadAction.Decimal -> {
                if (!current.contains(".")) {
                    if (current == "0" || current.isEmpty()) "0." else "$current."
                } else {
                    current
                }
            }
            is KeypadAction.Digit -> {
                val digit = action.value
                val parts = current.split(".")
                if (parts.size > 1 && parts[1].length >= 2) {
                    current // Max 2 decimal digits
                } else if (current == "0") {
                    digit.toString()
                } else if (current.length < 9) {
                    current + digit
                } else {
                    current
                }
            }
        }
        _uiState.update { it.copy(amountInput = updated, errorMessage = null) }
    }

    fun onSelectCategory(category: CategoryEntity) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onSelectPaymentMethod(paymentMethod: PaymentMethodEntity?) {
        _uiState.update { it.copy(selectedPaymentMethod = paymentMethod) }
    }

    fun onSelectDate(dateMillis: Long) {
        _uiState.update { it.copy(selectedDate = dateMillis) }
    }

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun onToggleExpanded() {
        _uiState.update { it.copy(isExpanded = !it.isExpanded) }
    }

    fun saveExpense() {
        val state = _uiState.value
        val amount = Amount.fromDecimalString(state.amountInput)

        if (amount.minorUnits <= 0L) {
            _uiState.update { it.copy(errorMessage = "Please enter an amount greater than zero.") }
            return
        }

        val category = state.selectedCategory
        if (category == null) {
            _uiState.update { it.copy(errorMessage = "Please select a category.") }
            return
        }

        viewModelScope.launch {
            try {
                expenseRepository.createExpense(
                    profileId = state.profileId,
                    amount = amount,
                    currencyCode = AppCurrency.fromCode(state.currencySymbol).code,
                    categoryId = category.id,
                    expenseDate = state.selectedDate,
                    paymentMethodId = state.selectedPaymentMethod?.id,
                    title = state.title.ifBlank { null },
                    notes = state.notes.ifBlank { null },
                    source = "MANUAL"
                )
                _events.emit(AddExpenseEvent.ExpenseSaved)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to save expense") }
                _events.emit(AddExpenseEvent.ShowError(e.message ?: "Failed to save expense"))
            }
        }
    }
}
