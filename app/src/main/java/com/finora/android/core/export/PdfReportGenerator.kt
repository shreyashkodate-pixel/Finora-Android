package com.finora.android.core.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.finora.android.core.model.Amount
import com.finora.android.data.local.relation.ExpenseWithDetails
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates formatted PDF statements per SRS FR-RPT-V1.1-003 using native Android PdfDocument.
 */
object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 40f

    private val titleFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val itemDateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    fun generateExpensePdf(
        periodTitle: String,
        expenses: List<ExpenseWithDetails>,
        currencySymbol: String = "₹",
        outputStream: OutputStream
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Header Banner
        paint.color = Color.rgb(24, 28, 36) // Dark Finora brand color
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 90f, paint)

        // Title text
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FINORA FINANCIAL STATEMENT", MARGIN, 50f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Reporting Period: $periodTitle", MARGIN, 72f, paint)

        // 2. Summary Metrics Card
        var currentY = 115f
        val totalMinorUnits = expenses.sumOf { it.expense.amountMinorUnits }
        val totalAmountStr = Amount(totalMinorUnits).toFormattedString(currencySymbol)
        val transactionCount = expenses.size
        val avgMinorUnits = if (transactionCount > 0) totalMinorUnits / transactionCount else 0L
        val avgAmountStr = Amount(avgMinorUnits).toFormattedString(currencySymbol)

        paint.color = Color.rgb(245, 246, 248)
        canvas.drawRoundRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 65f, 8f, 8f, paint)

        paint.color = Color.rgb(60, 64, 75)
        paint.textSize = 10f
        canvas.drawText("TOTAL SPENT", MARGIN + 20f, currentY + 25f, paint)
        canvas.drawText("TRANSACTIONS", MARGIN + 180f, currentY + 25f, paint)
        canvas.drawText("AVG PER EXPENSE", MARGIN + 340f, currentY + 25f, paint)

        paint.color = Color.rgb(18, 20, 24)
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(totalAmountStr, MARGIN + 20f, currentY + 48f, paint)
        canvas.drawText("$transactionCount", MARGIN + 180f, currentY + 48f, paint)
        canvas.drawText(avgAmountStr, MARGIN + 340f, currentY + 48f, paint)

        currentY += 90f

        // 3. Category Breakdown Section
        paint.color = Color.rgb(20, 24, 30)
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Category Breakdown", MARGIN, currentY, paint)
        currentY += 15f

        val categoryGroups = expenses.groupBy { it.category.name }
            .mapValues { (_, list) -> list.sumOf { it.expense.amountMinorUnits } }
            .toList()
            .sortedByDescending { it.second }
            .take(6)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(90, 95, 105)

        for ((catName, catTotal) in categoryGroups) {
            val percentage = if (totalMinorUnits > 0) (catTotal * 100) / totalMinorUnits else 0
            val catAmountStr = Amount(catTotal).toFormattedString(currencySymbol)
            paint.color = Color.rgb(40, 44, 52)
            canvas.drawText("• $catName", MARGIN + 10f, currentY, paint)
            canvas.drawText("$catAmountStr ($percentage%)", MARGIN + 220f, currentY, paint)
            currentY += 16f
        }

        currentY += 15f

        // 4. Transaction Listing Table
        paint.color = Color.rgb(20, 24, 30)
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Transaction Listing", MARGIN, currentY, paint)
        currentY += 18f

        // Table Header
        paint.color = Color.rgb(235, 238, 242)
        canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 22f, paint)

        paint.color = Color.rgb(60, 64, 75)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DATE", MARGIN + 10f, currentY + 15f, paint)
        canvas.drawText("TITLE / CATEGORY", MARGIN + 120f, currentY + 15f, paint)
        canvas.drawText("PAYMENT", MARGIN + 320f, currentY + 15f, paint)
        canvas.drawText("AMOUNT", MARGIN + 430f, currentY + 15f, paint)
        currentY += 30f

        // Table Rows
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val maxRows = 20
        for ((index, item) in expenses.take(maxRows).withIndex()) {
            if (currentY > PAGE_HEIGHT - 60f) break

            if (index % 2 == 1) {
                paint.color = Color.rgb(250, 251, 252)
                canvas.drawRect(MARGIN, currentY - 12f, PAGE_WIDTH - MARGIN, currentY + 8f, paint)
            }

            val expense = item.expense
            val dateStr = itemDateFormat.format(Date(expense.expenseDate))
            val titleStr = expense.title?.ifBlank { item.category.name } ?: item.category.name
            val payStr = item.paymentMethod?.name ?: "—"
            val amtStr = Amount(expense.amountMinorUnits).toFormattedString(currencySymbol)

            paint.color = Color.rgb(40, 44, 52)
            canvas.drawText(dateStr, MARGIN + 10f, currentY, paint)
            canvas.drawText(titleStr.take(28), MARGIN + 120f, currentY, paint)
            canvas.drawText(payStr.take(18), MARGIN + 320f, currentY, paint)
            canvas.drawText(amtStr, MARGIN + 430f, currentY, paint)

            currentY += 20f
        }

        // 5. Footer
        paint.color = Color.rgb(150, 155, 165)
        paint.textSize = 8f
        canvas.drawText("Generated by Finora • Offline & Privacy-First Personal Finance", MARGIN, PAGE_HEIGHT - 25f, paint)
        val printDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Export Date: $printDate", PAGE_WIDTH - MARGIN - 130f, PAGE_HEIGHT - 25f, paint)

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }
}
