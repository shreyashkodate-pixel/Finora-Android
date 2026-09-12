package com.finora.android.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.Amount
import com.finora.android.core.model.AppCurrency
import com.finora.android.data.local.relation.ExpenseWithDetails
import com.finora.android.data.repository.BudgetRepository
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.domain.model.BudgetCalculator
import com.finora.android.domain.model.BudgetSummary
import com.finora.android.ui.screens.home.components.DayRhythm
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

data class TopCategorySpend(
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: String,
    val spendAmount: Amount,
    val percentageOfTotal: Int
)

data class HomeUiState(
    val profileName: String = "Personal",
    val avatarLetter: String = "P",
    val greeting: String = "Welcome",
    val cycleLabel: String = "",
    val cycleDayProgress: String = "",
    val currencySymbol: String = "₹",
    val monthOutflow: Amount = Amount.ZERO,
    val budgetSummary: BudgetSummary? = null,
    val todaySpend: Amount = Amount.ZERO,
    val dailyBurnRate: Amount = Amount.ZERO,
    val weeklyRhythm: List<DayRhythm> = emptyList(),
    val peakOutflow: ExpenseWithDetails? = null,
    val topCategories: List<TopCategorySpend> = emptyList(),
    val recentExpenses: List<ExpenseWithDetails> = emptyList(),
    val hasExpenses: Boolean = false,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val expenseRepository: ExpenseRepository = DatabaseModule.expenseRepository,
    private val budgetRepository: BudgetRepository = DatabaseModule.budgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val profile = profileRepository.getActiveProfile()
                ?: profileRepository.createProfile("Personal", AppCurrency.DEFAULT.code)

            val currency = AppCurrency.fromCode(profile.currencyCode)
            val name = profile.name.ifBlank { "Personal" }
            val avatar = name.first().uppercase()

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when (hour) {
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                else -> "Good evening"
            }

            // Month date calculations
            val calendar = Calendar.getInstance()
            val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)

            // Start of Month
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            // End of Month
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, daysInMonth)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfMonth = calendar.timeInMillis

            // Today boundaries
            val todayCal = Calendar.getInstance()
            val dayOfMonth = todayCal.get(Calendar.DAY_OF_MONTH)
            todayCal.set(Calendar.HOUR_OF_DAY, 0)
            todayCal.set(Calendar.MINUTE, 0)
            todayCal.set(Calendar.SECOND, 0)
            todayCal.set(Calendar.MILLISECOND, 0)
            val startOfToday = todayCal.timeInMillis
            val endOfToday = startOfToday + 86_399_999L

            val cycleFormat = SimpleDateFormat("MMM 1 – MMM $daysInMonth, yyyy", Locale.getDefault())
            val cycleLabel = cycleFormat.format(Date(startOfMonth))
            val cycleDayProgress = "Cycle Day $dayOfMonth/$daysInMonth"

            _uiState.update {
                it.copy(
                    profileName = name,
                    avatarLetter = avatar,
                    greeting = "$greeting, $name",
                    cycleLabel = cycleLabel,
                    cycleDayProgress = cycleDayProgress,
                    currencySymbol = currency.symbol
                )
            }

            // Combine reactive flows: all expenses, month expenses, recent expenses, budget
            launch {
                combine(
                    expenseRepository.getAllExpensesFlow(profile.id),
                    expenseRepository.getExpensesByDateRangeFlow(profile.id, startOfMonth, endOfMonth),
                    expenseRepository.getRecentExpensesFlow(profile.id, 4),
                    budgetRepository.getOverallBudgetFlow(profile.id, currentYearMonth)
                ) { allExpenses, monthExpenses, recentExpenses, overallBudget ->
                    val hasAny = allExpenses.isNotEmpty()
                    val monthTotalUnits = monthExpenses.sumOf { it.expense.amountMinorUnits }
                    val monthOutflow = Amount(monthTotalUnits)

                    // Budget summary
                    val budgetSummary = overallBudget?.let { budget ->
                        BudgetCalculator.calculate(
                            budgetAmount = Amount(budget.amountMinorUnits),
                            spentAmount = monthOutflow
                        )
                    }

                    // Today's spend
                    val todayUnits = monthExpenses
                        .filter { it.expense.expenseDate in startOfToday..endOfToday }
                        .sumOf { it.expense.amountMinorUnits }
                    val todaySpend = Amount(todayUnits)

                    // Daily burn rate = monthTotal / dayOfMonth (elapsed days)
                    val burnRateUnits = if (dayOfMonth > 0) monthTotalUnits / dayOfMonth else 0L
                    val dailyBurnRate = Amount(burnRateUnits)

                    // Peak outflow in current month
                    val peakOutflow = monthExpenses.maxByOrNull { it.expense.amountMinorUnits }

                    // Top 4 categories breakdown
                    val topCategories = calculateTopCategories(monthExpenses, monthTotalUnits)

                    // 7-Day Weekly Rhythm (today and 6 previous days)
                    val weeklyRhythm = calculateWeeklyRhythm(allExpenses, startOfToday)

                    HomeDashboardData(
                        hasExpenses = hasAny,
                        monthOutflow = monthOutflow,
                        budgetSummary = budgetSummary,
                        todaySpend = todaySpend,
                        dailyBurnRate = dailyBurnRate,
                        peakOutflow = peakOutflow,
                        topCategories = topCategories,
                        recentExpenses = recentExpenses,
                        weeklyRhythm = weeklyRhythm
                    )
                }.collect { data ->
                    _uiState.update {
                        it.copy(
                            hasExpenses = data.hasExpenses,
                            monthOutflow = data.monthOutflow,
                            budgetSummary = data.budgetSummary,
                            todaySpend = data.todaySpend,
                            dailyBurnRate = data.dailyBurnRate,
                            peakOutflow = data.peakOutflow,
                            topCategories = data.topCategories,
                            recentExpenses = data.recentExpenses,
                            weeklyRhythm = data.weeklyRhythm,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    private fun calculateTopCategories(
        monthExpenses: List<ExpenseWithDetails>,
        totalMonthUnits: Long
    ): List<TopCategorySpend> {
        if (monthExpenses.isEmpty() || totalMonthUnits <= 0L) return emptyList()

        val grouped = monthExpenses.groupBy { it.category.id }
        return grouped.map { (catId, items) ->
            val first = items.first()
            val catTotal = items.sumOf { it.expense.amountMinorUnits }
            val percentage = ((catTotal.toDouble() / totalMonthUnits.toDouble()) * 100.0).toInt()
            TopCategorySpend(
                categoryId = catId,
                categoryName = first.category.name,
                categoryIcon = first.category.iconName,
                categoryColorHex = first.category.colorHex,
                spendAmount = Amount(catTotal),
                percentageOfTotal = percentage
            )
        }
            .sortedByDescending { it.spendAmount.minorUnits }
            .take(4)
    }

    private fun calculateWeeklyRhythm(
        allExpenses: List<ExpenseWithDetails>,
        startOfToday: Long
    ): List<DayRhythm> {
        val days = mutableListOf<DayRhythm>()
        val dayInitialFormat = SimpleDateFormat("EEEEE", Locale.getDefault()) // Single letter: M, T, W, T, F, S, S
        val fullDateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())

        val daySpends = mutableListOf<Long>()

        for (i in 6 downTo 0) {
            val dayStart = startOfToday - (i * 86_400_000L)
            val dayEnd = dayStart + 86_399_999L
            val daySpend = allExpenses
                .filter { it.expense.expenseDate in dayStart..dayEnd }
                .sumOf { it.expense.amountMinorUnits }
            daySpends.add(daySpend)
        }

        val maxSpend = daySpends.maxOrNull()?.coerceAtLeast(1L) ?: 1L

        for (i in 6 downTo 0) {
            val dayStart = startOfToday - (i * 86_400_000L)
            val dayEnd = dayStart + 86_399_999L
            val daySpend = allExpenses
                .filter { it.expense.expenseDate in dayStart..dayEnd }
                .sumOf { it.expense.amountMinorUnits }

            val dateObj = Date(dayStart)
            val initial = dayInitialFormat.format(dateObj)
            val fullLabel = fullDateFormat.format(dateObj)
            val ratio = (daySpend.toFloat() / maxSpend.toFloat()).coerceIn(0.06f, 1.0f)

            days.add(
                DayRhythm(
                    dayLabel = initial,
                    fullDateLabel = fullLabel,
                    amount = Amount(daySpend),
                    barHeightRatio = ratio,
                    isToday = (i == 0)
                )
            )
        }
        return days
    }

    private data class HomeDashboardData(
        val hasExpenses: Boolean,
        val monthOutflow: Amount,
        val budgetSummary: BudgetSummary?,
        val todaySpend: Amount,
        val dailyBurnRate: Amount,
        val peakOutflow: ExpenseWithDetails?,
        val topCategories: List<TopCategorySpend>,
        val recentExpenses: List<ExpenseWithDetails>,
        val weeklyRhythm: List<DayRhythm>
    )
}
