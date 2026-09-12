package com.finora.android.core.model

/**
 * Supported currencies for Finora. Explicit currency tracking per SRS DC-8.
 */
enum class AppCurrency(
    val code: String,
    val symbol: String,
    val displayName: String
) {
    INR("INR", "₹", "Indian Rupee"),
    USD("USD", "$", "US Dollar"),
    EUR("EUR", "€", "Euro"),
    GBP("GBP", "£", "British Pound"),
    JPY("JPY", "¥", "Japanese Yen"),
    AUD("AUD", "A$", "Australian Dollar"),
    CAD("CAD", "C$", "Canadian Dollar");

    companion object {
        val DEFAULT = INR

        fun fromCode(code: String): AppCurrency {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: DEFAULT
        }
    }
}
