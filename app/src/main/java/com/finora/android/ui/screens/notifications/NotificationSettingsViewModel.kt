package com.finora.android.ui.screens.notifications

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NotificationSettingsUiState(
    val dailyCadenceEnabled: Boolean = true,
    val dailyCadenceTime: String = "21:00",
    val budgetPacingAlertsEnabled: Boolean = true,
    val billRemindersEnabled: Boolean = true,
    val billReminderDaysAhead: Int = 7,
    val weeklyDigestEnabled: Boolean = true,
    val zeroTelemetryVerified: Boolean = true
)

class NotificationSettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    fun toggleDailyCadence(enabled: Boolean) {
        _uiState.update { it.copy(dailyCadenceEnabled = enabled) }
    }

    fun setDailyCadenceTime(time: String) {
        _uiState.update { it.copy(dailyCadenceTime = time) }
    }

    fun toggleBudgetPacing(enabled: Boolean) {
        _uiState.update { it.copy(budgetPacingAlertsEnabled = enabled) }
    }

    fun toggleBillReminders(enabled: Boolean) {
        _uiState.update { it.copy(billRemindersEnabled = enabled) }
    }

    fun setBillReminderDays(days: Int) {
        _uiState.update { it.copy(billReminderDaysAhead = days) }
    }

    fun toggleWeeklyDigest(enabled: Boolean) {
        _uiState.update { it.copy(weeklyDigestEnabled = enabled) }
    }
}
