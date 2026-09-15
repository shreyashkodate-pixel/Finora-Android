package com.finora.android.ui.screens.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.domain.model.AuditVerificationResult
import com.finora.android.domain.model.MerkleLedgerAudit
import com.finora.android.domain.model.SimpleExpenseRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MerkleAuditUiState(
    val auditResult: AuditVerificationResult? = null,
    val isVerifying: Boolean = false
)

class MerkleAuditViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val expenseRepository: ExpenseRepository = DatabaseModule.expenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MerkleAuditUiState())
    val uiState: StateFlow<MerkleAuditUiState> = _uiState.asStateFlow()

    init {
        runAuditVerification()
    }

    fun runAuditVerification() {
        _uiState.update { it.copy(isVerifying = true) }
        viewModelScope.launch {
            val profile = profileRepository.getActiveProfile() ?: return@launch
            val expenses = expenseRepository.getAllExpensesFlow(profile.id).firstOrNull() ?: emptyList()

            val simpleRecords = if (expenses.isNotEmpty()) {
                expenses.map {
                    SimpleExpenseRecord(
                        id = it.expense.id,
                        amountMinorUnits = it.expense.amountMinorUnits,
                        title = it.expense.title ?: "Transaction",
                        timestampMillis = it.expense.expenseDate
                    )
                }
            } else {
                // Seed mock ledger blocks if user has zero expenses
                listOf(
                    SimpleExpenseRecord("tx1", 45000L, "Coffee & Bagel", 1726000000000L),
                    SimpleExpenseRecord("tx2", 32000L, "Metro Transit Pass", 1726086400000L),
                    SimpleExpenseRecord("tx3", 85000L, "Groceries Supermarket", 1726172800000L),
                    SimpleExpenseRecord("tx4", 120000L, "Electricity Utility Bill", 1726259200000L)
                )
            }

            val result = MerkleLedgerAudit.buildAndVerifyLedger(simpleRecords)
            _uiState.update {
                it.copy(
                    auditResult = result,
                    isVerifying = false
                )
            }
        }
    }
}
