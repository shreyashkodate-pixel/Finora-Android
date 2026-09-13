package com.finora.android.core.export

import com.finora.android.data.local.relation.ExpenseWithDetails
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports financial records to RFC 4180 compliant CSV format per SRS FR-RPT-V1.1-001 & FR-RPT-V1.1-002.
 */
object CsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun exportExpensesToCsv(
        expenses: List<ExpenseWithDetails>,
        outputStream: OutputStream
    ) {
        val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)
        writer.use { out ->
            // Header
            out.write("Date,Title,Amount,Currency,Category,Payment Method,Notes\r\n")

            // Rows
            for (item in expenses) {
                val expense = item.expense
                val formattedDate = dateFormat.format(Date(expense.expenseDate))
                val title = escapeCsv(expense.title?.ifBlank { item.category.name } ?: item.category.name)
                val major = expense.amountMinorUnits / 100
                val minor = expense.amountMinorUnits % 100
                val decimalAmount = String.format(Locale.US, "%d.%02d", major, minor)
                val currency = escapeCsv(expense.currencyCode)
                val category = escapeCsv(item.category.name)
                val paymentMethod = escapeCsv(item.paymentMethod?.name ?: "Unspecified")
                val notes = escapeCsv(expense.notes ?: "")

                out.write("$formattedDate,$title,$decimalAmount,$currency,$category,$paymentMethod,$notes\r\n")
            }
            out.flush()
        }
    }

    fun exportExpensesToString(expenses: List<ExpenseWithDetails>): String {
        val baos = java.io.ByteArrayOutputStream()
        exportExpensesToCsv(expenses, baos)
        return baos.toString(Charsets.UTF_8.name())
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
