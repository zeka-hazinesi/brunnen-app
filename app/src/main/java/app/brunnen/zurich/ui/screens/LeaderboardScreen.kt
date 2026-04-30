package app.brunnen.zurich.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brunnen.zurich.data.AuthClient
import app.brunnen.zurich.data.UserProfile
import app.brunnen.zurich.ui.theme.GoldAccent
import app.brunnen.zurich.ui.theme.NavyDark
import app.brunnen.zurich.ui.theme.TextPrimary
import app.brunnen.zurich.ui.theme.TextSecondary
import app.brunnen.zurich.ui.theme.WaterDark
import app.brunnen.zurich.ui.theme.WaterTeal
import kotlinx.coroutines.launch

@Composable
fun LeaderboardScreen(authClient: AuthClient) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Alle", "Freunde")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    height = 3.dp,
                    color = WaterTeal,
                )
            },
            divider = {},
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            color = if (selectedTab == index) TextPrimary else TextSecondary,
                        )
                    },
                )
            }
        }

        when (selectedTab) {
            0 -> PublicLeaderboard(authClient)
            1 -> FriendsLeaderboard(authClient)
        }
    }
}

@Composable
private fun PublicLeaderboard(authClient: AuthClient) {
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            users = authClient.getLeaderboard()
        } catch (e: Throwable) {
            Log.e("Leaderboard", "Failed to load", e)
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = WaterTeal, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
        }
        return
    }

    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFE2E8F0),
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Noch keine Einträge",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        return
    }

    LeaderboardList(users = users, currentUserId = authClient.currentUserId)
}

@Composable
private fun FriendsLeaderboard(authClient: AuthClient) {
    val scope = rememberCoroutineScope()
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var friendNickname by remember { mutableStateOf("") }
    var addFriendMessage by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            friends = authClient.getFriendsLeaderboard()
        } catch (e: Throwable) {
            Log.e("Leaderboard", "Failed to load friends", e)
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Freund hinzufügen",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = friendNickname,
                        onValueChange = {
                            friendNickname = it
                            addFriendMessage = null
                        },
                        placeholder = { Text("Nickname eingeben", fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WaterTeal,
                            cursorColor = WaterTeal,
                        ),
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isAdding = true
                                addFriendMessage = null
                                try {
                                    val success = authClient.addFriendByNickname(friendNickname.trim())
                                    addFriendMessage = if (success) {
                                        friends = authClient.getFriendsLeaderboard()
                                        friendNickname = ""
                                        "Freund hinzugefügt!"
                                    } else {
                                        "Nickname nicht gefunden."
                                    }
                                } catch (e: Throwable) {
                                    addFriendMessage = "Fehler: ${e.message}"
                                } finally {
                                    isAdding = false
                                }
                            }
                        },
                        enabled = friendNickname.isNotBlank() && !isAdding,
                        colors = ButtonDefaults.buttonColors(containerColor = WaterTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp),
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = "Hinzufügen",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                addFriendMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (msg.startsWith("Freund")) Color(0xFF16A34A) else Color(0xFFDC2626),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WaterTeal, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
            }
        } else if (friends.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color(0xFFE2E8F0),
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Noch keine Freunde",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                    Text(
                        text = "Füge Freunde per Nickname hinzu",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary.copy(alpha = 0.7f),
                    )
                }
            }
        } else {
            LeaderboardList(users = friends, currentUserId = authClient.currentUserId)
        }
    }
}

@Composable
private fun LeaderboardList(users: List<UserProfile>, currentUserId: String?) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column {
                    users.forEachIndexed { index, user ->
                        val rank = index + 1
                        val isCurrentUser = user.uid == currentUserId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isCurrentUser) Modifier.background(WaterTeal.copy(alpha = 0.04f))
                                    else Modifier,
                                )
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Rank badge
                            if (rank <= 3) {
                                val badgeColor = when (rank) {
                                    1 -> GoldAccent
                                    2 -> Color(0xFFA8A29E)
                                    else -> Color(0xFFCD7F32)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(badgeColor.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier.size(36.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "$rank",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.nickname.ifBlank { "Anonym" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "${user.checkInCount} Check-ins",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${user.points}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rank <= 3) WaterDark else WaterTeal,
                                )
                                Text(
                                    text = "Punkte",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                )
                            }
                        }

                        if (index < users.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 68.dp),
                                thickness = 0.5.dp,
                                color = Color(0xFFF1F5F9),
                            )
                        }
                    }
                }
            }
        }
    }
}
