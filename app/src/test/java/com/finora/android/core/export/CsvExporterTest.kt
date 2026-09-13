package com.finora.android.core.export

import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.ExpenseEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
import com.finora.android.data.local.relation.ExpenseWithDetails
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun testExportExpensesToCsv() {
        val category = CategoryEntity(id = "cat-1", profileId = "prof-1", name = "Food & Dining", iconName = "Fastfood", colorHex = "#FF0000")
        val paymentMethod = PaymentMethodEntity(id = "pm-1", profileId = "prof-1", name = "UPI / GPay", type = "UPI")
        val expense = ExpenseEntity(
            id = "exp-1",
            profileId = "prof-1",
            amountMinorUnits = 25050L, // 250.50
            currencyCode = "INR",
            categoryId = "cat-1",
            paymentMethodId = "pm-1",
            expenseDate = 1726000000000L,
            title = "Lunch with \"colleagues\", burger",
            notes = "Work lunch"
        )

        val item = ExpenseWithDetails(
            expense = expense,
            category = category,
            paymentMethod = paymentMethod
        )

        val csv = CsvExporter.exportExpensesToString(listOf(item))

        // Check header
        assertTrue(csv.contains("Date,Title,Amount,Currency,Category,Payment Method,Notes"))
        // Check amount formatting (250.50)
        assertTrue(csv.contains("250.50"))
        // Check escaped quotes in title
        assertTrue(csv.contains("\"Lunch with \"\"colleagues\"\", burger\""))
        // Check category and payment method
        assertTrue(csv.contains("Food & Dining"))
        assertTrue(csv.contains("UPI / GPay"))
    }
}
