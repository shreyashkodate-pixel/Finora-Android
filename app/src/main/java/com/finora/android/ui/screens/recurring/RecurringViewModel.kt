package com.finora.android.ui.screens.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.Amount
import com.finora.android.core.model.AppCurrency
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
import com.finora.android.data.local.entity.RecurringExpenseEntity
import com.finora.android.data.repository.CategoryRepository
import com.finora.android.data.repository.PaymentMethodRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.data.repository.RecurringRepository
import com.finora.android.domain.model.RecurringCalculator
import com.finora.android.domain.model.RecurringCommitmentSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecurringUiState(
    val items: List<RecurringExpenseEntity> = emptyList(),
    val summary: RecurringCommitmentSummary = RecurringCommitmentSummary(Amount(0L), Amount(0L), 0, 0),
    val categories: List<CategoryEntity> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val currencySymbol: String = "₹",
    val isLoading: Boolean = true,
    val isAddDialogOpen: Boolean = false
)

class RecurringViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val recurringRepository: RecurringRepository = DatabaseModule.recurringRepository,
    private val categoryRepository: CategoryRepository = DatabaseModule.categoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository = DatabaseModule.paymentMethodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

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

            launch {
                categoryRepository.getCategoriesFlow(profile.id).collect { cats ->
                    _uiState.update { it.copy(categories = cats) }
                }
            }

            launch {
                paymentMethodRepository.getPaymentMethodsFlow(profile.id).collect { pms ->
                    _uiState.update { it.copy(paymentMethods = pms) }
                }
            }

            launch {
                recurringRepository.getAllRecurringFlow(profile.id).collect { list ->
                    val summary = RecurringCalculator.calculateSummary(list)
                    _uiState.update {
                        it.copy(
                            items = list,
                            summary = summary,
                            isLoading = false
                        )
                    }
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

    fun addRecurring(
        title: String,
        amount: Amount,
        categoryId: String,
        frequency: String
    ) {
        if (activeProfileId.isBlank() || title.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val nextDue = RecurringCalculator.computeNextDueDate(now, frequency)
            recurringRepository.createRecurring(
                profileId = activeProfileId,
                title = title,
                amount = amount,
                currencyCode = _uiState.value.currencySymbol,
                categoryId = categoryId,
                frequency = frequency,
                startDate = now,
                nextDueDate = nextDue
            )
            closeAddDialog()
        }
    }

    fun logOccurrence(item: RecurringExpenseEntity) {
        viewModelScope.launch {
            val defaultPm = _uiState.value.paymentMethods.find { it.isDefault }
                ?: _uiState.value.paymentMethods.firstOrNull()
            val pmId = defaultPm?.id ?: ""
            recurringRepository.logOccurrence(item, pmId)
        }
    }

    fun toggleActive(item: RecurringExpenseEntity) {
        viewModelScope.launch {
            recurringRepository.updateRecurring(item.copy(isActive = !item.isActive))
        }
    }

    fun deleteRecurring(item: RecurringExpenseEntity) {
        viewModelScope.launch {
            recurringRepository.deleteRecurring(item)
        }
    }
}
