package com.finora.android.ui.screens.statement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.Amount
import com.finora.android.data.repository.AccountRepository
import com.finora.android.data.repository.CategoryRepository
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.domain.model.CsvStatementParser
import com.finora.android.domain.model.SimpleExpenseRecord
import com.finora.android.domain.model.StatementParseResult
import com.finora.android.domain.model.StatementRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatementUiState(
    val parseResult: StatementParseResult? = null,
    val selectedAccountId: String? = null,
    val accountName: String = "Primary Bank Account",
    val isCommittedSuccess: Boolean = false,
    val isLoading: Boolean = false
)

class StatementViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val expenseRepository: ExpenseRepository = DatabaseModule.expenseRepository,
    private val categoryRepository: CategoryRepository = DatabaseModule.categoryRepository,
    private val accountRepository: AccountRepository = DatabaseModule.accountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatementUiState())
    val uiState: StateFlow<StatementUiState> = _uiState.asStateFlow()

    private var activeProfileId: String = ""

    init {
        loadDefaultStatement()
    }

    private fun loadDefaultStatement() {
        viewModelScope.launch {
            val profile = profileRepository.getActiveProfile() ?: return@launch
            activeProfileId = profile.id

            val accounts = accountRepository.getActiveAccountsFlow(profile.id).firstOrNull() ?: emptyList()
            val defaultAcc = accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
            _uiState.update {
                it.copy(
                    selectedAccountId = defaultAcc?.id,
                    accountName = defaultAcc?.name ?: "Primary Bank Account"
                )
            }

            // Seed a realistic bank statement for parsing preview
            val sampleCsv = """
                Date,Description,Amount
                2026-09-02,Swiggy Bangalore IN,420.00
                2026-09-03,Uber India Mumbai,320.00
                2026-09-05,Blinkit Commerce Retail,890.00
                2026-09-08,Amazon India Marketplace,1450.00
                2026-09-10,Netflix Monthly Subscription,649.00
            """.trimIndent()

            val existing = expenseRepository.getAllExpensesFlow(profile.id).firstOrNull() ?: emptyList()
            val simpleRecords = existing.map {
                SimpleExpenseRecord(it.expense.id, it.expense.amountMinorUnits, it.expense.title ?: "", it.expense.expenseDate)
            }

            val parsed = CsvStatementParser.parse("HDFC_Sep2026_Statement.csv", sampleCsv, simpleRecords)
            _uiState.update { it.copy(parseResult = parsed) }
        }
    }

    fun toggleRowAccepted(rowIndex: Int) {
        val current = _uiState.value.parseResult ?: return
        val updatedRows = current.validRows.map {
            if (it.rowIndex == rowIndex) it.copy(isAccepted = !it.isAccepted) else it
        }
        _uiState.update {
            it.copy(parseResult = current.copy(validRows = updatedRows))
        }
    }

    fun commitReconciliation(onFinished: () -> Unit) {
        val result = _uiState.value.parseResult ?: return
        if (activeProfileId.isBlank()) return

        viewModelScope.launch {
            val categories = categoryRepository.getCategories(activeProfileId)
            val defaultCategory = categories.firstOrNull()?.id ?: return@launch

            val acceptedRows = result.validRows.filter { it.isAccepted }
            acceptedRows.forEach { row ->
                val matchedCat = categories.find { it.name.contains(row.categorySuggestion, ignoreCase = true) }?.id ?: defaultCategory
                expenseRepository.createExpense(
                    profileId = activeProfileId,
                    amount = Amount(row.amountMinorUnits),
                    currencyCode = "INR",
                    categoryId = matchedCat,
                    expenseDate = System.currentTimeMillis(),
                    title = row.description,
                    notes = "Reconciled from CSV statement: ${result.fileName}",
                    source = "CSV_IMPORT"
                )
            }
            _uiState.update { it.copy(isCommittedSuccess = true) }
            onFinished()
        }
    }
}
