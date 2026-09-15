package com.finora.android.domain.model

data class ParsedExpenseDraft(
    val rawInput: String,
    val amountMinorUnits: Long,
    val merchantOrTitle: String,
    val categorySuggestion: String,
    val paymentMethodSuggestion: String,
    val timestampMillis: Long,
    val notes: String
)

object NaturalLanguageExpenseParser {

    private val AMOUNT_REGEX = Regex("""(?i)(?:₹|rs\.?\s*|inr\s*)?(\d+(?:\.\d{1,2})?)(?:k\b|₹)?""")
    private val YESTERDAY_MILLIS = 24 * 60 * 60 * 1000L

    fun parse(input: String, currentTimeMillis: Long = System.currentTimeMillis()): ParsedExpenseDraft {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParsedExpenseDraft(
                rawInput = input,
                amountMinorUnits = 0L,
                merchantOrTitle = "",
                categorySuggestion = "General",
                paymentMethodSuggestion = "Cash",
                timestampMillis = currentTimeMillis,
                notes = ""
            )
        }

        val tokens = trimmed.split(Regex("""\s+"""))
        var detectedAmountMinorUnits = 0L
        var amountToken = ""

        // 1. Detect Amount
        for (token in tokens) {
            val cleanToken = token.replace(",", "").replace("₹", "").replace("rs.", "", ignoreCase = true)
            if (cleanToken.endsWith("k", ignoreCase = true)) {
                val numPart = cleanToken.dropLast(1).toDoubleOrNull()
                if (numPart != null && numPart > 0) {
                    detectedAmountMinorUnits = (numPart * 1000 * 100).toLong()
                    amountToken = token
                    break
                }
            } else {
                val numPart = cleanToken.toDoubleOrNull()
                if (numPart != null && numPart > 0) {
                    detectedAmountMinorUnits = (numPart * 100).toLong()
                    amountToken = token
                    break
                }
            }
        }

        // 2. Detect Payment Method
        val lowerInput = trimmed.lowercase()
        val detectedPayment = when {
            lowerInput.contains("upi") || lowerInput.contains("gpay") || lowerInput.contains("phonepe") -> "UPI"
            lowerInput.contains("card") || lowerInput.contains("credit") || lowerInput.contains("debit") -> "Credit Card"
            lowerInput.contains("cash") -> "Cash"
            lowerInput.contains("bank") || lowerInput.contains("neft") || lowerInput.contains("imps") -> "Bank Account"
            else -> "Default Account"
        }

        // 3. Detect Category
        val detectedCategory = when {
            lowerInput.contains("swiggy") || lowerInput.contains("zomato") || lowerInput.contains("dinner") ||
                    lowerInput.contains("lunch") || lowerInput.contains("food") || lowerInput.contains("coffee") ||
                    lowerInput.contains("restaurant") || lowerInput.contains("cafe") -> "Food & Dining"

            lowerInput.contains("uber") || lowerInput.contains("ola") || lowerInput.contains("metro") ||
                    lowerInput.contains("fuel") || lowerInput.contains("petrol") || lowerInput.contains("cab") ||
                    lowerInput.contains("taxi") || lowerInput.contains("train") || lowerInput.contains("flight") -> "Transportation"

            lowerInput.contains("groceries") || lowerInput.contains("blinkit") || lowerInput.contains("zepto") ||
                    lowerInput.contains("supermarket") || lowerInput.contains("milk") || lowerInput.contains("vegetables") -> "Groceries"

            lowerInput.contains("amazon") || lowerInput.contains("flipkart") || lowerInput.contains("myntra") ||
                    lowerInput.contains("clothes") || lowerInput.contains("shopping") -> "Shopping"

            lowerInput.contains("electricity") || lowerInput.contains("wifi") || lowerInput.contains("broadband") ||
                    lowerInput.contains("water") || lowerInput.contains("rent") || lowerInput.contains("bill") -> "Bills & Utilities"

            lowerInput.contains("movie") || lowerInput.contains("cinema") || lowerInput.contains("netflix") ||
                    lowerInput.contains("spotify") || lowerInput.contains("game") -> "Entertainment"

            lowerInput.contains("doctor") || lowerInput.contains("pharmacy") || lowerInput.contains("medicine") ||
                    lowerInput.contains("hospital") || lowerInput.contains("clinic") -> "Health"

            else -> "General"
        }

        // 4. Detect Date
        val timestamp = when {
            lowerInput.contains("yesterday") -> currentTimeMillis - YESTERDAY_MILLIS
            else -> currentTimeMillis
        }

        // 5. Detect Merchant / Title (The first significant word that isn't amount/payment/date)
        val filteredTokens = tokens.filterNot {
            it.equals(amountToken, ignoreCase = true) ||
                    it.equals("upi", ignoreCase = true) ||
                    it.equals("card", ignoreCase = true) ||
                    it.equals("cash", ignoreCase = true) ||
                    it.equals("yesterday", ignoreCase = true) ||
                    it.equals("today", ignoreCase = true) ||
                    it.equals("to", ignoreCase = true) ||
                    it.equals("for", ignoreCase = true) ||
                    it.equals("at", ignoreCase = true)
        }

        val title = if (filteredTokens.isNotEmpty()) {
            filteredTokens.first().replaceFirstChar { it.uppercase() }
        } else {
            detectedCategory
        }

        // 6. Notes: Remaining tokens or tags
        val notes = filteredTokens.drop(1).joinToString(" ")

        return ParsedExpenseDraft(
            rawInput = input,
            amountMinorUnits = detectedAmountMinorUnits,
            merchantOrTitle = title,
            categorySuggestion = detectedCategory,
            paymentMethodSuggestion = detectedPayment,
            timestampMillis = timestamp,
            notes = notes
        )
    }
}
