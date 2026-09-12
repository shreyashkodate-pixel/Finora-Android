package com.finora.android.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.model.Amount
import com.finora.android.core.model.AppCurrency
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.repository.CategoryRepository
import com.finora.android.data.repository.ExpenseRepository
import com.finora.android.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class CategoryFilterTab {
    ALL,
    DEFAULT,
    CUSTOM
}

data class CategoryItemUiModel(
    val category: CategoryEntity,
    val transactionCountThisMonth: Int,
    val amountSpentThisMonth: Amount,
    val percentageShare: Int
)

data class DeleteDialogState(
    val category: CategoryEntity,
    val expenseCount: Int,
    val totalExpenseAmount: Amount,
    val requiresReassignment: Boolean,
    val availableReassignCategories: List<CategoryEntity> = emptyList(),
    val selectedReassignCategoryId: String? = null
)

data class CategoryManagementUiState(
    val filterTab: CategoryFilterTab = CategoryFilterTab.ALL,
    val currencySymbol: String = "₹",
    val totalMonthlySpend: Amount = Amount.ZERO,
    val totalMonthlyTransactions: Int = 0,
    val allCategories: List<CategoryItemUiModel> = emptyList(),
    val filteredCategories: List<CategoryItemUiModel> = emptyList(),
    val allCount: Int = 0,
    val defaultCount: Int = 0,
    val customCount: Int = 0,
    val isLoading: Boolean = true,
    val showAddEditDialog: Boolean = false,
    val editingCategory: CategoryEntity? = null,
    val deleteDialogState: DeleteDialogState? = null,
    val userFeedbackMessage: String? = null
)

class CategoryManagementViewModel(
    private val profileRepository: ProfileRepository = DatabaseModule.profileRepository,
    private val categoryRepository: CategoryRepository = DatabaseModule.categoryRepository,
    private val expenseRepository: ExpenseRepository = DatabaseModule.expenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryManagementUiState())
    val uiState: StateFlow<CategoryManagementUiState> = _uiState.asStateFlow()

    private val _filterTabFlow = MutableStateFlow(CategoryFilterTab.ALL)
    private var activeProfileId: String? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val profile = profileRepository.getActiveProfile()
                ?: profileRepository.createProfile("Personal", AppCurrency.DEFAULT.code)

            activeProfileId = profile.id
            val currency = AppCurrency.fromCode(profile.currencyCode)
            _uiState.update { it.copy(currencySymbol = currency.symbol) }

            val (startOfMonth, endOfMonth) = getCurrentMonthRange()

            combine(
                categoryRepository.getCategoriesFlow(profile.id),
                expenseRepository.getExpensesByDateRangeFlow(profile.id, startOfMonth, endOfMonth),
                _filterTabFlow
            ) { categories, monthExpenses, filterTab ->
                val totalUnits = monthExpenses.sumOf { it.expense.amountMinorUnits }
                val totalSpend = Amount(totalUnits)
                val totalTxns = monthExpenses.size

                val items = categories.map { cat ->
                    val catExpenses = monthExpenses.filter { it.expense.categoryId == cat.id }
                    val catUnits = catExpenses.sumOf { it.expense.amountMinorUnits }
                    val percentage = if (totalUnits > 0L) {
                        ((catUnits * 100L) / totalUnits).toInt()
                    } else {
                        0
                    }
                    CategoryItemUiModel(
                        category = cat,
                        transactionCountThisMonth = catExpenses.size,
                        amountSpentThisMonth = Amount(catUnits),
                        percentageShare = percentage
                    )
                }

                val allCount = items.size
                val defaultCount = items.count { it.category.isDefault }
                val customCount = items.count { !it.category.isDefault }

                val filtered = when (filterTab) {
                    CategoryFilterTab.ALL -> items
                    CategoryFilterTab.DEFAULT -> items.filter { it.category.isDefault }
                    CategoryFilterTab.CUSTOM -> items.filter { !it.category.isDefault }
                }

                CategoryManagementUiState(
                    filterTab = filterTab,
                    currencySymbol = currency.symbol,
                    totalMonthlySpend = totalSpend,
                    totalMonthlyTransactions = totalTxns,
                    allCategories = items,
                    filteredCategories = filtered,
                    allCount = allCount,
                    defaultCount = defaultCount,
                    customCount = customCount,
                    isLoading = false,
                    showAddEditDialog = _uiState.value.showAddEditDialog,
                    editingCategory = _uiState.value.editingCategory,
                    deleteDialogState = _uiState.value.deleteDialogState,
                    userFeedbackMessage = _uiState.value.userFeedbackMessage
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun setFilterTab(tab: CategoryFilterTab) {
        _filterTabFlow.value = tab
        _uiState.update { it.copy(filterTab = tab) }
    }

    fun openAddCategory() {
        _uiState.update {
            it.copy(
                showAddEditDialog = true,
                editingCategory = null
            )
        }
    }

    fun openEditCategory(category: CategoryEntity) {
        _uiState.update {
            it.copy(
                showAddEditDialog = true,
                editingCategory = category
            )
        }
    }

    fun dismissAddEditDialog() {
        _uiState.update {
            it.copy(
                showAddEditDialog = false,
                editingCategory = null
            )
        }
    }

    fun saveCategory(name: String, iconName: String, colorHex: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return

        val profileId = activeProfileId ?: return
        viewModelScope.launch {
            val editing = _uiState.value.editingCategory
            if (editing != null) {
                categoryRepository.updateCategory(
                    editing.copy(
                        name = trimmedName,
                        iconName = iconName,
                        colorHex = colorHex
                    )
                )
            } else {
                categoryRepository.createCategory(
                    profileId = profileId,
                    name = trimmedName,
                    iconName = iconName,
                    colorHex = colorHex
                )
            }
            dismissAddEditDialog()
        }
    }

    fun requestDeleteCategory(category: CategoryEntity) {
        val profileId = activeProfileId ?: return
        viewModelScope.launch {
            val expenseCount = categoryRepository.countExpensesForCategory(category.id, profileId)
            val otherCategories = _uiState.value.allCategories
                .map { it.category }
                .filter { it.id != category.id }

            // Calculate total expenses amount for this category
            val allExpenses = expenseRepository.getAllExpensesFlow(profileId)
            // Or get from month spend or count
            val requiresReassignment = expenseCount > 0
            val defaultReassign = if (requiresReassignment) {
                otherCategories.firstOrNull { it.name.contains("Other", ignoreCase = true) }
                    ?: otherCategories.firstOrNull()
            } else null

            // Find sum of all expenses for this category
            val categoryItem = _uiState.value.allCategories.find { it.category.id == category.id }
            val amount = categoryItem?.amountSpentThisMonth ?: Amount.ZERO

            _uiState.update {
                it.copy(
                    deleteDialogState = DeleteDialogState(
                        category = category,
                        expenseCount = expenseCount,
                        totalExpenseAmount = amount,
                        requiresReassignment = requiresReassignment,
                        availableReassignCategories = otherCategories,
                        selectedReassignCategoryId = defaultReassign?.id
                    )
                )
            }
        }
    }

    fun selectReassignCategory(categoryId: String) {
        _uiState.update { state ->
            val dialog = state.deleteDialogState ?: return@update state
            state.copy(deleteDialogState = dialog.copy(selectedReassignCategoryId = categoryId))
        }
    }

    fun confirmDeleteCategory() {
        val dialogState = _uiState.value.deleteDialogState ?: return
        viewModelScope.launch {
            try {
                if (dialogState.requiresReassignment) {
                    val reassignId = dialogState.selectedReassignCategoryId
                    requireNotNull(reassignId) { "A reassignment category must be selected." }
                    categoryRepository.deleteCategory(dialogState.category, reassignId)
                } else {
                    categoryRepository.deleteCategory(dialogState.category)
                }
                _uiState.update {
                    it.copy(
                        deleteDialogState = null,
                        userFeedbackMessage = "Category \"${dialogState.category.name}\" deleted successfully."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        userFeedbackMessage = e.message ?: "Failed to delete category."
                    )
                }
            }
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(deleteDialogState = null) }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(userFeedbackMessage = null) }
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }
}
