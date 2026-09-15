package com.finora.android.domain.model

import kotlin.math.pow
import kotlin.math.sqrt

enum class EvidenceLevel {
    OBSERVED_FACT,
    DETECTED_PATTERN,
    AI_INTERPRETATION,
    RECOMMENDATION
}

enum class LeakSeverity {
    HIGH,
    MEDIUM,
    LOW
}

data class LeakAnomaly(
    val id: String,
    val title: String,
    val description: String,
    val amountMinorUnits: Long,
    val evidenceLevel: EvidenceLevel,
    val severity: LeakSeverity,
    val merchantName: String? = null
)

data class LeakHunterSummary(
    val totalScannedCount: Int,
    val highAnomaliesCount: Int,
    val mediumAnomaliesCount: Int,
    val totalPotentialWasteMinorUnits: Long,
    val anomalies: List<LeakAnomaly>
)

object LeakHunterCalculation {

    fun analyze(
        transactions: List<SimpleExpenseRecord>
    ): LeakHunterSummary {
        if (transactions.size < 3) {
            return LeakHunterSummary(
                totalScannedCount = transactions.size,
                highAnomaliesCount = 0,
                mediumAnomaliesCount = 0,
                totalPotentialWasteMinorUnits = 0L,
                anomalies = emptyList()
            )
        }

        val anomalies = mutableListOf<LeakAnomaly>()
        val amounts = transactions.map { it.amountMinorUnits }
        val mean = amounts.average()
        val variance = amounts.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)

        // 1. Statistical Outlier Detection (Z-Score > 2.0)
        if (stdDev > 0) {
            transactions.forEach { tx ->
                val zScore = (tx.amountMinorUnits - mean) / stdDev
                if (zScore > 2.2) {
                    anomalies.add(
                        LeakAnomaly(
                            id = "outlier_${tx.id}",
                            title = "High Outlier: ${tx.title}",
                            description = "Amount deviates significantly from your 90-day mean (Z-Score: ${String.format("%.1f", zScore)}).",
                            amountMinorUnits = tx.amountMinorUnits,
                            evidenceLevel = EvidenceLevel.DETECTED_PATTERN,
                            severity = LeakSeverity.HIGH,
                            merchantName = tx.title
                        )
                    )
                }
            }
        }

        // 2. High Frequency Merchant Patterns (>= 5 purchases)
        val merchantGroups = transactions.filter { it.title.isNotBlank() }.groupBy { it.title.trim().lowercase() }
        merchantGroups.forEach { (merchant, txList) ->
            if (txList.size >= 5) {
                val totalSpent = txList.sumOf { it.amountMinorUnits }
                anomalies.add(
                    LeakAnomaly(
                        id = "freq_$merchant",
                        title = "Frequent Visits: ${txList.first().title}",
                        description = "${txList.size} transactions recorded in this period totaling ₹${totalSpent / 100}.",
                        amountMinorUnits = totalSpent,
                        evidenceLevel = EvidenceLevel.OBSERVED_FACT,
                        severity = LeakSeverity.MEDIUM,
                        merchantName = txList.first().title
                    )
                )
            }
        }

        // 3. Micro-Spending Drain (small amounts <= 250 paise * 100 = 25000 minor units, repeated >= 4 times)
        val microTransactions = transactions.filter { it.amountMinorUnits in 2000L..25000L }
        if (microTransactions.size >= 4) {
            val totalMicro = microTransactions.sumOf { it.amountMinorUnits }
            anomalies.add(
                LeakAnomaly(
                    id = "micro_drain_cluster",
                    title = "Micro-Spending Cluster",
                    description = "${microTransactions.size} sub-₹250 transactions accumulating quietly to ₹${totalMicro / 100}.",
                    amountMinorUnits = totalMicro,
                    evidenceLevel = EvidenceLevel.DETECTED_PATTERN,
                    severity = LeakSeverity.MEDIUM
                )
            )
        }

        val highCount = anomalies.count { it.severity == LeakSeverity.HIGH }
        val medCount = anomalies.count { it.severity == LeakSeverity.MEDIUM }
        val waste = anomalies.sumOf { it.amountMinorUnits }

        return LeakHunterSummary(
            totalScannedCount = transactions.size,
            highAnomaliesCount = highCount,
            mediumAnomaliesCount = medCount,
            totalPotentialWasteMinorUnits = waste,
            anomalies = anomalies
        )
    }
}

/**
 * Lightweight transaction record used by local analytics algorithms.
 */
data class SimpleExpenseRecord(
    val id: String,
    val amountMinorUnits: Long,
    val title: String,
    val timestampMillis: Long,
    val categoryId: String? = null
)
