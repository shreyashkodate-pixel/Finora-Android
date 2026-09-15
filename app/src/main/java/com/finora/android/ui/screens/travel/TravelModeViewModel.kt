package com.finora.android.ui.screens.travel

import androidx.lifecycle.ViewModel
import com.finora.android.domain.model.CurrencyEngine
import com.finora.android.domain.model.ExchangeRate
import com.finora.android.domain.model.TravelLedgerSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TravelModeUiState(
    val isTravelModeActive: Boolean = true,
    val tripDestination: String = "Tokyo & Kyoto, Japan",
    val tripCurrencyCode: String = "JPY",
    val homeCurrencyCode: String = "INR",
    val spotForeignAmount: String = "15000",
    val convertedHomeAmountMinorUnits: Long = 825000L, // 15,000 JPY * 0.55 = 8,250 INR
    val rates: List<ExchangeRate> = CurrencyEngine.getRatesList(),
    val tripSummary: TravelLedgerSummary = TravelLedgerSummary(
        destination = "Tokyo & Kyoto, Japan",
        tripCurrencyCode = "JPY",
        homeCurrencyCode = "INR",
        totalSpentInTripCurrencyMinorUnits = 14200000L, // 142,000 JPY
        totalSpentInHomeCurrencyMinorUnits = 7810000L,  // 78,100 INR
        activeTripDay = 6,
        totalTripDays = 14,
        exchangeRateUsed = 0.55
    )
)

class TravelModeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TravelModeUiState())
    val uiState: StateFlow<TravelModeUiState> = _uiState.asStateFlow()

    fun toggleTravelMode(active: Boolean) {
        _uiState.update { it.copy(isTravelModeActive = active) }
    }

    fun onForeignAmountChanged(raw: String) {
        val num = raw.toDoubleOrNull() ?: 0.0
        val inForeignMinor = (num * 100).toLong()
        val inrMinor = CurrencyEngine.convert(inForeignMinor, _uiState.value.tripCurrencyCode, "INR")
        _uiState.update {
            it.copy(
                spotForeignAmount = raw,
                convertedHomeAmountMinorUnits = inrMinor
            )
        }
    }

    fun selectTripCurrency(code: String) {
        _uiState.update {
            val inrMinor = CurrencyEngine.convert(
                ((it.spotForeignAmount.toDoubleOrNull() ?: 0.0) * 100).toLong(),
                code,
                "INR"
            )
            it.copy(
                tripCurrencyCode = code,
                convertedHomeAmountMinorUnits = inrMinor
            )
        }
    }
}
