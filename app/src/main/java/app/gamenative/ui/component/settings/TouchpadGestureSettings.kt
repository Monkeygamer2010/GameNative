package app.gamenative.ui.component.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.winlator.inputcontrols.ControlsProfile

/**
 * Touchpad gesture settings for mouse input
 *
 * Configures touch gestures for mouse clicks:
 * - Single tap = Left click
 * - Two-finger tap/hold = Right click
 */
@Composable
fun TouchpadGestureSettings(
    profile: ControlsProfile,
    onSettingsChanged: () -> Unit
) {
    // State for gesture settings
    var enableTapToClick by remember { mutableStateOf(profile.isEnableTapToClick) }
    var enableTwoFingerRightClick by remember { mutableStateOf(profile.isEnableTwoFingerRightClick) }

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
                Icon(Icons.Default.TouchApp, "Touchpad Gestures")
                Text(
                    text = "Touchpad Gestures",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            Text(
                text = "Configure touch gestures for mouse input when using touchpad area (empty screen space)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Tap to Click
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tap to Click",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Single tap triggers left mouse click",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enableTapToClick,
                    onCheckedChange = { newValue ->
                        enableTapToClick = newValue
                        profile.setEnableTapToClick(newValue)
                        profile.save()
                        onSettingsChanged()
                    }
                )
            }

            // Two-Finger Right Click
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Two-Finger Right Click",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Two-finger tap/hold triggers right mouse click",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enableTwoFingerRightClick,
                    onCheckedChange = { newValue ->
                        enableTwoFingerRightClick = newValue
                        profile.setEnableTwoFingerRightClick(newValue)
                        profile.save()
                        onSettingsChanged()
                    }
                )
            }
        }
    }
}
