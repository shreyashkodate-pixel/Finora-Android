package com.finora.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.finora.android.domain.model.BudgetStatus
import com.finora.android.ui.theme.StatusNearLimit
import com.finora.android.ui.theme.StatusOnTrack
import com.finora.android.ui.theme.StatusOverBudget

/**
 * Accessible status badge pairing color, icon, and explicit text label per SRS §9.
 */
@Composable
fun StatusBadge(
    status: BudgetStatus,
    modifier: Modifier = Modifier
) {
    val (color, icon) = when (status) {
        BudgetStatus.ON_TRACK -> StatusOnTrack to Icons.Default.CheckCircle
        BudgetStatus.NEAR_LIMIT -> StatusNearLimit to Icons.Default.Warning
        BudgetStatus.OVER_BUDGET -> StatusOverBudget to Icons.Default.Error
    }

    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, color.copy(alpha = 0.5f), shape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
