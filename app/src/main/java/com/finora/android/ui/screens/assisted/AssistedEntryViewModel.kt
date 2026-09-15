package com.finora.android.ui.screens.assisted

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.Amount
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.repository.CategoryRepository
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.domain.model.DuplicateDetection
import com.finora.android.domain.model.DuplicateMatch
import com.finora.android.domain.model.NaturalLanguageExpenseParser
import com.finora.android.domain.model.ParsedExpenseDraft
import com.finora.android.domain.model.SimpleExpenseRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssistedEntryUiState(
    val rawInputText: String = "",
    val parsedDraft: ParsedExpenseDraft? = null,
    val isListeningVoice: Boolean = false,
    val duplicateWarning: DuplicateMatch? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: String? = null,
    val isSavedSuccess: Boolean = false,
    val receiptMerchantConfidence: Int = 88,
    val receiptAmountConfidence: Int = 98,
    val receiptImageUri: String? = null
)

class AssistedEntryViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val expenseRepository: ExpenseRepository = DatabaseModule.expenseRepository,
    private val categoryRepository: CategoryRepository = DatabaseModule.categoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistedEntryUiState())
    val uiState: StateFlow<AssistedEntryUiState> = _uiState.asStateFlow()

    private var activeProfileId: String = ""
    private var currencyCode: String = "INR"

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val profile = profileRepository.getActiveProfile() ?: return@launch
            activeProfileId = profile.id
            currencyCode = profile.currencyCode

            categoryRepository.getCategoriesFlow(profile.id).collect { cats ->
                _uiState.update {
                    it.copy(
                        categories = cats,
                        selectedCategoryId = it.selectedCategoryId ?: cats.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        val draft = NaturalLanguageExpenseParser.parse(text)
        val matchedCategory = _uiState.value.categories.find {
            it.name.contains(draft.categorySuggestion, ignoreCase = true)
        } ?: _uiState.value.categories.firstOrNull()

        _uiState.update {
            it.copy(
                rawInputText = text,
                parsedDraft = draft,
                selectedCategoryId = matchedCategory?.id
            )
        }
        checkForDuplicates(draft.amountMinorUnits, draft.merchantOrTitle, draft.timestampMillis)
    }

    fun setDraftManually(
        merchant: String,
        amountMinorUnits: Long,
        categoryName: String,
        notes: String = ""
    ) {
        val matchedCategory = _uiState.value.categories.find {
            it.name.contains(categoryName, ignoreCase = true)
        } ?: _uiState.value.categories.firstOrNull()

        val draft = ParsedExpenseDraft(
            rawInput = "$merchant ${amountMinorUnits / 100}",
            amountMinorUnits = amountMinorUnits,
            merchantOrTitle = merchant,
            categorySuggestion = categoryName,
            paymentMethodSuggestion = "Default Account",
            timestampMillis = System.currentTimeMillis(),
            notes = notes
        )

        _uiState.update {
            it.copy(
                parsedDraft = draft,
                selectedCategoryId = matchedCategory?.id
            )
        }
        checkForDuplicates(amountMinorUnits, merchant, System.currentTimeMillis())
    }

    fun toggleVoiceListening() {
        val currentlyListening = _uiState.value.isListeningVoice
        if (!currentlyListening) {
            _uiState.update { it.copy(isListeningVoice = true) }
        } else {
            // Simulated speech-to-text recognition result
            val sampleSpoken = "Coffee 220 upi with team"
            _uiState.update { it.copy(isListeningVoice = false) }
            onInputTextChanged(sampleSpoken)
        }
    }

    fun selectCategory(categoryId: String) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    private fun checkForDuplicates(amountMinor: Long, title: String, timestamp: Long) {
        if (amountMinor <= 0 || activeProfileId.isBlank()) return
        viewModelScope.launch {
            val recents = expenseRepository.getAllExpensesFlow(activeProfileId).firstOrNull() ?: emptyList()
            val simpleRecords = recents.map {
                SimpleExpenseRecord(it.expense.id, it.expense.amountMinorUnits, it.expense.title ?: "", it.expense.expenseDate)
            }
            val match = DuplicateDetection.findDuplicate(amountMinor, title, timestamp, simpleRecords)
            _uiState.update { it.copy(duplicateWarning = match) }
        }
    }

    fun confirmAndSave(sourceType: String = "ASSISTED_NLP", onSaved: () -> Unit) {
        val draft = _uiState.value.parsedDraft ?: return
        val categoryId = _uiState.value.selectedCategoryId ?: return
        if (activeProfileId.isBlank() || draft.amountMinorUnits <= 0) return

        viewModelScope.launch {
            expenseRepository.createExpense(
                profileId = activeProfileId,
                amount = Amount(draft.amountMinorUnits),
                currencyCode = currencyCode,
                categoryId = categoryId,
                expenseDate = draft.timestampMillis,
                title = draft.merchantOrTitle.ifBlank { "Expense" },
                notes = draft.notes,
                source = sourceType
            )
            _uiState.update { it.copy(isSavedSuccess = true) }
            onSaved()
        }
    }
}
