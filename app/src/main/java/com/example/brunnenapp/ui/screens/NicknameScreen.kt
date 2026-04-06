package com.example.brunnenapp.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.brunnenapp.data.AuthClient
import com.example.brunnenapp.ui.theme.NavyDark
import com.example.brunnenapp.ui.theme.TextPrimary
import com.example.brunnenapp.ui.theme.TextSecondary
import com.example.brunnenapp.ui.theme.WaterTeal
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
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFC)),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Wähle deinen Nickname",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )

                Text(
                    text = "Dein Nickname wird im Leaderboard und für Freunde angezeigt. Er muss 3–20 Zeichen lang sein (Buchstaben, Zahlen, Unterstrich).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = {
                        nickname = it.take(20)
                        errorMessage = null
                    },
                    label = { Text("Nickname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )

                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB42318),
                    )
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyDark,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    if (isWorking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = WaterTeal,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text(if (isWorking) "Wird gespeichert..." else "Los geht's!")
                }
            }
        }
    }
}
