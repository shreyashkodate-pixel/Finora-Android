package com.finora.android.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finora.android.core.model.Amount

data class DayRhythm(
    val dayLabel: String,
    val fullDateLabel: String,
    val amount: Amount,
    val barHeightRatio: Float,
    val isToday: Boolean
)

@Composable
fun WeeklyRhythmCard(
    rhythmDays: List<DayRhythm>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = "Weekly Rhythm",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Past 7 days spending distribution",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 7-day micro-bar visualizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                rhythmDays.forEach { day ->
                    DayBarItem(
                        day = day,
                        currencySymbol = currencySymbol,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayBarItem(
    day: DayRhythm,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val barColor = if (day.isToday) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    val labelColor = if (day.isToday) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val formattedAmount = when {
        day.amount.minorUnits == 0L -> "—"
        day.amount.minorUnits >= 100_000L -> "${currencySymbol}${String.format(java.util.Locale.US, "%.1fk", day.amount.minorUnits / 100_000.0)}"
        else -> "${currencySymbol}${day.amount.minorUnits / 100}"
    }

    val contentDesc = "${day.fullDateLabel}: ${day.amount.toFormattedString(currencySymbol)}"

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 2.dp)
            .semantics { contentDescription = contentDesc },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Amount label
        Text(
            text = formattedAmount,
            fontSize = 9.sp,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Bar container
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(68.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val clampedRatio = day.barHeightRatio.coerceIn(0.06f, 1.0f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(clampedRatio)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(barColor)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Day initial (M, T, W, etc.)
        Text(
            text = day.dayLabel,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium
        )
    }
}
