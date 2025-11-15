package app.gamenative.ui.component.dialog

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NoteAdd
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
 * Dialog for creating a blank profile
 *
 * Features:
 * - Input field for new profile name
 * - Auto-generates name based on container context
 * - Creates empty profile locked to the game
 */
@Composable
fun CreateBlankProfileDialog(
    context: Context,
    container: Container,
    onProfileCreated: (ControlsProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val inputControlsManager = remember { InputControlsManager(context) }

    // Auto-generate profile name
    val defaultProfileName = remember {
        // Find next available number
        val allExistingProfiles = inputControlsManager.getProfiles(false)
        val baseName = InputControlsManager.sanitizeProfileName("${container.name} Profile")
        var finalName = baseName
        var counter = 2

        while (allExistingProfiles.any { it.name == finalName }) {
            finalName = InputControlsManager.sanitizeProfileName("$baseName $counter")
            counter++
        }
        finalName
    }
    var profileName by remember { mutableStateOf(defaultProfileName) }

    var isCreating by remember { mutableStateOf(false) }

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
                        Icons.Default.NoteAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Create Blank Profile",
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

                Text(
                    text = "Create a new empty profile to customize from scratch",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Profile name input
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isCreating
                )

                Text(
                    text = "Profile will be locked to ${container.name}",
                    style = MaterialTheme.typography.bodySmall,
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
                        enabled = !isCreating
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val name = profileName.trim()

                            // Validation
                            val validationError = InputControlsManager.validateProfileName(name)
                            if (validationError != null) {
                                Toast.makeText(context, validationError, Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isCreating = true

                            try {
                                // Create blank profile
                                val newProfile = inputControlsManager.createProfile(name)

                                // Set lock status to current container
                                newProfile.setLockedToContainer(container.id.toString())
                                newProfile.save()

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
                                    "Failed to create profile: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                isCreating = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isCreating && profileName.trim().isNotEmpty()
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }
}
