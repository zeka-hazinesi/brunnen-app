package com.example.brunnenapp.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.example.brunnenapp.data.AuthClient
import com.example.brunnenapp.ui.theme.TextPrimary
import com.example.brunnenapp.ui.theme.TextSecondary
import com.example.brunnenapp.ui.theme.WaterDark
import com.example.brunnenapp.ui.theme.WaterTeal
import kotlinx.coroutines.launch

private const val Tag = "LoginScreen"

@Composable
fun LoginScreen(
    authClient: AuthClient,
    onSignedIn: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    var isWorking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun doSignIn(block: suspend (Activity) -> Unit) {
        val currentActivity = activity ?: return
        scope.launch {
            isWorking = true
            errorMessage = null
            try {
                block(currentActivity)
                onSignedIn()
            } catch (e: Throwable) {
                Log.e(Tag, "Sign-in failed", e)
                errorMessage = e.toUserMessage()
            } finally {
                isWorking = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = WaterDark),
            shape = RoundedCornerShape(22.dp),
        ) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\uD83D\uDEB0",
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Trinkbrunnen Zürich",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Entdecke die Trinkbrunnen deiner Stadt",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFC)),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Willkommen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(
                    text = "Melde dich mit deinem Google-Konto an, um Brunnen zu entdecken, einzuchecken und Punkte zu sammeln.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )

                errorMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB42318),
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        doSignIn { authClient.signInWithBottomSheet(it) }
                    },
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF111827),
                    ),
                ) {
                    if (isWorking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = WaterTeal,
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text("Anmeldung läuft...")
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "G",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A73E8),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = "Mit Google anmelden",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                TextButton(
                    onClick = {
                        doSignIn { authClient.signInWithGoogleButton(it) }
                    },
                    enabled = !isWorking,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Alternativer Google-Login")
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun Throwable.toUserMessage(): String {
    return when (this) {
        is NoCredentialException -> "Kein passendes Google-Konto gefunden."
        is GetCredentialCancellationException -> "Die Anmeldung wurde abgebrochen."
        is GetCredentialProviderConfigurationException -> "Credential-Provider nicht korrekt konfiguriert."
        is GetCredentialUnknownException -> "Unbekannter Fehler beim Credential Manager."
        else -> message ?: javaClass.simpleName
    }
}
