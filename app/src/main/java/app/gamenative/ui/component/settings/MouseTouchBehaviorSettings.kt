package app.gamenative.ui.component.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.winlator.inputcontrols.ControlsProfile

/**
 * Mouse and touch behavior settings
 *
 * Controls how mouse input and touch mode work:
 * - Disable Touchpad Mouse: Prevents touch input from controlling the mouse cursor
 * - Touchscreen Mode: Enables direct touch interaction with games
 */
@Composable
fun MouseTouchBehaviorSettings(
    profile: ControlsProfile,
    onSettingsChanged: () -> Unit
) {
    // State for mouse/touch settings
    var disableTouchpadMouse by remember { mutableStateOf(profile.isDisableTouchpadMouse) }
    var touchscreenMode by remember { mutableStateOf(profile.isTouchscreenMode) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Mouse, "Mouse & Touch Behavior")
                Text(
                    text = "Mouse & Touch Behavior",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            Text(
                text = "Configure how mouse and touch input behave when playing games",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Disable Touchpad Mouse
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Disable Touchpad Mouse",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Prevent touchpad area from controlling mouse cursor (on-screen controls still work)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = disableTouchpadMouse,
                    onCheckedChange = { newValue ->
                        disableTouchpadMouse = newValue
                        profile.setDisableTouchpadMouse(newValue)
                        profile.save()
                        onSettingsChanged()
                    }
                )
            }

            // Touchscreen Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Touchscreen Mode",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Enable direct touch interaction with games (touch translates to screen coordinates)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = touchscreenMode,
                    onCheckedChange = { newValue ->
                        touchscreenMode = newValue
                        profile.setTouchscreenMode(newValue)
                        profile.save()
                        onSettingsChanged()
                    }
                )
            }
        }
    }
}
