package app.brunnen.zurich.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import app.brunnen.zurich.data.AuthClient
import app.brunnen.zurich.ui.theme.TextPrimary
import app.brunnen.zurich.ui.theme.TextSecondary
import app.brunnen.zurich.ui.theme.WaterDark
import app.brunnen.zurich.ui.theme.WaterTeal
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
            .padding(horizontal = 28.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(16.dp, CircleShape, spotColor = WaterTeal.copy(alpha = 0.3f))
                .background(
                    brush = Brush.linearGradient(listOf(WaterTeal, WaterDark)),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp),
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Trinkbrunnen",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            letterSpacing = (-0.5).sp,
        )
        Text(
            text = "Zürich",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Light,
            color = WaterTeal,
            letterSpacing = (-0.5).sp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Entdecke die Trinkbrunnen deiner Stadt",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(40.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
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
                    lineHeight = 22.sp,
                )

                errorMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFDC2626),
                        )
                    }
                }

                Button(
                    onClick = {
                        doSignIn { authClient.signInWithBottomSheet(it) }
                    },
                    enabled = !isWorking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextPrimary,
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 0.dp,
                    ),
                ) {
                    if (isWorking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            "Anmeldung läuft...",
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        Text(
                            text = "G",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = "Mit Google anmelden",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
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
                    Text(
                        "Alternativer Google-Login",
                        color = TextSecondary,
                        fontSize = 13.sp,
                    )
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
