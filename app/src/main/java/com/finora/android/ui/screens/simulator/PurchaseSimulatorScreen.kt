package com.finora.android.ui.screens.simulator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finora.android.domain.model.SimulatorVerdict

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseSimulatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: PurchaseSimulatorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Purchase Simulator", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "DETERMINISTIC MONTE CARLO ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input Configuration Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = state.itemName,
                        onValueChange = { viewModel.onItemNameChanged(it) },
                        label = { Text("Target Acquisition / Item") },
                        leadingIcon = {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = state.purchaseAmountText,
                        onValueChange = { viewModel.onAmountChanged(it) },
                        label = { Text("Simulated Purchase Price (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick micro adjusters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { viewModel.addMicroAdjustment(-5000) },
                            label = { Text("-₹5,000", style = MaterialTheme.typography.labelSmall) }
                        )
                        SuggestionChip(
                            onClick = { viewModel.addMicroAdjustment(5000) },
                            label = { Text("+₹5,000", style = MaterialTheme.typography.labelSmall) }
                        )
                        SuggestionChip(
                            onClick = { viewModel.addMicroAdjustment(10000) },
                            label = { Text("+₹10,000", style = MaterialTheme.typography.labelSmall) }
                        )
                        SuggestionChip(
                            onClick = { viewModel.addMicroAdjustment(25000) },
                            label = { Text("+₹25,000", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Simulation Verdict Card
            state.simulationResult?.let { res ->
                val verdictColor = when (res.verdict) {
                    SimulatorVerdict.SAFE_TO_BUY -> MaterialTheme.colorScheme.primary
                    SimulatorVerdict.PROCEED_WITH_CAUTION -> MaterialTheme.colorScheme.secondary
                    SimulatorVerdict.DELAY_PURCHASE -> MaterialTheme.colorScheme.error
                }

                val verdictBg = when (res.verdict) {
                    SimulatorVerdict.SAFE_TO_BUY -> MaterialTheme.colorScheme.secondaryContainer
                    SimulatorVerdict.PROCEED_WITH_CAUTION -> MaterialTheme.colorScheme.surfaceVariant
                    SimulatorVerdict.DELAY_PURCHASE -> MaterialTheme.colorScheme.errorContainer
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = verdictBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (res.verdict == SimulatorVerdict.DELAY_PURCHASE) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = verdictColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = when (res.verdict) {
                                        SimulatorVerdict.SAFE_TO_BUY -> "SAFE TO BUY"
                                        SimulatorVerdict.PROCEED_WITH_CAUTION -> "PROCEED WITH CAUTION"
                                        SimulatorVerdict.DELAY_PURCHASE -> "DELAY PURCHASE RECOMMENDED"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = verdictColor
                                )
                            }
                        }

                        Text(
                            text = res.verdictRationale,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = res.impactAnalysis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider()

                        // Impact Comparison Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Safe-to-Spend / Day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "₹${res.safeToSpendBeforeMinorUnits / 100} → ₹${res.safeToSpendAfterMinorUnits / 100}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Remaining Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "₹${res.remainingBudgetAfterMinorUnits / 100}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (res.remainingBudgetAfterMinorUnits < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Alternatives List
                        if (res.alternatives.isNotEmpty()) {
                            Text("Recommended Next Steps", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            res.alternatives.forEach { alt ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                    Text(alt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
