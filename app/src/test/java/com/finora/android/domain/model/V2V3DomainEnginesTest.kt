package com.finora.android.domain.model

import org.junit.Assert.*
import org.junit.Test

class V2V3DomainEnginesTest {

    @Test
    fun testSafeToSpendCalculation_healthyAndDanger() {
        // Healthy scenario: 30,000 remaining, 5,000 upcoming bills, 10 days left
        val healthy = SafeToSpendCalculation.calculate(
            remainingBudgetMinorUnits = 3000000L,
            totalBudgetMinorUnits = 5000000L,
            upcomingRecurringMinorUnits = 500000L,
            remainingDaysInCycle = 10
        )
        assertEquals(SafeToSpendState.HEALTHY, healthy.state)
        assertEquals(250000L, healthy.dailyAllowanceMinorUnits) // (30,000 - 5,000) / 10 = 2,500

        // Danger scenario: Exceeded budget
        val danger = SafeToSpendCalculation.calculate(
            remainingBudgetMinorUnits = -100000L,
            totalBudgetMinorUnits = 5000000L,
            upcomingRecurringMinorUnits = 500000L,
            remainingDaysInCycle = 5
        )
        assertEquals(SafeToSpendState.DANGER, danger.state)
        assertEquals(0L, danger.dailyAllowanceMinorUnits)
    }

    @Test
    fun testFinancialHealthScoreCalculation_allPillars() {
        val result = FinancialHealthScoreCalculation.calculate(
            savingsRatePercent = 25.0,
            budgetUtilizationPercent = 75.0,
            dailySpendStdDevRatio = 0.25,
            cashBufferMonths = 4.5,
            detectedLeaksCount = 0
        )
        assertTrue("Score should be high for optimal finances", result.overallScore >= 85)
        assertEquals("Excellent", result.ratingLabel)
        assertEquals(5, result.pillars.size)
        assertTrue(result.positiveDrivers.isNotEmpty())
        assertTrue(result.actionableTips.isNotEmpty())
    }

    @Test
    fun testLeakHunterCalculation_outliersAndMicroSpending() {
        val records = listOf(
            SimpleExpenseRecord("1", 50000L, "Coffee", 1000L),
            SimpleExpenseRecord("2", 60000L, "Coffee", 2000L),
            SimpleExpenseRecord("3", 55000L, "Coffee", 3000L),
            SimpleExpenseRecord("4", 52000L, "Coffee", 4000L),
            SimpleExpenseRecord("5", 58000L, "Coffee", 5000L),
            SimpleExpenseRecord("6", 1500000L, "Luxury Watch", 6000L), // Outlier
            SimpleExpenseRecord("7", 10000L, "Snack", 7000L),
            SimpleExpenseRecord("8", 12000L, "Tea", 8000L),
            SimpleExpenseRecord("9", 11000L, "Snack", 9000L),
            SimpleExpenseRecord("10", 15000L, "Tea", 10000L) // Micro-spending
        )

        val summary = LeakHunterCalculation.analyze(records)
        assertTrue(summary.totalScannedCount == 10)
        assertTrue("Should detect at least 1 high anomaly", summary.highAnomaliesCount >= 1)
        assertTrue(summary.anomalies.any { it.title.contains("Luxury Watch") })
        assertTrue(summary.anomalies.any { it.title.contains("Frequent Visits") })
    }

    @Test
    fun testDuplicateDetection_matchesCorrectly() {
        val existing = listOf(
            SimpleExpenseRecord("tx1", 45000L, "Swiggy", 100000L)
        )

        // Exact match within 2 days
        val match = DuplicateDetection.findDuplicate(
            candidateAmountMinorUnits = 45000L,
            candidateTitle = "swiggy",
            candidateTimestamp = 100000L + (10 * 60 * 1000L),
            existingExpenses = existing
        )
        assertNotNull(match)
        assertEquals("tx1", match?.existingExpenseId)
        assertTrue(match?.confidencePercent ?: 0 >= 75)

        // Non-matching amount
        val noMatch = DuplicateDetection.findDuplicate(
            candidateAmountMinorUnits = 99000L,
            candidateTitle = "swiggy",
            candidateTimestamp = 100000L,
            existingExpenses = existing
        )
        assertNull(noMatch)
    }

    @Test
    fun testNaturalLanguageExpenseParser_extractsDraft() {
        val input = "Swiggy 420 upi dinner yesterday #foodie"
        val now = 1700000000000L
        val draft = NaturalLanguageExpenseParser.parse(input, now)

        assertEquals(42000L, draft.amountMinorUnits)
        assertEquals("Swiggy", draft.merchantOrTitle)
        assertEquals("Food & Dining", draft.categorySuggestion)
        assertEquals("UPI", draft.paymentMethodSuggestion)
        assertTrue(draft.timestampMillis < now) // yesterday
    }

    @Test
    fun testCsvStatementParser_parsesValidRows() {
        val csv = """
            Date,Description,Amount
            2026-09-01,Swiggy,450.00
            2026-09-02,Uber,220.50
            2026-09-03,Blinkit,780.00
        """.trimIndent()

        val result = CsvStatementParser.parse("bank_statement.csv", csv)
        assertEquals(3, result.validRows.size)
        assertEquals(0, result.invalidRowsCount)
        assertEquals(145050L, result.totalDebitMinorUnits)
        assertEquals("Food & Dining", result.validRows[0].categorySuggestion)
        assertEquals("Transportation", result.validRows[1].categorySuggestion)
    }

    @Test
    fun testNetWorthCalculation_assetsAndLiabilities() {
        val accounts = listOf(
            AccountBalanceItem("1", "HDFC Bank", "BANK_ACCOUNT", 25000000L), // 2.5L
            AccountBalanceItem("2", "Cash Wallet", "CASH", 1500000L),       // 15k
            AccountBalanceItem("3", "Credit Card", "CREDIT_CARD", 3000000L)  // 30k debt
        )

        val summary = NetWorthCalculation.calculate(accounts, savingsGoalBalances = 5000000L)
        // Total Assets = 250,000 + 15,000 + 50,000 = 315,000 INR = 31,500,000 paise
        // Total Liabilities = 30,000 INR = 3,000,000 paise
        // Net worth = 285,000 INR = 28,500,000 paise
        assertEquals(31500000L, summary.totalAssetsMinorUnits)
        assertEquals(3000000L, summary.totalLiabilitiesMinorUnits)
        assertEquals(28500000L, summary.totalNetWorthMinorUnits)
        assertTrue(summary.isLeverageConservative)
    }

    @Test
    fun testPurchaseSimulatorCalculation_verdicts() {
        // Safe scenario
        val safe = PurchaseSimulatorCalculation.simulate(
            purchaseAmountMinorUnits = 100000L, // 1,000
            currentRemainingBudgetMinorUnits = 2000000L, // 20,000
            totalBudgetMinorUnits = 4000000L,
            upcomingRecurringMinorUnits = 200000L,
            remainingDaysInCycle = 15,
            totalLiquidSavingsMinorUnits = 5000000L
        )
        assertEquals(SimulatorVerdict.SAFE_TO_BUY, safe.verdict)

        // Delay scenario (exceeds budget)
        val delay = PurchaseSimulatorCalculation.simulate(
            purchaseAmountMinorUnits = 5000000L, // 50,000
            currentRemainingBudgetMinorUnits = 2000000L, // 20,000
            totalBudgetMinorUnits = 4000000L,
            upcomingRecurringMinorUnits = 200000L,
            remainingDaysInCycle = 15,
            totalLiquidSavingsMinorUnits = 5000000L
        )
        assertEquals(SimulatorVerdict.DELAY_PURCHASE, delay.verdict)
    }

    @Test
    fun testCurrencyEngine_offlineConversion() {
        // 100 USD to INR at 83.50 = 8350 INR
        val inr = CurrencyEngine.convert(10000L, "USD", "INR")
        assertEquals(835000L, inr)

        // JPY to INR
        val jpyInInr = CurrencyEngine.convert(100000L, "JPY", "INR") // 1000 JPY
        assertEquals(55000L, jpyInInr) // 550 INR
    }

    @Test
    fun testMerkleLedgerAudit_hashChainingAndIntegrity() {
        val records = listOf(
            SimpleExpenseRecord("1", 45000L, "Swiggy", 1000L),
            SimpleExpenseRecord("2", 32000L, "Uber", 2000L),
            SimpleExpenseRecord("3", 120000L, "Groceries", 3000L)
        )

        val audit = MerkleLedgerAudit.buildAndVerifyLedger(records)
        assertTrue(audit.isVerified)
        assertEquals(3, audit.totalBlocks)
        assertTrue(audit.merkleRootDigest.isNotEmpty())
        assertEquals(3, audit.blocks.size)
        // Most recent block on top
        assertEquals("Groceries", audit.blocks.first().transactionTitle)
    }

    @Test
    fun testFinancialCoachEngine_returnsStructuredAnswers() {
        val response = FinancialCoachEngine.generateResponse(
            userQuery = "What is my 50/30/20 breakdown?",
            totalIncomeMinorUnits = 10000000L,
            totalExpenseMinorUnits = 5000000L,
            categoryExpenses = mapOf("Food & Dining" to 1500000L),
            safeToSpendTodayMinorUnits = 250000L
        )
        assertFalse(response.isUser)
        assertTrue(response.text.contains("Needs"))
        assertNotNull(response.structuredCard)
    }
}
