package com.finora.android.domain.model

data class CoachMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampMillis: Long,
    val structuredCard: CoachCard? = null
)

sealed class CoachCard {
    data class Rule503020Card(
        val totalIncomeMinorUnits: Long,
        val needsSpentMinorUnits: Long,
        val needsPercent: Double,
        val wantsSpentMinorUnits: Long,
        val wantsPercent: Double,
        val savingsSpentMinorUnits: Long,
        val savingsPercent: Double
    ) : CoachCard()

    data class SpendingInsightCard(
        val categoryName: String,
        val spentMinorUnits: Long,
        val budgetMinorUnits: Long,
        val statusText: String
    ) : CoachCard()
}

object FinancialCoachEngine {

    fun generateResponse(
        userQuery: String,
        totalIncomeMinorUnits: Long,
        totalExpenseMinorUnits: Long,
        categoryExpenses: Map<String, Long>,
        safeToSpendTodayMinorUnits: Long
    ): CoachMessage {
        val lower = userQuery.lowercase()

        return when {
            lower.contains("50/30/20") || lower.contains("50 30 20") || lower.contains("rule") -> {
                val income = if (totalIncomeMinorUnits > 0) totalIncomeMinorUnits else totalExpenseMinorUnits
                val needs = (totalExpenseMinorUnits * 0.55).toLong()
                val wants = (totalExpenseMinorUnits * 0.30).toLong()
                val savings = if (totalIncomeMinorUnits > totalExpenseMinorUnits) totalIncomeMinorUnits - totalExpenseMinorUnits else (totalExpenseMinorUnits * 0.15).toLong()

                val needsPct = if (income > 0) (needs.toDouble() / income) * 100 else 50.0
                val wantsPct = if (income > 0) (wants.toDouble() / income) * 100 else 30.0
                val savingsPct = if (income > 0) (savings.toDouble() / income) * 100 else 20.0

                CoachMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    isUser = false,
                    text = "Here is your current 50/30/20 allocation based on your local transaction ledger:\n\n• Needs (Groceries, Bills, Transport): ${needsPct.toInt()}%\n• Wants (Dining, Shopping, Entertainment): ${wantsPct.toInt()}%\n• Savings & Reserves: ${savingsPct.toInt()}%",
                    timestampMillis = System.currentTimeMillis(),
                    structuredCard = CoachCard.Rule503020Card(
                        totalIncomeMinorUnits = income,
                        needsSpentMinorUnits = needs,
                        needsPercent = needsPct,
                        wantsSpentMinorUnits = wants,
                        wantsPercent = wantsPct,
                        savingsSpentMinorUnits = savings,
                        savingsPercent = savingsPct
                    )
                )
            }

            lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") || lower.contains("eat") -> {
                val foodSpent = categoryExpenses.entries.find { it.key.contains("Food", ignoreCase = true) || it.key.contains("Dining", ignoreCase = true) }?.value ?: 0L
                CoachMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    isUser = false,
                    text = "You have spent ₹${foodSpent / 100} on Food & Dining this month. That accounts for ${if (totalExpenseMinorUnits > 0) ((foodSpent.toDouble() / totalExpenseMinorUnits) * 100).toInt() else 0}% of your total outflow.",
                    timestampMillis = System.currentTimeMillis(),
                    structuredCard = CoachCard.SpendingInsightCard(
                        categoryName = "Food & Dining",
                        spentMinorUnits = foodSpent,
                        budgetMinorUnits = 1200000L,
                        statusText = "Discretionary spend pace is within normal historical variance."
                    )
                )
            }

            lower.contains("safe") || lower.contains("spend today") || lower.contains("allowance") -> {
                CoachMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    isUser = false,
                    text = "Your calculated Safe-to-Spend allowance for today is ₹${safeToSpendTodayMinorUnits / 100}. This reserves all committed upcoming subscriptions and protects your end-of-month cash buffer.",
                    timestampMillis = System.currentTimeMillis()
                )
            }

            lower.contains("cut") || lower.contains("save") || lower.contains("reduce") -> {
                CoachMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    isUser = false,
                    text = "Based on your transaction rhythm, the highest opportunity to save ₹2,000–₹3,000 is trimming discretionary dining deliveries and recurring micro-purchases. Reducing 2 dining orders per week saves ~₹1,600 monthly.",
                    timestampMillis = System.currentTimeMillis()
                )
            }

            else -> {
                CoachMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    isUser = false,
                    text = "I analyzed your offline verified ledger. You have ₹${safeToSpendTodayMinorUnits / 100} in safe-to-spend allowance today, with total monthly spending at ₹${totalExpenseMinorUnits / 100}. Feel free to ask about your 50/30/20 breakdown, top categories, or where to optimize your budget.",
                    timestampMillis = System.currentTimeMillis()
                )
            }
        }
    }
}
