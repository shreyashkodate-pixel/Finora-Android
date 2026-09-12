package com.finora.android.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.Amount
import com.finora.android.core.model.AppCurrency
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
import com.finora.android.data.local.relation.ExpenseWithDetails
import com.finora.android.data.repository.CategoryRepository
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.PaymentMethodRepository
import com.finora.android.data.repository.ProfileRepository
import com.finora.android.domain.model.ExpenseFilterState
import com.finora.android.domain.model.SortOrder
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

data class DayGroupedExpenses(
    val dateLabel: String,
    val dateSubtotal: Amount,
    val expenses: List<ExpenseWithDetails>
)

data class ExpensesUiState(
    val profileId: String = "",
    val currencySymbol: String = "₹",
    val filterState: ExpenseFilterState = ExpenseFilterState(),
    val categories: List<CategoryEntity> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val groupedExpenses: List<DayGroupedExpenses> = emptyList(),
    val totalSpend: Amount = Amount.ZERO,
    val totalCount: Int = 0,
    val isFilterSheetVisible: Boolean = false,
    val isLoading: Boolean = false
)

class ExpensesViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val categoryRepository: CategoryRepository = DatabaseModule.categoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository = DatabaseModule.paymentMethodRepository,
    private val expenseRepository: ExpenseRepository = DatabaseModule.expenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    private val _filterFlow = MutableStateFlow(ExpenseFilterState())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val profile = profileRepository.getActiveProfile()
                ?: profileRepository.createProfile("Personal", AppCurrency.DEFAULT.code)

            val currency = AppCurrency.fromCode(profile.currencyCode)
            _uiState.update {
                it.copy(
                    profileId = profile.id,
                    currencySymbol = currency.symbol
                )
            }

            // Load categories and payment methods for filters
            launch {
                categoryRepository.getCategoriesFlow(profile.id).collect { cats ->
                    _uiState.update { it.copy(categories = cats) }
                }
            }
            launch {
                paymentMethodRepository.getPaymentMethodsFlow(profile.id).collect { pms ->
                    _uiState.update { it.copy(paymentMethods = pms) }
                }
            }

            // Combine raw expenses with dynamic filter state
            launch {
                combine(
                    expenseRepository.getAllExpensesFlow(profile.id),
                    _filterFlow
                ) { rawExpenses, filter ->
                    val filtered = applyFilters(rawExpenses, filter)
                    val grouped = groupExpensesByDay(filtered)
                    val totalMinor = filtered.sumOf { it.expense.amountMinorUnits }
                    Triple(grouped, Amount(totalMinor), filtered.size)
                }.collect { (grouped, totalSpend, count) ->
                    _uiState.update {
                        it.copy(
                            groupedExpenses = grouped,
                            totalSpend = totalSpend,
                            totalCount = count,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _filterFlow.update { it.copy(searchQuery = query) }
        _uiState.update { it.copy(filterState = _filterFlow.value) }
    }

    fun onApplyFilters(newFilterState: ExpenseFilterState) {
        _filterFlow.value = newFilterState
        _uiState.update { it.copy(filterState = newFilterState) }
    }

    fun onRemoveCategoryFilter(categoryId: String) {
        val updated = _filterFlow.value.selectedCategoryIds - categoryId
        onApplyFilters(_filterFlow.value.copy(selectedCategoryIds = updated))
    }

    fun onRemoveDateFilter() {
        onApplyFilters(_filterFlow.value.copy(datePreset = com.finora.android.domain.model.DatePreset.ALL))
    }

    fun onClearAllFilters() {
        onApplyFilters(ExpenseFilterState())
    }

    fun setFilterSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isFilterSheetVisible = visible) }
    }

    private fun applyFilters(
        expenses: List<ExpenseWithDetails>,
        filter: ExpenseFilterState
    ): List<ExpenseWithDetails> {
        val query = filter.searchQuery.trim().lowercase()
        val dateRange = filter.getDateRangeMillis()

        val filtered = expenses.filter { item ->
            // Search query filter
            val matchesQuery = query.isEmpty() ||
                    (item.expense.title?.lowercase()?.contains(query) == true) ||
                    item.category.name.lowercase().contains(query) ||
                    (item.paymentMethod?.name?.lowercase()?.contains(query) == true)

            // Date range filter
            val matchesDate = if (dateRange != null) {
                item.expense.expenseDate >= dateRange.first && item.expense.expenseDate <= dateRange.second
            } else true

            // Category filter
            val matchesCategory = filter.selectedCategoryIds.isEmpty() ||
                    filter.selectedCategoryIds.contains(item.category.id)

            // Payment method filter
            val matchesPayment = filter.selectedPaymentMethodIds.isEmpty() ||
                    (item.paymentMethod != null && filter.selectedPaymentMethodIds.contains(item.paymentMethod.id))

            matchesQuery && matchesDate && matchesCategory && matchesPayment
        }

        return when (filter.sortOrder) {
            SortOrder.NEWEST -> filtered.sortedWith(
                compareByDescending<ExpenseWithDetails> { it.expense.expenseDate }
                    .thenByDescending { it.expense.createdAt }
            )
            SortOrder.OLDEST -> filtered.sortedWith(
                compareBy<ExpenseWithDetails> { it.expense.expenseDate }
                    .thenBy { it.expense.createdAt }
            )
            SortOrder.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.expense.amountMinorUnits }
            SortOrder.LOWEST_AMOUNT -> filtered.sortedBy { it.expense.amountMinorUnits }
        }
    }

    private fun groupExpensesByDay(expenses: List<ExpenseWithDetails>): List<DayGroupedExpenses> {
        val calendar = Calendar.getInstance()
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterdayStart = todayStart - 86_400_000L

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())

        val groupedMap = expenses.groupBy { dateFormat.format(Date(it.expense.expenseDate)) }

        return groupedMap.map { (_, dayExpenses) ->
            val firstDate = dayExpenses.first().expense.expenseDate
            val label = when {
                firstDate >= todayStart -> "Today • ${SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(firstDate))}"
                firstDate >= yesterdayStart -> "Yesterday • ${SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(firstDate))}"
                else -> displayDateFormat.format(Date(firstDate))
            }

            val subtotal = dayExpenses.sumOf { it.expense.amountMinorUnits }

            DayGroupedExpenses(
                dateLabel = label,
                dateSubtotal = Amount(subtotal),
                expenses = dayExpenses
            )
        }
    }

    fun deleteExpense(expenseId: String) {
        val profileId = _uiState.value.profileId
        viewModelScope.launch {
            val resolvedProfileId = if (profileId.isNotBlank()) profileId else profileRepository.getActiveProfile()?.id
            if (resolvedProfileId != null) {
                expenseRepository.deleteExpenseById(expenseId, resolvedProfileId)
            }
        }
    }
}
