package com.finora.android.domain.model

/**
 * Budget health status per SRS FR-BUD-V1.0-004.
 * Statuses must always include textual labels for accessibility.
 */
enum class BudgetStatus(val label: String) {
    ON_TRACK("On Track"),
    NEAR_LIMIT("Near Limit"),
    OVER_BUDGET("Over Budget")
}
