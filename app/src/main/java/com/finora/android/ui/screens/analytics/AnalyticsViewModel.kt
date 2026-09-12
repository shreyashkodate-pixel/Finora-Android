package com.finora.android.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.Amount
import com.finora.android.core.model.AppCurrency
import com.finora.android.data.local.relation.ExpenseWithDetails
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.domain.model.AnalyticsTimeframe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategorySpendDistribution(
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: String,
    val amount: Amount,
    val percentage: Int
)

data class VelocityBarPoint(
    val label: String,
    val amount: Amount,
    val heightRatio: Float,
    val isPeak: Boolean
)

data class AnalyticsUiState(
    val timeframe: AnalyticsTimeframe = AnalyticsTimeframe.MONTH,
    val currencySymbol: String = "₹",
    val periodLabel: String = "",
    val totalSpend: Amount = Amount.ZERO,
    val transactionCount: Int = 0,
    val dailyAverageSpend: Amount = Amount.ZERO,
    val categoryDistribution: List<CategorySpendDistribution> = emptyList(),
    val accessibleNarrative: String = "",
    val velocityBars: List<VelocityBarPoint> = emptyList(),
    val peakExpense: ExpenseWithDetails? = null,
    val topOutflows: List<ExpenseWithDetails> = emptyList(),
    val hasData: Boolean = false,
    val isLoading: Boolean = true
)

class AnalyticsViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val expenseRepository: ExpenseRepository = DatabaseModule.expenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val _timeframeFlow = MutableStateFlow(AnalyticsTimeframe.MONTH)

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val profile = profileRepository.getActiveProfile()
                ?: profileRepository.createProfile("Personal", AppCurrency.DEFAULT.code)

            val currency = AppCurrency.fromCode(profile.currencyCode)
            _uiState.update { it.copy(currencySymbol = currency.symbol) }

            combine(
                expenseRepository.getAllExpensesFlow(profile.id),
                _timeframeFlow
            ) { allExpenses, timeframe ->
                val (startDate, endDate) = timeframe.getDateRangeMillis()
                val filtered = allExpenses.filter { it.expense.expenseDate in startDate..endDate }
                val hasAny = filtered.isNotEmpty()

                val totalUnits = filtered.sumOf { it.expense.amountMinorUnits }
                val totalSpend = Amount(totalUnits)
                val count = filtered.size

                val daysCount = ((endDate - startDate) / 86_400_000L).toInt().coerceAtLeast(1)
                val dailyAvgUnits = if (daysCount > 0) totalUnits / daysCount else 0L
                val dailyAvgSpend = Amount(dailyAvgUnits)

                // Category distribution
                val distribution = calculateDistribution(filtered, totalUnits)

                // Accessible narrative
                val narrative = generateNarrative(distribution, totalSpend, currency.symbol)

                // Velocity bars
                val velocity = calculateVelocity(filtered, timeframe, startDate, endDate)

                // Peak expense
                val peak = filtered.maxByOrNull { it.expense.amountMinorUnits }

                // Top 5 outflows
                val top5 = filtered
                    .sortedByDescending { it.expense.amountMinorUnits }
                    .take(5)

                val periodLabel = when (timeframe) {
                    AnalyticsTimeframe.WEEK -> "Past 7 Days"
                    AnalyticsTimeframe.MONTH -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
                    AnalyticsTimeframe.YEAR -> SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
                }

                AnalyticsCalculations(
                    timeframe = timeframe,
                    periodLabel = periodLabel,
                    totalSpend = totalSpend,
                    count = count,
                    dailyAvg = dailyAvgSpend,
                    distribution = distribution,
                    narrative = narrative,
                    velocity = velocity,
                    peak = peak,
                    top5 = top5,
                    hasData = hasAny
                )
            }.collect { calc ->
                _uiState.update {
                    it.copy(
                        timeframe = calc.timeframe,
                        periodLabel = calc.periodLabel,
                        totalSpend = calc.totalSpend,
                        transactionCount = calc.count,
                        dailyAverageSpend = calc.dailyAvg,
                        categoryDistribution = calc.distribution,
                        accessibleNarrative = calc.narrative,
                        velocityBars = calc.velocity,
                        peakExpense = calc.peak,
                        topOutflows = calc.top5,
                        hasData = calc.hasData,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setTimeframe(timeframe: AnalyticsTimeframe) {
        _timeframeFlow.value = timeframe
    }

    private fun calculateDistribution(
        expenses: List<ExpenseWithDetails>,
        totalUnits: Long
    ): List<CategorySpendDistribution> {
        if (expenses.isEmpty() || totalUnits <= 0L) return emptyList()

        return expenses.groupBy { it.category.id }
            .map { (_, items) ->
                val first = items.first()
                val catUnits = items.sumOf { it.expense.amountMinorUnits }
                val pct = ((catUnits.toDouble() / totalUnits.toDouble()) * 100.0).toInt()
                CategorySpendDistribution(
                    categoryId = first.category.id,
                    categoryName = first.category.name,
                    categoryIcon = first.category.iconName,
                    categoryColorHex = first.category.colorHex,
                    amount = Amount(catUnits),
                    percentage = pct
                )
            }
            .sortedByDescending { it.amount.minorUnits }
    }

    private fun generateNarrative(
        distribution: List<CategorySpendDistribution>,
        totalSpend: Amount,
        currencySymbol: String
    ): String {
        if (distribution.isEmpty()) {
            return "No expenses recorded for this timeframe."
        }
        val top = distribution.first()
        val second = distribution.getOrNull(1)

        return if (second != null) {
            "Highest spending category is ${top.categoryName} at ${top.amount.toFormattedString(currencySymbol)} (${top.percentage}% of total), followed by ${second.categoryName} at ${second.amount.toFormattedString(currencySymbol)} (${second.percentage}%)."
        } else {
            "100% of spending in this period was allocated to ${top.categoryName} (${top.amount.toFormattedString(currencySymbol)})."
        }
    }

    private fun calculateVelocity(
        expenses: List<ExpenseWithDetails>,
        timeframe: AnalyticsTimeframe,
        startDate: Long,
        endDate: Long
    ): List<VelocityBarPoint> {
        if (expenses.isEmpty()) return emptyList()

        val points = mutableListOf<VelocityBarPoint>()
        val maxSpend = expenses.maxOfOrNull { it.expense.amountMinorUnits }?.coerceAtLeast(1L) ?: 1L

        when (timeframe) {
            AnalyticsTimeframe.WEEK -> {
                val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
                for (i in 6 downTo 0) {
                    val dayStart = endDate - (i * 86_400_000L)
                    val dayEnd = dayStart + 86_399_999L
                    val daySpend = expenses.filter { it.expense.expenseDate in dayStart..dayEnd }.sumOf { it.expense.amountMinorUnits }
                    val label = dateFormat.format(Date(dayStart))
                    val ratio = (daySpend.toFloat() / maxSpend.toFloat()).coerceIn(0.06f, 1f)
                    points.add(VelocityBarPoint(label, Amount(daySpend), ratio, daySpend == maxSpend && daySpend > 0L))
                }
            }
            AnalyticsTimeframe.MONTH -> {
                val cal = Calendar.getInstance()
                cal.timeInMillis = startDate
                val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                for (day in 1..totalDays) {
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    val dayStart = cal.timeInMillis
                    val dayEnd = dayStart + 86_399_999L
                    val daySpend = expenses.filter { it.expense.expenseDate in dayStart..dayEnd }.sumOf { it.expense.amountMinorUnits }
                    val label = if (day == 1 || day == 7 || day == 14 || day == 21 || day == totalDays) "$day" else ""
                    val ratio = (daySpend.toFloat() / maxSpend.toFloat()).coerceIn(0.06f, 1f)
                    points.add(VelocityBarPoint(label, Amount(daySpend), ratio, daySpend == maxSpend && daySpend > 0L))
                }
            }
            AnalyticsTimeframe.YEAR -> {
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                val cal = Calendar.getInstance()
                cal.timeInMillis = startDate
                for (m in 0..11) {
                    cal.set(Calendar.MONTH, m)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    val mStart = cal.timeInMillis
                    val maxD = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, maxD)
                    val mEnd = cal.timeInMillis
                    val mSpend = expenses.filter { it.expense.expenseDate in mStart..mEnd }.sumOf { it.expense.amountMinorUnits }
                    val label = monthFormat.format(Date(mStart)).take(1)
                    val ratio = (mSpend.toFloat() / maxSpend.toFloat()).coerceIn(0.06f, 1f)
                    points.add(VelocityBarPoint(label, Amount(mSpend), ratio, mSpend == maxSpend && mSpend > 0L))
                }
            }
        }
        return points
    }

    private data class AnalyticsCalculations(
        val timeframe: AnalyticsTimeframe,
        val periodLabel: String,
        val totalSpend: Amount,
        val count: Int,
        val dailyAvg: Amount,
        val distribution: List<CategorySpendDistribution>,
        val narrative: String,
        val velocity: List<VelocityBarPoint>,
        val peak: ExpenseWithDetails?,
        val top5: List<ExpenseWithDetails>,
        val hasData: Boolean
    )
}
