package app.gamenative.ui.component.dialog

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.winlator.container.Container
import com.winlator.inputcontrols.ControlsProfile
import com.winlator.inputcontrols.InputControlsManager

/**
 * Dialog for selecting input controls profile
 *
 * Features:
 * - Shows global profiles + profiles locked to current container by default
 * - Toggle to show profiles from other games
 * - Displays lock status with container name
 * - Shows templates separately (not selectable)
 */
@Composable
fun ProfilePickerDialog(
    context: Context,
    container: Container?,
    currentProfileId: Int,
    onProfileSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val inputControlsManager = remember { InputControlsManager(context) }
    var showAllProfiles by remember { mutableStateOf(false) }

    // Get filtered profiles based on toggle state
    val selectableProfiles = remember(showAllProfiles, container) {
        if (showAllProfiles || container == null) {
            // Show all profiles (excluding templates)
            inputControlsManager.getProfiles(true)
        } else {
            // Show global + this game's locked profiles
            inputControlsManager.getProfilesForContainer(container.id.toString())
        }
    }

    // Get templates separately (always shown, not selectable)
    val templates = remember {
        inputControlsManager.templates
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "Select Input Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Toggle for showing all profiles
                if (container != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show profiles from other games",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = showAllProfiles,
                            onCheckedChange = { showAllProfiles = it }
                        )
                    }
                    HorizontalDivider()
                }

                // Scrollable profile list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Selectable profiles
                    items(selectableProfiles) { profile ->
                        ProfileCard(
                            profile = profile,
                            isSelected = profile.id == currentProfileId,
                            isSelectable = true,
                            container = container,
                            onClick = {
                                onProfileSelected(profile.id)
                                onDismiss()
                            }
                        )
                    }

                    // Templates section (shown separately, not selectable)
                    if (templates.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Templates (use 'Create from Template' to use these)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(templates) { template ->
                            ProfileCard(
                                profile = template,
                                isSelected = false,
                                isSelectable = false,
                                container = null,
                                onClick = { /* Templates not selectable */ }
                            )
                        }
                    }
                }

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ControlsProfile,
    isSelected: Boolean,
    isSelectable: Boolean,
    container: Container?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelectable) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                !isSelectable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lock/Global icon
                    if (profile.isLockedToGame) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Game-locked profile",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Public,
                            contentDescription = "Global profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Show container name if locked
                if (profile.isLockedToGame) {
                    val containerName = getContainerDisplayName(profile.lockedToContainer, container)
                    Text(
                        text = "Locked to: $containerName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Template indicator
                if (profile.isTemplate) {
                    Text(
                        text = "Template",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Selection indicator
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Helper function to get display name for a container
 */
private fun getContainerDisplayName(lockedToContainer: String?, currentContainer: Container?): String {
    if (lockedToContainer == null) return "Unknown"

    // If this is the current container, show "This Game"
    if (currentContainer != null && lockedToContainer == currentContainer.id.toString()) {
        return "This Game (${currentContainer.name})"
    }

    // Otherwise, try to extract a readable name from the container ID
    // For now, just return the ID - can be enhanced later to look up actual container names
    return "Game #$lockedToContainer"
}
