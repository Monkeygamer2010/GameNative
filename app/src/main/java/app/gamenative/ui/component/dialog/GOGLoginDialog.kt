package app.gamenative.ui.component.dialog

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.gamenative.service.gog.GOGConstants
import app.gamenative.ui.theme.PluviaTheme
import android.content.Intent
import android.net.Uri

/**
 * GOG Login Dialog
 *
 * GOG uses OAuth2 authentication with automatic callback handling:
 * 1. Open GOG login URL in browser
 * 2. Login with GOG credentials
 * 3. GOG redirects back to app with authorization code automatically
 */
@Composable
fun GOGLoginDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onAuthCodeClick: (authCode: String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
) {
    val context = LocalContext.current
    var authCode by rememberSaveable { mutableStateOf("") }

    if (visible) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(imageVector = Icons.Default.Login, contentDescription = null) },
            title = { Text("Sign in to GOG") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Instructions
                    Text(
                        text = "Sign in with your GOG account:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Tap 'Open GOG Login' and sign in. The app will automatically receive your authorization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Open browser button
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GOGConstants.GOG_AUTH_LOGIN_URL))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Browser not available
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open GOG Login")
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Manual code entry fallback
                    Text(
                        text = "Or manually paste authorization code:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Authorization code input
                    OutlinedTextField(
                        value = authCode,
                        onValueChange = { authCode = it.trim() },
                        label = { Text("Authorization Code (optional)") },
                        placeholder = { Text("Paste code here if needed...") },
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Error message
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Loading indicator
                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (authCode.isNotBlank()) {
                            onAuthCodeClick(authCode)
                        }
                    },
                    enabled = !isLoading && authCode.isNotBlank()
                ) {
                    Text("Login")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissRequest,
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_GOGLoginDialog() {
    PluviaTheme {
        GOGLoginDialog(
            visible = true,
            onDismissRequest = {},
            onAuthCodeClick = {},
            isLoading = false,
            errorMessage = null
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_GOGLoginDialogWithError() {
    PluviaTheme {
        GOGLoginDialog(
            visible = true,
            onDismissRequest = {},
            onAuthCodeClick = {},
            isLoading = false,
            errorMessage = "Invalid authorization code. Please try again."
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_GOGLoginDialogLoading() {
    PluviaTheme {
        GOGLoginDialog(
            visible = true,
            onDismissRequest = {},
            onAuthCodeClick = {},
            isLoading = true,
            errorMessage = null
        )
    }
}
