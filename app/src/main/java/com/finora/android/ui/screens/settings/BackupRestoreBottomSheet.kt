package com.finora.android.ui.screens.settings

import android.content.Context
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.finora.android.core.backup.BackupManager
import com.finora.android.core.backup.BackupPreview
import com.finora.android.core.backup.InvalidBackupPasswordException
import com.finora.android.core.backup.RestoreMode
import com.finora.android.core.backup.UnsupportedBackupFormatException
import com.finora.android.core.di.DatabaseModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSuccessMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Backup State
    var backupPassword by remember { mutableStateOf("") }
    var confirmBackupPassword by remember { mutableStateOf("") }
    var backupError by remember { mutableStateOf<String?>(null) }
    var isBackingUp by remember { mutableStateOf(false) }

    // Restore State
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var restorePassword by remember { mutableStateOf("") }
    var restoreError by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    var previewData by remember { mutableStateOf<Pair<BackupPreview, JSONObject>?>(null) }
    var selectedRestoreMode by remember { mutableStateOf(RestoreMode.IMPORT_INTO_CURRENT_PROFILE) }
    var customNewProfileName by remember { mutableStateOf("") }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isBackingUp = true
                val success = withContext(Dispatchers.IO) {
                    try {
                        val db = DatabaseModule.getDatabase()
                        val manager = BackupManager(db)
                        val activeProfile = DatabaseModule.profileRepository.getActiveProfile()
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            manager.createBackup(backupPassword, os, activeProfile?.id)
                        }
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }
                isBackingUp = false
                if (success) {
                    onSuccessMessage("Encrypted backup saved successfully!")
                    onDismiss()
                } else {
                    backupError = "Failed to save backup file."
                }
            }
        }
    }

    val openBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedRestoreUri = uri
            restoreError = null
            previewData = null
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
                text = "Backup & Restore Vault",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Create Backup") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Restore Backup") }
                )
            }

            if (selectedTab == 0) {
                // CREATE BACKUP TAB
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Backups are encrypted using AES-256-GCM. Your data remains fully under your control. A forgotten password cannot be recovered.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = backupPassword,
                    onValueChange = { backupPassword = it; backupError = null },
                    label = { Text("Backup Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = confirmBackupPassword,
                    onValueChange = { confirmBackupPassword = it; backupError = null },
                    label = { Text("Confirm Backup Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (backupError != null) {
                    Text(
                        text = backupError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        if (backupPassword.isBlank()) {
                            backupError = "Password cannot be empty."
                        } else if (backupPassword != confirmBackupPassword) {
                            backupError = "Passwords do not match."
                        } else {
                            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            createBackupLauncher.launch("Finora_Backup_$dateStr.finora")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isBackingUp
                ) {
                    Text(if (isBackingUp) "Encrypting & Saving..." else "Create Encrypted Backup")
                }
            } else {
                // RESTORE BACKUP TAB
                if (selectedRestoreUri == null) {
                    OutlinedButton(
                        onClick = { openBackupFileLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Text(" Select .finora Backup File", modifier = Modifier.padding(start = 8.dp))
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Selected Backup File", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    selectedRestoreUri?.lastPathSegment ?: "backup.finora",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            TextButton(onClick = { selectedRestoreUri = null; previewData = null }) {
                                Text("Change")
                            }
                        }
                    }

                    if (previewData == null) {
                        OutlinedTextField(
                            value = restorePassword,
                            onValueChange = { restorePassword = it; restoreError = null },
                            label = { Text("Enter Backup Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (restoreError != null) {
                            Text(
                                text = restoreError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                val uri = selectedRestoreUri ?: return@Button
                                scope.launch {
                                    isRestoring = true
                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            context.contentResolver.openInputStream(uri)?.use { ins ->
                                                val manager = BackupManager(DatabaseModule.getDatabase())
                                                manager.decryptAndPreview(restorePassword, ins)
                                            } ?: error("Unable to open file stream")
                                        }
                                        previewData = result
                                        restoreError = null
                                    } catch (e: InvalidBackupPasswordException) {
                                        restoreError = "Incorrect password or corrupt file."
                                    } catch (e: UnsupportedBackupFormatException) {
                                        restoreError = e.message
                                    } catch (e: Exception) {
                                        restoreError = "Failed to parse backup: ${e.localizedMessage}"
                                    } finally {
                                        isRestoring = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isRestoring && restorePassword.isNotBlank()
                        ) {
                            Text(if (isRestoring) "Decrypting & Validating..." else "Validate & Preview Backup")
                        }
                    } else {
                        // RESTORE PREVIEW & COMMIT
                        val preview = previewData!!.first
                        val dateFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            .format(Date(preview.createdAtEpochMillis))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Backup Preview", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Created: $dateFormatted", style = MaterialTheme.typography.bodySmall)
                                Text("• Expenses: ${preview.expenseCount}", style = MaterialTheme.typography.bodySmall)
                                Text("• Categories: ${preview.categoryCount}", style = MaterialTheme.typography.bodySmall)
                                Text("• Budgets: ${preview.budgetCount}", style = MaterialTheme.typography.bodySmall)
                                Text("• Payment Methods: ${preview.paymentMethodCount}", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Text("Restore Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 1. Import into Current Profile
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRestoreMode = RestoreMode.IMPORT_INTO_CURRENT_PROFILE },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedRestoreMode == RestoreMode.IMPORT_INTO_CURRENT_PROFILE,
                                    onClick = { selectedRestoreMode = RestoreMode.IMPORT_INTO_CURRENT_PROFILE }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text("Import into Current Profile (Recommended)", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    Text("Imports and maps all expenses, categories, and accounts into your currently active profile so you can continue your work.", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            // 2. Import as New Profile
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRestoreMode = RestoreMode.IMPORT_AS_NEW_PROFILE },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedRestoreMode == RestoreMode.IMPORT_AS_NEW_PROFILE,
                                    onClick = { selectedRestoreMode = RestoreMode.IMPORT_AS_NEW_PROFILE }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text("Import as New Profile", fontWeight = FontWeight.SemiBold)
                                    Text("Creates a separate new profile from the backup without modifying your current profile.", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            if (selectedRestoreMode == RestoreMode.IMPORT_AS_NEW_PROFILE) {
                                OutlinedTextField(
                                    value = customNewProfileName,
                                    onValueChange = { customNewProfileName = it },
                                    label = { Text("New Profile Name (Optional)") },
                                    placeholder = { Text("e.g. Work, Secondary, or Imported") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().padding(start = 36.dp, top = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            // 3. Replace
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRestoreMode = RestoreMode.REPLACE },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedRestoreMode == RestoreMode.REPLACE,
                                    onClick = { selectedRestoreMode = RestoreMode.REPLACE }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text("Replace Entire Vault", fontWeight = FontWeight.SemiBold)
                                    Text("Clears all existing profiles and records, replacing them with the backup.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val json = previewData!!.second
                                scope.launch {
                                    isRestoring = true
                                    val resultId = withContext(Dispatchers.IO) {
                                        try {
                                            val db = DatabaseModule.getDatabase()
                                            val manager = BackupManager(db)
                                            val activeProfile = DatabaseModule.profileRepository.getActiveProfile()
                                            manager.restoreFromDecryptedJson(
                                                json = json,
                                                mode = selectedRestoreMode,
                                                targetProfileId = activeProfile?.id,
                                                newProfileName = customNewProfileName.takeIf { it.isNotBlank() }
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            "ERROR"
                                        }
                                    }
                                    isRestoring = false
                                    if (resultId != "ERROR") {
                                        if (selectedRestoreMode == RestoreMode.IMPORT_AS_NEW_PROFILE && resultId != null) {
                                            DatabaseModule.profileRepository.switchActiveProfile(resultId)
                                            onSuccessMessage("Backup imported as new profile & activated!")
                                        } else if (selectedRestoreMode == RestoreMode.IMPORT_INTO_CURRENT_PROFILE) {
                                            onSuccessMessage("Backup imported into current profile successfully!")
                                        } else {
                                            onSuccessMessage("Backup successfully restored!")
                                        }
                                        onDismiss()
                                    } else {
                                        restoreError = "Failed to commit restore. Changes rolled back."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isRestoring
                        ) {
                            Text(if (isRestoring) "Restoring Database..." else "Confirm & Commit Restore")
                        }
                    }
                }
            }
        }
    }
}
