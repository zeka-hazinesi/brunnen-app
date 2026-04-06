package com.example.brunnenapp

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.example.brunnenapp.data.AuthClient
import com.example.brunnenapp.ui.screens.CompassScreen
import com.example.brunnenapp.ui.screens.FountainListScreen
import com.example.brunnenapp.ui.screens.LeaderboardScreen
import com.example.brunnenapp.ui.screens.LoginScreen
import com.example.brunnenapp.ui.screens.MapScreen
import com.example.brunnenapp.ui.screens.NicknameScreen
import com.example.brunnenapp.ui.theme.BackgroundBottom
import com.example.brunnenapp.ui.theme.BackgroundMid
import com.example.brunnenapp.ui.theme.BackgroundTop
import com.example.brunnenapp.ui.theme.WaterDark
import com.example.brunnenapp.ui.theme.WaterTeal

private const val Tag = "MainActivity"

class MainActivity : ComponentActivity() {

    private lateinit var authClient: AuthClient

    private val locationPermissionState = mutableStateOf(false)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        locationPermissionState.value = permissions.values.any { it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authClient = AuthClient(applicationContext)
        checkLocationPermission()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(BackgroundTop, BackgroundMid, BackgroundBottom),
                                ),
                            ),
                    ) {
                        AppRoot(
                            authClient = authClient,
                            hasLocationPermission = locationPermissionState.value,
                            onRequestLocationPermission = { requestLocationPermission() },
                        )
                    }
                }
            }
        }
    }

    private fun checkLocationPermission() {
        locationPermissionState.value = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }
}

private enum class AppScreen {
    LOADING,
    LOGIN,
    NICKNAME,
    MAIN,
}

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem("Kompass", Icons.Default.Explore),
    BottomNavItem("Karte", Icons.Default.Map),
    BottomNavItem("Brunnen", Icons.Default.WaterDrop),
    BottomNavItem("Rangliste", Icons.Default.EmojiEvents),
)

@Composable
private fun AppRoot(
    authClient: AuthClient,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
) {
    var screen by remember { mutableStateOf(AppScreen.LOADING) }

    LaunchedEffect(Unit) {
        screen = if (authClient.restoreSession() != null) {
            try {
                if (authClient.hasNickname()) AppScreen.MAIN else AppScreen.NICKNAME
            } catch (e: Throwable) {
                Log.e(Tag, "Nickname check failed", e)
                AppScreen.MAIN
            }
        } else {
            AppScreen.LOGIN
        }
    }

    when (screen) {
        AppScreen.LOADING -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WaterTeal)
            }
        }

        AppScreen.LOGIN -> {
            LoginScreen(
                authClient = authClient,
                onSignedIn = {
                    screen = AppScreen.NICKNAME
                },
            )
        }

        AppScreen.NICKNAME -> {
            LaunchedEffect(Unit) {
                try {
                    if (authClient.hasNickname()) {
                        screen = AppScreen.MAIN
                    }
                } catch (_: Throwable) {}
            }

            NicknameScreen(
                authClient = authClient,
                onNicknameSet = {
                    screen = AppScreen.MAIN
                },
            )
        }

        AppScreen.MAIN -> {
            MainContent(
                authClient = authClient,
                hasLocationPermission = hasLocationPermission,
                onRequestLocationPermission = onRequestLocationPermission,
            )
        }
    }
}

@Composable
private fun MainContent(
    authClient: AuthClient,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = Color.White.copy(alpha = 0.95f),
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(item.icon, contentDescription = item.label)
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WaterTeal,
                            selectedTextColor = WaterDark,
                            indicatorColor = Color(0xFFE0F7FA),
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (selectedTab) {
                0 -> CompassScreen(
                    authClient = authClient,
                    hasLocationPermission = hasLocationPermission,
                    onRequestPermission = onRequestLocationPermission,
                )
                1 -> MapScreen(
                    hasLocationPermission = hasLocationPermission,
                )
                2 -> FountainListScreen(
                    hasLocationPermission = hasLocationPermission,
                )
                3 -> LeaderboardScreen(
                    authClient = authClient,
                )
            }
        }
    }
}
