package app.brunnen.zurich.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brunnen.zurich.data.AuthClient
import app.brunnen.zurich.ui.theme.NavyDark
import app.brunnen.zurich.ui.theme.TextPrimary
import app.brunnen.zurich.ui.theme.TextSecondary
import app.brunnen.zurich.ui.theme.WaterDark
import app.brunnen.zurich.ui.theme.WaterTeal
import kotlinx.coroutines.launch

@Composable
fun NicknameScreen(
    authClient: AuthClient,
    onNicknameSet: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var nickname by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isValid = nickname.length in 3..20 && nickname.matches(Regex("^[a-zA-Z0-9_]+$"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(12.dp, CircleShape, spotColor = WaterTeal.copy(alpha = 0.25f))
                .background(
                    brush = Brush.linearGradient(listOf(WaterTeal, WaterDark)),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Fast geschafft!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Wähle einen Nickname für die Rangliste",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = {
                        nickname = it.take(20)
                        errorMessage = null
                    },
                    label = { Text("Nickname") },
                    placeholder = { Text("z.B. waterwalker42", color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WaterTeal,
                        focusedLabelColor = WaterTeal,
                        cursorColor = WaterTeal,
                    ),
                )

                Text(
                    text = "3–20 Zeichen: Buchstaben, Zahlen, Unterstrich",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                errorMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFDC2626),
                        )
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            isWorking = true
                            errorMessage = null
                            try {
                                if (!authClient.isNicknameAvailable(nickname)) {
                                    errorMessage = "Dieser Nickname ist bereits vergeben."
                                } else {
                                    authClient.saveNickname(nickname)
                                    onNicknameSet()
                                }
                            } catch (e: Throwable) {
                                Log.e("NicknameScreen", "Save failed", e)
                                errorMessage = e.message ?: "Fehler beim Speichern."
                            } finally {
                                isWorking = false
                            }
                        }
                    },
                    enabled = isValid && !isWorking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyDark,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(14.dp),
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
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text(
                        if (isWorking) "Wird gespeichert..." else "Los geht's!",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}
