package app.gamenative.ui.component.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.winlator.inputcontrols.ControlsProfile

/**
 * Gamer-friendly sensitivity and dead zone settings section
 *
 * Provides intuitive sliders with real-time preview and preset options
 */
@Composable
fun SensitivitySettingsSection(
    profile: ControlsProfile,
    isPhysicalController: Boolean,
    onSettingsChanged: () -> Unit
) {
    val title = if (isPhysicalController) "Physical Controller" else "On-Screen Controls"
    val icon = if (isPhysicalController) Icons.Default.Gamepad else Icons.Default.TouchApp

    // Current values
    var stickDeadZone by remember {
        mutableFloatStateOf(
            if (isPhysicalController) profile.physicalStickDeadZone
            else profile.virtualStickDeadZone
        )
    }
    var stickSensitivity by remember {
        mutableFloatStateOf(
            if (isPhysicalController) profile.physicalStickSensitivity
            else profile.virtualStickSensitivity
        )
    }
    var dpadDeadZone by remember {
        mutableFloatStateOf(
            if (isPhysicalController) profile.physicalDpadDeadZone
            else profile.virtualDpadDeadZone
        )
    }

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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, title)
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Reset to defaults button
                IconButton(
                    onClick = {
                        stickDeadZone = 0.15f
                        stickSensitivity = 3.0f
                        dpadDeadZone = if (isPhysicalController) 0.15f else 0.3f

                        if (isPhysicalController) {
                            profile.physicalStickDeadZone = stickDeadZone
                            profile.physicalStickSensitivity = stickSensitivity
                            profile.physicalDpadDeadZone = dpadDeadZone
                        } else {
                            profile.virtualStickDeadZone = stickDeadZone
                            profile.virtualStickSensitivity = stickSensitivity
                            profile.virtualDpadDeadZone = dpadDeadZone
                        }
                        profile.save()
                        onSettingsChanged()
                    }
                ) {
                    Icon(Icons.Default.RestartAlt, "Reset to Defaults")
                }
            }

            HorizontalDivider()

            // Stick Dead Zone
            SettingSlider(
                label = "Stick Dead Zone",
                value = stickDeadZone,
                valueRange = 0f..0.5f,
                steps = 49,
                valueText = "${(stickDeadZone * 100).toInt()}%",
                description = "Ignore small stick movements (prevents drift)",
                onValueChange = { newValue ->
                    stickDeadZone = newValue
                    if (isPhysicalController) {
                        profile.physicalStickDeadZone = newValue
                    } else {
                        profile.virtualStickDeadZone = newValue
                    }
                    profile.save()
                    onSettingsChanged()
                }
            )

            // Stick Sensitivity
            SettingSlider(
                label = "Stick Sensitivity",
                value = stickSensitivity,
                valueRange = 0.5f..5.0f,
                steps = 44,
                valueText = "${"%.1f".format(stickSensitivity)}x",
                description = "How responsive the stick feels (higher = faster)",
                onValueChange = { newValue ->
                    stickSensitivity = newValue
                    if (isPhysicalController) {
                        profile.physicalStickSensitivity = newValue
                    } else {
                        profile.virtualStickSensitivity = newValue
                    }
                    profile.save()
                    onSettingsChanged()
                }
            )

            // D-Pad Dead Zone
            SettingSlider(
                label = "D-Pad Dead Zone",
                value = dpadDeadZone,
                valueRange = 0f..0.5f,
                steps = 49,
                valueText = "${(dpadDeadZone * 100).toInt()}%",
                description = "Minimum movement before D-pad activates",
                onValueChange = { newValue ->
                    dpadDeadZone = newValue
                    if (isPhysicalController) {
                        profile.physicalDpadDeadZone = newValue
                    } else {
                        profile.virtualDpadDeadZone = newValue
                    }
                    profile.save()
                    onSettingsChanged()
                }
            )

            // Quick presets
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetButton(
                    text = "Tight",
                    modifier = Modifier.weight(1f, fill = true),
                    onClick = {
                        stickDeadZone = 0.05f
                        stickSensitivity = 4.0f
                        dpadDeadZone = 0.1f
                        applySettings(profile, isPhysicalController, stickDeadZone, stickSensitivity, dpadDeadZone)
                        onSettingsChanged()
                    }
                )
                PresetButton(
                    text = "Balanced",
                    modifier = Modifier.weight(1f, fill = true),
                    onClick = {
                        stickDeadZone = 0.15f
                        stickSensitivity = 3.0f
                        dpadDeadZone = if (isPhysicalController) 0.15f else 0.3f
                        applySettings(profile, isPhysicalController, stickDeadZone, stickSensitivity, dpadDeadZone)
                        onSettingsChanged()
                    }
                )
                PresetButton(
                    text = "Smooth",
                    modifier = Modifier.weight(1f, fill = true),
                    onClick = {
                        stickDeadZone = 0.25f
                        stickSensitivity = 2.0f
                        dpadDeadZone = 0.35f
                        applySettings(profile, isPhysicalController, stickDeadZone, stickSensitivity, dpadDeadZone)
                        onSettingsChanged()
                    }
                )
            }
        }
    }
}

private fun applySettings(
    profile: ControlsProfile,
    isPhysical: Boolean,
    deadZone: Float,
    sensitivity: Float,
    dpadDeadZone: Float
) {
    if (isPhysical) {
        profile.physicalStickDeadZone = deadZone
        profile.physicalStickSensitivity = sensitivity
        profile.physicalDpadDeadZone = dpadDeadZone
    } else {
        profile.virtualStickDeadZone = deadZone
        profile.virtualStickSensitivity = sensitivity
        profile.virtualDpadDeadZone = dpadDeadZone
    }
    profile.save()
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    description: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PresetButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
