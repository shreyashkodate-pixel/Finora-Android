package com.finora.android.domain.model

data class ExchangeRate(
    val fromCurrency: String,
    val toCurrency: String,
    val rateToInr: Double, // Multiplier to convert 1 unit of fromCurrency to INR
    val lastUpdatedText: String
)

data class TravelLedgerSummary(
    val destination: String,
    val tripCurrencyCode: String,
    val homeCurrencyCode: String,
    val totalSpentInTripCurrencyMinorUnits: Long,
    val totalSpentInHomeCurrencyMinorUnits: Long,
    val activeTripDay: Int,
    val totalTripDays: Int,
    val exchangeRateUsed: Double
)

object CurrencyEngine {

    // Offline cached fixed exchange rate table relative to INR
    private val RATE_TABLE_TO_INR = mapOf(
        "INR" to 1.0,
        "USD" to 83.50,
        "EUR" to 91.20,
        "GBP" to 106.80,
        "JPY" to 0.55
    )

    fun getSupportedCurrencies(): List<String> = listOf("INR", "USD", "EUR", "GBP", "JPY")

    fun getRateToInr(currencyCode: String): Double {
        return RATE_TABLE_TO_INR[currencyCode.uppercase()] ?: 1.0
    }

    /**
     * Converts an amount from source currency to target currency preserving minor units.
     */
    fun convert(
        amountMinorUnits: Long,
        fromCurrency: String,
        toCurrency: String
    ): Long {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amountMinorUnits

        val fromRate = getRateToInr(fromCurrency)
        val toRate = getRateToInr(toCurrency)

        // Value in INR minor units
        val inrMinorUnits = amountMinorUnits * fromRate
        // Value in target minor units
        return (inrMinorUnits / toRate).toLong()
    }

    fun getRatesList(): List<ExchangeRate> {
        return listOf(
            ExchangeRate("USD", "INR", 83.50, "Offline Fixed Cache"),
            ExchangeRate("EUR", "INR", 91.20, "Offline Fixed Cache"),
            ExchangeRate("GBP", "INR", 106.80, "Offline Fixed Cache"),
            ExchangeRate("JPY", "INR", 0.55, "Offline Fixed Cache")
        )
    }
}
