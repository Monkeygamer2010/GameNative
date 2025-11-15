package app.gamenative.ui.component.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.gamenative.ui.component.dialog.ImportProfileDialog
import app.gamenative.ui.component.dialog.ProfilePickerDialog
import app.gamenative.ui.component.dialog.UnifiedProfileCreationDialog
import com.winlator.container.Container
import com.winlator.inputcontrols.ControlsProfile
import com.winlator.inputcontrols.InputControlsManager

/**
 * Prominent card for selecting and editing the input controls profile
 *
 * Shows the currently selected profile with options to:
 * - Change profile (opens profile picker)
 * - Create from template
 * - Clone profile
 * - Edit profile (opens unified profile editor)
 */
@Composable
fun ProfileSelectionCard(
    context: Context,
    selectedProfileId: Int,
    onProfileSelected: (Int) -> Unit,
    onEditProfile: (ControlsProfile) -> Unit,
    container: Container? = null
) {
    // Always create a fresh InputControlsManager to ensure we get latest profiles
    val currentProfile = remember(selectedProfileId) {
        val manager = InputControlsManager(context)
        manager.getProfile(selectedProfileId)
    }

    // Dialog states
    var showProfilePicker by remember { mutableStateOf(false) }
    var showCreateProfile by remember { mutableStateOf(false) }
    var showImportProfile by remember { mutableStateOf(false) }
    var showDropdownMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Gamepad,
                    contentDescription = "Input Profile",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Input Controls Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Current profile display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Selected Profile",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentProfile?.name ?: "No Profile Selected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Profile operations dropdown
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showDropdownMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Profile Options")
                    }

                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Change Profile") },
                            onClick = {
                                showDropdownMenu = false
                                showProfilePicker = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Create New Profile") },
                            onClick = {
                                showDropdownMenu = false
                                showCreateProfile = true
                            }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text("Import Profile") },
                            leadingIcon = {
                                Icon(Icons.Default.Upload, contentDescription = null)
                            },
                            onClick = {
                                showDropdownMenu = false
                                showImportProfile = true
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Export Current Profile") },
                            leadingIcon = {
                                Icon(Icons.Default.FileDownload, contentDescription = null)
                            },
                            onClick = {
                                showDropdownMenu = false
                                currentProfile?.let { profile ->
                                    val manager = InputControlsManager(context)
                                    val exportedFile = manager.exportProfile(profile)
                                    if (exportedFile != null) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Profile exported to:\n${exportedFile.absolutePath}",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Failed to export profile",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            enabled = currentProfile != null
                        )

                        // Lock/Unlock option (only show if in game context)
                        if (container != null && currentProfile != null) {
                            HorizontalDivider()

                            val isLocked = currentProfile.isLockedToGame
                            DropdownMenuItem(
                                text = {
                                    Text(if (isLocked) "Unlock from ${container.name}" else "Lock to ${container.name}")
                                },
                                onClick = {
                                    showDropdownMenu = false

                                    // Toggle lock status
                                    val newLockStatus = if (isLocked) null else container.id.toString()
                                    currentProfile.setLockedToContainer(newLockStatus)

                                    // Save the profile
                                    currentProfile.save()

                                    // Show toast
                                    android.widget.Toast.makeText(
                                        context,
                                        if (isLocked) "Profile unlocked - now available globally"
                                        else "Profile locked to ${container.name}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }

                // Edit Profile button
                Button(
                    onClick = {
                        currentProfile?.let { onEditProfile(it) }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = currentProfile != null
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profile")
                }
            }

            // Info text
            Text(
                text = "This profile controls on-screen controls, physical controller bindings, and input behavior",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }

    // Dialogs
    if (showProfilePicker) {
        ProfilePickerDialog(
            context = context,
            container = container,
            currentProfileId = selectedProfileId,
            onProfileSelected = { profileId ->
                onProfileSelected(profileId)
            },
            onDismiss = { showProfilePicker = false }
        )
    }

    if (showCreateProfile) {
        UnifiedProfileCreationDialog(
            context = context,
            container = container,
            onProfileCreated = { newProfile ->
                onProfileSelected(newProfile.id)
            },
            onDismiss = { showCreateProfile = false }
        )
    }

    if (showImportProfile) {
        ImportProfileDialog(
            context = context,
            onProfileImported = {
                // Refresh the profile list by forcing recomposition
                // The parent will handle this automatically
                android.widget.Toast.makeText(
                    context,
                    "Profile imported successfully! You can now select it.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { showImportProfile = false }
        )
    }
}
