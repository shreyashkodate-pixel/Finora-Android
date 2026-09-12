package com.finora.android.domain.model

import java.util.Calendar

enum class DatePreset(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    CUSTOM("Custom")
}

enum class SortOrder(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST_AMOUNT("Highest"),
    LOWEST_AMOUNT("Lowest")
}

/**
 * Filter and sort state supporting combinable query parameters per SRS §3.5 (SRCH).
 */
data class ExpenseFilterState(
    val searchQuery: String = "",
    val datePreset: DatePreset = DatePreset.ALL,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val selectedCategoryIds: Set<String> = emptySet(),
    val selectedPaymentMethodIds: Set<String> = emptySet(),
    val sortOrder: SortOrder = SortOrder.NEWEST
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (datePreset != DatePreset.ALL) count++
            if (selectedCategoryIds.isNotEmpty()) count += selectedCategoryIds.size
            if (selectedPaymentMethodIds.isNotEmpty()) count += selectedPaymentMethodIds.size
            if (sortOrder != SortOrder.NEWEST) count++
            return count
        }

    fun getDateRangeMillis(): Pair<Long, Long>? {
        val calendar = Calendar.getInstance()
        return when (datePreset) {
            DatePreset.ALL -> null
            DatePreset.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis

                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                start to end
            }
            DatePreset.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis

                calendar.add(Calendar.DAY_OF_WEEK, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                start to end
            }
            DatePreset.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                start to end
            }
            DatePreset.CUSTOM -> {
                if (customStartDate != null && customEndDate != null) {
                    customStartDate to customEndDate
                } else null
            }
        }
    }
}
