package com.example.brunnenapp.ui.screens

import android.util.Log
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.brunnenapp.data.AuthClient
import com.example.brunnenapp.data.UserProfile
import com.example.brunnenapp.ui.theme.GoldAccent
import com.example.brunnenapp.ui.theme.NavyDark
import com.example.brunnenapp.ui.theme.TextPrimary
import com.example.brunnenapp.ui.theme.TextSecondary
import com.example.brunnenapp.ui.theme.WaterTeal
import kotlinx.coroutines.launch

@Composable
fun LeaderboardScreen(authClient: AuthClient) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Public", "Freunde")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = WaterTeal,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = WaterTeal,
                )
            },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
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
            CircularProgressIndicator(color = WaterTeal)
        }
        return
    }

    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Noch keine Einträge.", color = TextSecondary)
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Add friend card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Freund hinzufügen",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = friendNickname,
                        onValueChange = {
                            friendNickname = it
                            addFriendMessage = null
                        },
                        label = { Text("Nickname") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
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
                        color = if (msg.startsWith("Freund")) Color(0xFF2E7D32) else Color(0xFFB42318),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WaterTeal)
            }
        } else if (friends.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Noch keine Freunde hinzugefügt.\nFüge Freunde per Nickname hinzu!",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LeaderboardList(users = friends, currentUserId = authClient.currentUserId)
        }
    }
}

@Composable
private fun LeaderboardList(users: List<UserProfile>, currentUserId: String?) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(users, key = { _, user -> user.uid }) { index, user ->
            val rank = index + 1
            val isCurrentUser = user.uid == currentUserId

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isCurrentUser -> Color(0xFFE0F7FA)
                        rank <= 3 -> Color(0xFFFFF8E1)
                        else -> Color.White.copy(alpha = 0.92f)
                    },
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Rank
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (rank <= 3) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = when (rank) {
                                    1 -> GoldAccent
                                    2 -> Color(0xFFBDBDBD)
                                    else -> Color(0xFFCD7F32)
                                },
                                modifier = Modifier.size(28.dp),
                            )
                        } else {
                            Text(
                                text = "#$rank",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.nickname.ifBlank { "Anonym" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Text(
                            text = "${user.checkInCount} Check-ins",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }

                    Text(
                        text = "${user.points}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WaterTeal,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pkt",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}
