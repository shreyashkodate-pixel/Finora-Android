package com.finora.android.ui.screens.budgets.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finora.android.core.model.Amount
import com.finora.android.data.local.entity.CategoryEntity

@Composable
fun SetBudgetDialog(
    title: String,
    currencySymbol: String,
    initialAmountMinorUnits: Long = 0L,
    isCategoryBudget: Boolean = false,
    availableCategories: List<CategoryEntity> = emptyList(),
    preselectedCategoryId: String? = null,
    onSave: (Amount, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var amountInput by remember {
        val initialStr = if (initialAmountMinorUnits > 0L) {
            val full = initialAmountMinorUnits / 100
            val fraction = initialAmountMinorUnits % 100
            if (fraction == 0L) "$full" else "$full.${fraction.toString().padStart(2, '0')}"
        } else ""
        mutableStateOf(initialStr)
    }

    var selectedCategoryId by remember {
        mutableStateOf(preselectedCategoryId ?: availableCategories.firstOrNull()?.id)
    }

    val parsedAmount: Amount? = remember(amountInput) {
        if (amountInput.isNotBlank()) {
            Amount.fromDecimalString(amountInput.trim())
        } else {
            null
        }
    }

    val isValid = (parsedAmount != null && parsedAmount.minorUnits > 0L) &&
            (!isCategoryBudget || selectedCategoryId != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isCategoryBudget && preselectedCategoryId == null && availableCategories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Select Category",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableCategories, key = { it.id }) { cat ->
                                FilterChip(
                                    selected = cat.id == selectedCategoryId,
                                    onClick = { selectedCategoryId = cat.id },
                                    label = { Text(cat.name) }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        // Allow digits and up to 1 decimal point with max 2 decimals
                        val sanitized = input.filter { it.isDigit() || it == '.' }
                        val parts = sanitized.split(".")
                        if (parts.size <= 2 && (parts.size == 1 || parts[1].length <= 2)) {
                            amountInput = sanitized
                        }
                    },
                    label = { Text("Budget Limit") },
                    prefix = {
                        Text(
                            text = "$currencySymbol ",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    placeholder = { Text("e.g. 50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "You will receive status badges and safe daily burn rate pacing for this cycle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid && parsedAmount != null) {
                        onSave(parsedAmount, selectedCategoryId)
                    }
                },
                enabled = isValid
            ) {
                Text("Save Limit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
