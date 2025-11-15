package app.gamenative.ui.screen.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.winlator.inputcontrols.ControlsProfile
import com.winlator.inputcontrols.InputControlsManager
import com.winlator.container.ContainerManager
import org.json.JSONObject

// Helper function to get element count without loading full elements
private fun getElementCount(context: Context, profile: ControlsProfile): String {
    return try {
        val file = ControlsProfile.getProfileFile(context, profile.id)
        if (file.exists()) {
            val json = JSONObject(file.readText())
            val elementsArray = json.optJSONArray("elements")
            val count = elementsArray?.length() ?: 0
            android.util.Log.d("ControlsEditorDialog", "Profile ${profile.name} (ID: ${profile.id}) has $count elements")
            " • $count elements"
        } else {
            android.util.Log.w("ControlsEditorDialog", "Profile file not found for ${profile.name} (ID: ${profile.id}): ${file.absolutePath}")
            " • File not found"
        }
    } catch (e: Exception) {
        android.util.Log.e("ControlsEditorDialog", "Error reading element count for ${profile.name} (ID: ${profile.id})", e)
        " • Error reading file"
    }
}

// Helper function to get container usage count for a profile
private fun getContainerUsageCount(context: Context, profileId: Int): Int {
    return try {
        val containerManager = ContainerManager(context)
        val containers = containerManager.containers
        containers.count { container ->
            try {
                val configFile = container.configFile
                if (configFile.exists()) {
                    val json = JSONObject(configFile.readText())
                    json.optInt("controlsProfileId", 4) == profileId
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("ControlsEditorDialog", "Error counting container usage for profile $profileId", e)
        0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlsEditorDialog(
    open: Boolean,
    onDismiss: () -> Unit
) {
    if (!open) return

    val context = LocalContext.current
    var profiles by remember { mutableStateOf(listOf<ControlsProfile>()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<ControlsProfile?>(null) }
    var showDuplicateDialog by remember { mutableStateOf<ControlsProfile?>(null) }
    var showUnifiedEditor by remember { mutableStateOf<Pair<ControlsProfile, Int>?>(null) } // Profile + Tab index
    var showEditChoiceDialog by remember { mutableStateOf<ControlsProfile?>(null) }
    var showUnlockConfirm by remember { mutableStateOf<ControlsProfile?>(null) }

    // State for showing templates and game-locked profiles
    var showTemplates by remember { mutableStateOf(false) }
    var showGameLockedProfiles by remember { mutableStateOf(false) }

    // Refresh profiles function - always create fresh instance to ensure proper state
    fun refreshProfiles() {
        android.util.Log.d("ControlsEditorDialog", "Refreshing profiles... showTemplates=$showTemplates, showGameLockedProfiles=$showGameLockedProfiles")

        val manager = InputControlsManager(context)
        profiles = when {
            showTemplates -> {
                // Show only templates
                manager.getProfiles(false).filter { it.isTemplate() }
            }
            showGameLockedProfiles -> {
                // Show all profiles (global + game-locked)
                manager.getProfiles(false).filter { !it.isTemplate() }
            }
            else -> {
                // Default: show only global profiles (exclude templates and game-locked)
                manager.globalProfiles
            }
        }

        android.util.Log.d("ControlsEditorDialog", "Loaded ${profiles.size} profiles:")

        // Log each profile with its file path to identify duplicates
        profiles.forEach { profile ->
            val file = ControlsProfile.getProfileFile(context, profile.id)
            android.util.Log.d("ControlsEditorDialog", "  - Profile: name='${profile.name}', id=${profile.id}, isTemplate=${profile.isTemplate()}, isLocked=${profile.isLockedToGame}, file=${file.name}")
        }
    }

    fun resetTemplateToDefault(profileId: Int) {
        android.util.Log.d("ControlsEditorDialog", "Resetting template with ID $profileId to default...")
        val profilesDir = InputControlsManager.getProfilesDir(context)
        val assetManager = context.assets

        try {
            val filename = "controls-$profileId.icp"
            val targetFile = java.io.File(profilesDir, filename)

            // Delete existing file
            if (targetFile.exists()) {
                targetFile.delete()
                android.util.Log.d("ControlsEditorDialog", "Deleted $filename")
            }

            // Restore fresh copy from assets
            val assetPath = "inputcontrols/profiles/$filename"
            android.util.Log.d("ControlsEditorDialog", "Restoring $filename from assets to ${targetFile.absolutePath}")
            assetManager.open(assetPath).use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            refreshProfiles()
            android.widget.Toast.makeText(
                context,
                "Template reset to default",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            android.util.Log.d("ControlsEditorDialog", "Template $profileId reset successfully")
        } catch (e: Exception) {
            android.util.Log.e("ControlsEditorDialog", "Error resetting template $profileId", e)
            android.widget.Toast.makeText(
                context,
                "Error resetting template",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun ensureTemplatesExist() {
        val profilesDir = InputControlsManager.getProfilesDir(context)
        val assetManager = context.assets

        try {
            // Ensure all 7 template files exist (controls-1 through controls-7)
            listOf("controls-1.icp", "controls-2.icp", "controls-3.icp", "controls-4.icp", "controls-5.icp", "controls-6.icp", "controls-7.icp").forEach { filename ->
                val targetFile = java.io.File(profilesDir, filename)

                // Only restore if file doesn't exist (don't overwrite user edits)
                if (!targetFile.exists()) {
                    android.util.Log.d("ControlsEditorDialog", "Template $filename missing, restoring from assets")
                    val assetPath = "inputcontrols/profiles/$filename"
                    assetManager.open(assetPath).use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ControlsEditorDialog", "Error ensuring templates exist", e)
            e.printStackTrace()
        }
    }

    // Load profiles on first composition and ensure templates exist
    LaunchedEffect(Unit) {
        ensureTemplatesExist()
        refreshProfiles()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Controls Profiles") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            ensureTemplatesExist()
                            refreshProfiles()
                            android.widget.Toast.makeText(
                                context,
                                "Profiles refreshed and missing templates restored",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh List and Restore Templates")
                        }
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Create Profile")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                // Filter controls as items in the scrollable list
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Display mode text
                        Text(
                            text = when {
                                showTemplates -> "Showing Default Templates"
                                showGameLockedProfiles -> "Showing All Profiles (including game-locked)"
                                else -> "Showing Global Profiles"
                            },
                            style = MaterialTheme.typography.titleSmall
                        )

                        // Toggle for templates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Show Templates",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = showTemplates,
                                onCheckedChange = {
                                    showTemplates = it
                                    if (it) showGameLockedProfiles = false // Can't show both at once
                                    refreshProfiles()
                                }
                            )
                        }

                        // Toggle for game-locked profiles (only show when not showing templates)
                        if (!showTemplates) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Show Game-Locked Profiles",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Switch(
                                    checked = showGameLockedProfiles,
                                    onCheckedChange = {
                                        showGameLockedProfiles = it
                                        refreshProfiles()
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider()
                }

                // Profile items
                items(profiles) { profile ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        ProfileListItem(
                            context = context,
                            profile = profile,
                            onEdit = {
                                android.util.Log.d("ControlsEditorDialog", "Edit clicked for profile: ${profile.name} (ID: ${profile.id})")
                                showEditChoiceDialog = profile
                            },
                            onExport = {
                                android.util.Log.d("ControlsEditorDialog", "Export clicked for profile: ${profile.name} (ID: ${profile.id})")
                                try {
                                    val manager = InputControlsManager(context)
                                    val exportedFile = manager.exportProfile(profile)
                                    if (exportedFile != null) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Profile exported to: ${exportedFile.absolutePath}",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Failed to export profile",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ControlsEditorDialog", "Error exporting profile", e)
                                    android.widget.Toast.makeText(
                                        context,
                                        "Error exporting profile: ${e.message}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onDuplicate = {
                                android.util.Log.d("ControlsEditorDialog", "Duplicate clicked for profile: ${profile.name} (ID: ${profile.id})")
                                showDuplicateDialog = profile
                            },
                            onDelete = {
                                android.util.Log.d("ControlsEditorDialog", "Delete clicked for profile: ${profile.name} (ID: ${profile.id})")
                                showDeleteConfirm = profile
                            },
                            onUnlock = {
                                android.util.Log.d("ControlsEditorDialog", "Unlock clicked for profile: ${profile.name} (ID: ${profile.id})")
                                showUnlockConfirm = profile
                            },
                            onRefresh = {
                                android.util.Log.d("ControlsEditorDialog", "Refreshing after lock/unlock")
                                refreshProfiles()
                            },
                            onResetTemplate = {
                                android.util.Log.d("ControlsEditorDialog", "Reset template clicked for: ${profile.name} (ID: ${profile.id})")
                                android.app.AlertDialog.Builder(context)
                                    .setTitle("Reset Template to Default")
                                    .setMessage("Reset \"${profile.name}\" to the default template? All your changes to this template will be lost.")
                                    .setPositiveButton("Reset") { _, _ ->
                                        resetTemplateToDefault(profile.id)
                                    }
                                    .setNegativeButton("Cancel", null)
                                    .show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Create profile dialog
    if (showCreateDialog) {
        app.gamenative.ui.component.dialog.UnifiedProfileCreationDialog(
            context = context,
            container = null, // No container in settings - creating global profiles
            onProfileCreated = { newProfile ->
                android.util.Log.d("ControlsEditorDialog", "Profile created: ${newProfile.name}, refreshing list")
                refreshProfiles()
                showCreateDialog = false
            },
            onDismiss = {
                android.util.Log.d("ControlsEditorDialog", "Create profile dialog dismissed")
                showCreateDialog = false
            }
        )
    }

    // Delete confirmation dialog
    showDeleteConfirm?.let { profile ->
        AlertDialog(
            onDismissRequest = {
                android.util.Log.d("ControlsEditorDialog", "Delete confirmation dismissed for: ${profile.name}")
                showDeleteConfirm = null
            },
            title = { Text("Delete Profile") },
            text = { Text("Are you sure you want to delete \"${profile.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        android.util.Log.d("ControlsEditorDialog", "Deleting profile: ${profile.name} (ID: ${profile.id})")
                        val manager = InputControlsManager(context)
                        manager.getProfiles() // Load profiles first to initialize the list
                        manager.removeProfile(profile)
                        android.util.Log.d("ControlsEditorDialog", "Profile deleted successfully")
                        refreshProfiles()
                        showDeleteConfirm = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    android.util.Log.d("ControlsEditorDialog", "Delete cancelled for: ${profile.name}")
                    showDeleteConfirm = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Unlock confirmation dialog
    showUnlockConfirm?.let { profile ->
        AlertDialog(
            onDismissRequest = {
                android.util.Log.d("ControlsEditorDialog", "Unlock confirmation dismissed for: ${profile.name}")
                showUnlockConfirm = null
            },
            title = { Text("Unlock Profile from Game") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to unlock \"${profile.name}\" from Game #${profile.lockedToContainer}?")
                    Text(
                        "This will make it a global profile available for all games.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        android.util.Log.d("ControlsEditorDialog", "Unlocking profile: ${profile.name} (ID: ${profile.id})")
                        profile.setLockedToContainer(null)
                        profile.save()
                        android.widget.Toast.makeText(
                            context,
                            "Profile \"${profile.name}\" is now available globally",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        refreshProfiles()
                        showUnlockConfirm = null
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    android.util.Log.d("ControlsEditorDialog", "Unlock cancelled for: ${profile.name}")
                    showUnlockConfirm = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Duplicate profile dialog
    showDuplicateDialog?.let { profile ->
        DuplicateProfileDialog(
            originalName = profile.name,
            onDismiss = {
                android.util.Log.d("ControlsEditorDialog", "Duplicate dialog dismissed for: ${profile.name}")
                showDuplicateDialog = null
            },
            onDuplicate = { newName ->
                android.util.Log.d("ControlsEditorDialog", "Duplicating profile: ${profile.name} as $newName")
                val manager = InputControlsManager(context)
                manager.getProfiles() // Load profiles first to initialize the list
                val newProfile = manager.duplicateProfile(profile)
                if (newProfile != null) {
                    android.util.Log.d("ControlsEditorDialog", "Duplicated profile with ID: ${newProfile.id}")
                    if (newName.isNotBlank() && newName != newProfile.name) {
                        newProfile.setName(newName)
                        newProfile.save()
                        android.util.Log.d("ControlsEditorDialog", "Renamed duplicated profile to: $newName")
                    }
                } else {
                    android.util.Log.e("ControlsEditorDialog", "Failed to duplicate profile: ${profile.name}")
                }
                refreshProfiles()
                showDuplicateDialog = null
            }
        )
    }

    // Edit Choice Dialog
    showEditChoiceDialog?.let { profile ->
        AlertDialog(
            onDismissRequest = { showEditChoiceDialog = null },
            title = { Text("Edit Profile: ${profile.name}") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "What would you like to edit?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            android.util.Log.d("ControlsEditorDialog", "Editing on-screen controls for: ${profile.name}")
                            showUnifiedEditor = Pair(profile, 0) // Tab 0 = On-Screen Controls
                            showEditChoiceDialog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Edit On-Screen Controls")
                    }

                    Button(
                        onClick = {
                            android.util.Log.d("ControlsEditorDialog", "Editing physical controller for: ${profile.name}")
                            showUnifiedEditor = Pair(profile, 1) // Tab 1 = Physical Controllers
                            showEditChoiceDialog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Edit Physical Controller")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditChoiceDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Unified Profile Editor Dialog
    showUnifiedEditor?.let { (profile, initialTab) ->
        app.gamenative.ui.component.dialog.UnifiedProfileEditorDialog(
            profile = profile,
            initialTab = initialTab,
            onDismiss = {
                android.util.Log.d("ControlsEditorDialog", "Unified editor dismissed for: ${profile.name}")
                showUnifiedEditor = null
                refreshProfiles()
            },
            onSave = {
                android.util.Log.d("ControlsEditorDialog", "Profile saved: ${profile.name}")
                showUnifiedEditor = null
                refreshProfiles()
            }
        )
    }
}

@Composable
private fun ProfileListItem(
    context: Context,
    profile: ControlsProfile,
    onEdit: () -> Unit,
    onExport: () -> Unit = {},
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onUnlock: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onResetTemplate: () -> Unit = {}
) {
    val isTemplate = profile.isTemplate()
    val isLocked = profile.isLockedToGame
    val usageCount = remember(profile.id) { getContainerUsageCount(context, profile.id) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Profile name with lock icon and badges in one line
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLocked) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Game-locked profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    // Compact badges
                    if (isTemplate) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Template",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Compact info line with all metadata
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID: ${profile.id}${getElementCount(context, profile)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isLocked) {
                        Text(
                            text = "• Game #${profile.lockedToContainer}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (usageCount > 0) {
                        Text(
                            text = "• $usageCount container${if (usageCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Unlock button - only show for locked profiles
                if (isLocked) {
                    IconButton(
                        onClick = onUnlock,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.LockOpen,
                            contentDescription = "Unlock profile (make global)",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Export button - available for all profiles
                IconButton(
                    onClick = onExport,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Export profile",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Edit button - enabled for all profiles including templates
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Show different buttons for templates vs regular profiles
                if (isTemplate) {
                    // Reset button for templates
                    IconButton(
                        onClick = onResetTemplate,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.RestorePage,
                            contentDescription = "Reset template to default",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    // Duplicate and Delete buttons for regular profiles
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.FileCopy,
                            contentDescription = "Duplicate",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateProfileDialog(
    originalName: String,
    onDismiss: () -> Unit,
    onDuplicate: (String) -> Unit
) {
    var profileName by remember { mutableStateOf("$originalName (Copy)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duplicate Profile") },
        text = {
            Column {
                Text(
                    text = "Enter a name for the duplicated profile:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onDuplicate(profileName) },
                enabled = profileName.isNotBlank()
            ) {
                Text("Duplicate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
