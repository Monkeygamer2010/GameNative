package app.gamenative.ui.component.dialog

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
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
 * Dialog for cloning an existing profile
 *
 * Features:
 * - Shows all clonable profiles (templates + user profiles)
 * - Toggle to show game-locked profiles
 * - Input field for new profile name
 * - Checkbox to lock to current game (checked by default when in game context)
 * - Displays source profile's lock status
 * - Clones the selected profile with the specified name and lock status
 */
@Composable
fun CloneProfileDialog(
    context: Context,
    container: Container,
    onProfileCreated: (ControlsProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val inputControlsManager = remember { InputControlsManager(context) }
    var showGameLockedProfiles by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Get profiles based on toggle state
    val allProfiles = remember(showGameLockedProfiles) {
        if (showGameLockedProfiles) {
            // Show all profiles
            inputControlsManager.getProfiles(false)
        } else {
            // Show global profiles only (includes templates)
            inputControlsManager.globalProfiles
        }
    }

    // Filter profiles by search query
    val profiles = remember(allProfiles, searchQuery) {
        if (searchQuery.isBlank()) {
            allProfiles
        } else {
            allProfiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    var selectedProfile by remember { mutableStateOf<ControlsProfile?>(null) }
    var profileName by remember { mutableStateOf("") }
    var lockToGame by remember { mutableStateOf(true) }
    var isCreating by remember { mutableStateOf(false) }

    // Auto-generate profile name when source profile is selected
    LaunchedEffect(selectedProfile, container) {
        selectedProfile?.let { source ->
            // Generate name with container prefix
            val baseName = if (source.isTemplate) {
                InputControlsManager.sanitizeProfileName("${container.name} - ${source.name}")
            } else {
                InputControlsManager.sanitizeProfileName("${container.name} Profile")
            }

            // Find next available number if name exists
            val allExistingProfiles = inputControlsManager.getProfiles(false)
            var finalName = baseName
            var counter = 2

            while (allExistingProfiles.any { it.name == finalName }) {
                finalName = InputControlsManager.sanitizeProfileName("$baseName $counter")
                counter++
            }

            profileName = finalName
        }
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Clone Profile",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "For: ${container.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Profile name input
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("New Profile Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isCreating
                )

                // Lock to game checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Lock to ${container.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Only show this profile for this game",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Checkbox(
                        checked = lockToGame,
                        onCheckedChange = { lockToGame = it },
                        enabled = !isCreating
                    )
                }

                HorizontalDivider()

                // Profile selection header with toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Profile to Clone",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show game-locked",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Switch(
                            checked = showGameLockedProfiles,
                            onCheckedChange = { showGameLockedProfiles = it },
                            enabled = !isCreating
                        )
                    }
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search profiles") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating
                )

                // Profile list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (profiles.isEmpty()) {
                        item {
                            Text(
                                text = "No profiles available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(profiles) { profile ->
                            ClonableProfileCard(
                                profile = profile,
                                isSelected = profile == selectedProfile,
                                container = container,
                                onClick = { selectedProfile = profile },
                                enabled = !isCreating
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isCreating
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val sourceProfile = selectedProfile
                            val name = profileName.trim()

                            // Validation
                            val validationError = InputControlsManager.validateProfileName(name)
                            if (validationError != null) {
                                Toast.makeText(context, validationError, Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (sourceProfile == null) {
                                Toast.makeText(context, "Please select a profile to clone", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isCreating = true

                            try {
                                // Clone the profile with the specified name and lock status
                                val lockedToContainer = if (lockToGame) {
                                    container.id.toString()
                                } else {
                                    null
                                }

                                val newProfile = inputControlsManager.cloneProfile(
                                    sourceProfile,
                                    name,
                                    lockedToContainer
                                )

                                Toast.makeText(
                                    context,
                                    "Profile '$name' created successfully",
                                    Toast.LENGTH_SHORT
                                ).show()

                                onProfileCreated(newProfile)
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Failed to clone profile: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                isCreating = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isCreating && selectedProfile != null && profileName.trim().isNotEmpty()
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Clone")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClonableProfileCard(
    profile: ControlsProfile,
    isSelected: Boolean,
    container: Container?,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
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

                // Show lock status
                if (profile.isLockedToGame) {
                    val containerName = getContainerDisplayName(profile.lockedToContainer, container)
                    Text(
                        text = "Locked to: $containerName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (profile.isTemplate) {
                    Text(
                        text = "Template",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    Text(
                        text = "Global profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
    return "Game #$lockedToContainer"
}
