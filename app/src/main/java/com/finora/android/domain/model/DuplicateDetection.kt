package com.finora.android.domain.model

import kotlin.math.abs

data class DuplicateMatch(
    val existingExpenseId: String,
    val existingTitle: String,
    val existingAmountMinorUnits: Long,
    val existingTimestamp: Long,
    val confidencePercent: Int,
    val matchReasons: List<String>
)

object DuplicateDetection {

    private const val TWO_DAYS_MILLIS = 2 * 24 * 60 * 60 * 1000L

    fun findDuplicate(
        candidateAmountMinorUnits: Long,
        candidateTitle: String,
        candidateTimestamp: Long,
        existingExpenses: List<SimpleExpenseRecord>
    ): DuplicateMatch? {
        val trimmedCandidateTitle = candidateTitle.trim().lowercase()

        for (existing in existingExpenses) {
            val isExactAmount = existing.amountMinorUnits == candidateAmountMinorUnits
            val timeDelta = abs(existing.timestampMillis - candidateTimestamp)
            val isDateClose = timeDelta <= TWO_DAYS_MILLIS
            val isTitleMatch = existing.title.trim().lowercase() == trimmedCandidateTitle ||
                    (trimmedCandidateTitle.isNotEmpty() && existing.title.contains(trimmedCandidateTitle, ignoreCase = true))

            val reasons = mutableListOf<String>()
            var confidence = 0

            if (isExactAmount) {
                reasons.add("Exact identical amount (₹${candidateAmountMinorUnits / 100})")
                confidence += 50
            }

            if (isDateClose) {
                val daysDiff = timeDelta / (24 * 60 * 60 * 1000L)
                reasons.add(if (daysDiff == 0L) "Logged on the same day" else "Logged within $daysDiff day(s)")
                confidence += 25
            }

            if (isTitleMatch && trimmedCandidateTitle.isNotBlank()) {
                reasons.add("Matching merchant title: '${existing.title}'")
                confidence += 25
            }

            if (isExactAmount && (isDateClose || isTitleMatch)) {
                return DuplicateMatch(
                    existingExpenseId = existing.id,
                    existingTitle = existing.title,
                    existingAmountMinorUnits = existing.amountMinorUnits,
                    existingTimestamp = existing.timestampMillis,
                    confidencePercent = confidence,
                    matchReasons = reasons
                )
            }
        }

        return null
    }
}
