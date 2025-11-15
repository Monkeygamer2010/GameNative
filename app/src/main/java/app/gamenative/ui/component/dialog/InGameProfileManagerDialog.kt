package app.gamenative.ui.component.dialog

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.winlator.container.Container
import com.winlator.inputcontrols.ControlsProfile

/**
 * In-game profile manager dialog
 *
 * Features:
 * - Three buttons for profile creation methods
 * - All profiles created are automatically locked to the current game
 * - Returns created profile to caller for immediate application
 */
@Composable
fun InGameProfileManagerDialog(
    context: Context,
    container: Container,
    currentProfileId: Int? = null,
    onProfileSelected: (ControlsProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var showSwitchProfile by remember { mutableStateOf(false) }
    var showCreateProfile by remember { mutableStateOf(false) }
    var showImportProfile by remember { mutableStateOf(false) }

    // Main dialog
    if (!showSwitchProfile && !showCreateProfile && !showImportProfile) {
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
                            Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Manage Profiles",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "For: ${container.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    // Switch Profile button
                    FilledTonalButton(
                        onClick = { showSwitchProfile = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Switch Profile",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Choose from existing profiles",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Create New Profile button
                    FilledTonalButton(
                        onClick = { showCreateProfile = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Create New Profile",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Create blank or copy from template/existing profile",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Import Profile button
                    FilledTonalButton(
                        onClick = { showImportProfile = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Import Profile",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Import a profile from a file",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Cancel button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    // Sub-dialogs
    if (showCreateProfile) {
        UnifiedProfileCreationDialog(
            context = context,
            container = container,
            onProfileCreated = { profile ->
                onProfileSelected(profile)
                onDismiss()
            },
            onDismiss = { showCreateProfile = false }
        )
    }

    if (showSwitchProfile) {
        ProfilePickerDialog(
            context = context,
            container = container,
            currentProfileId = currentProfileId ?: 0,
            onProfileSelected = { profileId ->
                // Load the selected profile and pass it back
                val manager = com.winlator.inputcontrols.InputControlsManager(context)
                manager.getProfile(profileId)?.let { profile ->
                    onProfileSelected(profile)
                    onDismiss()
                }
            },
            onDismiss = { showSwitchProfile = false }
        )
    }

    if (showImportProfile) {
        ImportProfileDialog(
            context = context,
            onProfileImported = {
                // Optionally could select the imported profile here
                // For now just dismiss
            },
            onDismiss = { showImportProfile = false }
        )
    }
}
