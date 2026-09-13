package com.finora.android.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.finora.android.core.di.DatabaseModule
import com.finora.android.core.export.CsvExporter
import com.finora.android.core.export.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ExportFormat {
    CSV,
    PDF
}

enum class ExportPeriod {
    THIS_MONTH,
    LAST_3_MONTHS,
    ALL_TIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onExportSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var selectedPeriod by remember { mutableStateOf(ExportPeriod.THIS_MONTH) }
    var isExporting by remember { mutableStateOf(false) }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (selectedFormat == ExportFormat.CSV) "text/csv" else "application/pdf"
        )
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isExporting = true
                val success = exportToUri(context, uri, selectedFormat, selectedPeriod)
                isExporting = false
                if (success) {
                    onExportSuccess("Report successfully exported!")
                    onDismiss()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Export Financial Statement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Generate and export your personal expense reports to CSV or printable PDF per SRS FR-RPT-V1.1.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Format Selection
            Text(
                text = "File Format",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFormat = ExportFormat.CSV },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedFormat == ExportFormat.CSV)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "CSV",
                            tint = if (selectedFormat == ExportFormat.CSV)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "CSV Data",
                            fontWeight = if (selectedFormat == ExportFormat.CSV) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFormat = ExportFormat.PDF },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedFormat == ExportFormat.PDF)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF",
                            tint = if (selectedFormat == ExportFormat.PDF)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "PDF Statement",
                            fontWeight = if (selectedFormat == ExportFormat.PDF) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Period Selection
            Text(
                text = "Time Period",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    ExportPeriod.THIS_MONTH to "This Month",
                    ExportPeriod.LAST_3_MONTHS to "Last 3 Months",
                    ExportPeriod.ALL_TIME to "All Time"
                ).forEach { (period, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPeriod = period },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isExporting = true
                            shareReport(context, selectedFormat, selectedPeriod)
                            isExporting = false
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isExporting
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    Text(text = " Share", modifier = Modifier.padding(start = 6.dp))
                }

                Button(
                    onClick = {
                        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                        val defaultFilename = if (selectedFormat == ExportFormat.CSV)
                            "Finora_Expenses_$dateStr.csv"
                        else
                            "Finora_Statement_$dateStr.pdf"
                        createDocLauncher.launch(defaultFilename)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isExporting
                ) {
                    Text(if (isExporting) "Exporting..." else "Save to File")
                }
            }
        }
    }
}

private suspend fun exportToUri(
    context: Context,
    uri: Uri,
    format: ExportFormat,
    period: ExportPeriod
): Boolean = withContext(Dispatchers.IO) {
    try {
        val database = DatabaseModule.getDatabase()
        val profile = database.profileDao().getActiveProfile() ?: return@withContext false
        val (startDate, endDate) = computeDateRange(period)

        val expenses = database.expenseDao().getExpensesByDateRangeFlow(profile.id, startDate, endDate).first()
        val periodTitle = when (period) {
            ExportPeriod.THIS_MONTH -> "This Month"
            ExportPeriod.LAST_3_MONTHS -> "Last 3 Months"
            ExportPeriod.ALL_TIME -> "All Transactions"
        }

        context.contentResolver.openOutputStream(uri)?.use { os ->
            if (format == ExportFormat.CSV) {
                CsvExporter.exportExpensesToCsv(expenses, os)
            } else {
                PdfReportGenerator.generateExpensePdf(periodTitle, expenses, "₹", os)
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private suspend fun shareReport(
    context: Context,
    format: ExportFormat,
    period: ExportPeriod
) = withContext(Dispatchers.IO) {
    try {
        val database = DatabaseModule.getDatabase()
        val profile = database.profileDao().getActiveProfile() ?: return@withContext
        val (startDate, endDate) = computeDateRange(period)

        val expenses = database.expenseDao().getExpensesByDateRangeFlow(profile.id, startDate, endDate).first()
        val periodTitle = when (period) {
            ExportPeriod.THIS_MONTH -> "This Month"
            ExportPeriod.LAST_3_MONTHS -> "Last 3 Months"
            ExportPeriod.ALL_TIME -> "All Transactions"
        }

        val cacheDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())

        val file = if (format == ExportFormat.CSV) {
            File(cacheDir, "Finora_Expenses_$dateStr.csv").also { f ->
                f.outputStream().use { CsvExporter.exportExpensesToCsv(expenses, it) }
            }
        } else {
            File(cacheDir, "Finora_Statement_$dateStr.pdf").also { f ->
                f.outputStream().use { PdfReportGenerator.generateExpensePdf(periodTitle, expenses, "₹", it) }
            }
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val mimeType = if (format == ExportFormat.CSV) "text/csv" else "application/pdf"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Finora Financial Statement").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun computeDateRange(period: ExportPeriod): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    val endDate = cal.timeInMillis

    val startDate = when (period) {
        ExportPeriod.THIS_MONTH -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        ExportPeriod.LAST_3_MONTHS -> {
            cal.add(Calendar.MONTH, -3)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        ExportPeriod.ALL_TIME -> 0L
    }
    return Pair(startDate, endDate)
}
