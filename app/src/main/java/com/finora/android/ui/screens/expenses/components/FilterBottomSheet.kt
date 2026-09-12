package com.finora.android.ui.screens.expenses.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finora.android.data.local.entity.CategoryEntity
import com.finora.android.data.local.entity.PaymentMethodEntity
import com.finora.android.domain.model.DatePreset
import com.finora.android.domain.model.ExpenseFilterState
import com.finora.android.domain.model.SortOrder
import com.finora.android.ui.components.FinoraPrimaryButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    filterState: ExpenseFilterState,
    categories: List<CategoryEntity>,
    paymentMethods: List<PaymentMethodEntity>,
    onApplyFilters: (ExpenseFilterState) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempState by remember { mutableStateOf(filterState) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter & Sort",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = {
                        tempState = ExpenseFilterState(searchQuery = tempState.searchQuery)
                    }
                ) {
                    Text(
                        text = "Reset All",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Presets
            Text(
                text = "DATE RANGE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DatePreset.entries.forEach { preset ->
                    val isSelected = tempState.datePreset == preset
                    SelectablePill(
                        label = preset.label,
                        isSelected = isSelected,
                        onClick = { tempState = tempState.copy(datePreset = preset) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Categories
            Text(
                text = "CATEGORIES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = tempState.selectedCategoryIds.contains(category.id)
                    SelectablePill(
                        label = category.name,
                        isSelected = isSelected,
                        onClick = {
                            val updated = if (isSelected) {
                                tempState.selectedCategoryIds - category.id
                            } else {
                                tempState.selectedCategoryIds + category.id
                            }
                            tempState = tempState.copy(selectedCategoryIds = updated)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Payment Methods
            if (paymentMethods.isNotEmpty()) {
                Text(
                    text = "PAYMENT METHODS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentMethods.forEach { method ->
                        val isSelected = tempState.selectedPaymentMethodIds.contains(method.id)
                        SelectablePill(
                            label = method.name,
                            isSelected = isSelected,
                            onClick = {
                                val updated = if (isSelected) {
                                    tempState.selectedPaymentMethodIds - method.id
                                } else {
                                    tempState.selectedPaymentMethodIds + method.id
                                }
                                tempState = tempState.copy(selectedPaymentMethodIds = updated)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Sort Order
            Text(
                text = "SORT BY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortOrder.entries.forEach { sort ->
                    val isSelected = tempState.sortOrder == sort
                    SelectablePill(
                        label = sort.label,
                        isSelected = isSelected,
                        onClick = { tempState = tempState.copy(sortOrder = sort) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Apply CTA
            FinoraPrimaryButton(
                text = "Apply Filters (${tempState.activeFilterCount})",
                onClick = {
                    onApplyFilters(tempState)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SelectablePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
