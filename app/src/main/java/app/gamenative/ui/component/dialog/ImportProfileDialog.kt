package app.gamenative.ui.component.dialog

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.winlator.inputcontrols.InputControlsManager
import org.json.JSONObject

/**
 * Import Profile Dialog
 *
 * Allows users to import profile .icp files from storage
 */
@Composable
fun ImportProfileDialog(
    context: Context,
    onProfileImported: () -> Unit,
    onDismiss: () -> Unit
) {
    val inputControlsManager = remember { InputControlsManager(context) }
    var isImporting by remember { mutableStateOf(false) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            try {
                // Read file content
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() }
                inputStream?.close()

                if (content != null) {
                    // Parse and import
                    val jsonData = JSONObject(content)
                    val importedProfile = inputControlsManager.importProfile(jsonData)

                    if (importedProfile != null) {
                        Toast.makeText(
                            context,
                            "Profile '${importedProfile.name}' imported successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        onProfileImported()
                        onDismiss()
                    } else {
                        Toast.makeText(
                            context,
                            "Failed to import profile: Invalid format",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Failed to read file",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error importing profile: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                isImporting = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Upload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Import Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Select a .icp profile file to import. The profile will be added to your collection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isImporting
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            // Launch file picker for all files (Android doesn't support custom extensions well)
                            filePickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isImporting
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Choose File")
                        }
                    }
                }
            }
        }
    }
}
