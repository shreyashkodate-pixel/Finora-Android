package com.finora.android.domain.model

data class StatementRow(
    val rowIndex: Int,
    val dateString: String,
    val description: String,
    val amountMinorUnits: Long,
    val isDebit: Boolean,
    val categorySuggestion: String,
    val isDuplicate: Boolean = false,
    val isAccepted: Boolean = true,
    val validationError: String? = null
)

data class StatementParseResult(
    val fileName: String,
    val totalRows: Int,
    val validRows: List<StatementRow>,
    val invalidRowsCount: Int,
    val duplicatesCount: Int,
    val totalDebitMinorUnits: Long
)

object CsvStatementParser {

    fun parse(
        fileName: String,
        csvContent: String,
        existingExpenses: List<SimpleExpenseRecord> = emptyList()
    ): StatementParseResult {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return StatementParseResult(fileName, 0, emptyList(), 0, 0, 0L)
        }

        // Header detection
        val header = lines.first().lowercase()
        val delimiter = if (header.contains(";")) ";" else ","
        val dataLines = lines.drop(1)

        val validRows = mutableListOf<StatementRow>()
        var invalidCount = 0
        var duplicatesCount = 0

        dataLines.forEachIndexed { index, rawLine ->
            val cols = rawLine.split(delimiter).map { it.trim().removeSurrounding("\"") }
            if (cols.size >= 3) {
                val dateStr = cols[0]
                val desc = cols[1]
                val rawAmount = cols[2].replace("₹", "").replace(",", "")
                val amountDouble = rawAmount.toDoubleOrNull()

                if (amountDouble != null && amountDouble > 0) {
                    val amountMinorUnits = (amountDouble * 100).toLong()
                    val category = suggestCategory(desc)
                    val isDuplicate = existingExpenses.any {
                        it.amountMinorUnits == amountMinorUnits &&
                                it.title.equals(desc, ignoreCase = true)
                    }

                    if (isDuplicate) duplicatesCount++

                    validRows.add(
                        StatementRow(
                            rowIndex = index + 1,
                            dateString = dateStr,
                            description = desc,
                            amountMinorUnits = amountMinorUnits,
                            isDebit = true,
                            categorySuggestion = category,
                            isDuplicate = isDuplicate,
                            isAccepted = !isDuplicate
                        )
                    )
                } else {
                    invalidCount++
                }
            } else {
                invalidCount++
            }
        }

        val totalDebits = validRows.filter { it.isAccepted }.sumOf { it.amountMinorUnits }

        return StatementParseResult(
            fileName = fileName,
            totalRows = dataLines.size,
            validRows = validRows,
            invalidRowsCount = invalidCount,
            duplicatesCount = duplicatesCount,
            totalDebitMinorUnits = totalDebits
        )
    }

    private fun suggestCategory(description: String): String {
        val lower = description.lowercase()
        return when {
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("cafe") || lower.contains("rest") -> "Food & Dining"
            lower.contains("uber") || lower.contains("ola") || lower.contains("petrol") || lower.contains("fuel") -> "Transportation"
            lower.contains("blinkit") || lower.contains("zepto") || lower.contains("mart") || lower.contains("supermarket") -> "Groceries"
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") -> "Shopping"
            lower.contains("bill") || lower.contains("bescom") || lower.contains("airtel") || lower.contains("jio") -> "Bills & Utilities"
            else -> "General"
        }
    }
}
