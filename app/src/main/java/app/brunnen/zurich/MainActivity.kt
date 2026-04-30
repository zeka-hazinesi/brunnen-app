package app.brunnen.zurich

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.brunnen.zurich.data.AuthClient
import app.brunnen.zurich.ui.screens.CompassScreen
import app.brunnen.zurich.ui.screens.FountainListScreen
import app.brunnen.zurich.ui.screens.LeaderboardScreen
import app.brunnen.zurich.ui.screens.LoginScreen
import app.brunnen.zurich.ui.screens.MapScreen
import app.brunnen.zurich.ui.screens.NicknameScreen
import app.brunnen.zurich.ui.theme.BackgroundBottom
import app.brunnen.zurich.ui.theme.BackgroundMid
import app.brunnen.zurich.ui.theme.BackgroundTop
import app.brunnen.zurich.ui.theme.NavyDark
import app.brunnen.zurich.ui.theme.TextSecondary
import app.brunnen.zurich.ui.theme.WaterDark
import app.brunnen.zurich.ui.theme.WaterTeal

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
                CircularProgressIndicator(
                    color = WaterTeal,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp),
                )
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
                containerColor = Color.White,
                tonalElevation = 0.dp,
                modifier = Modifier.shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    clip = false,
                    spotColor = Color.Black.copy(alpha = 0.08f),
                ),
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(if (selectedTab == index) 26.dp else 24.dp),
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WaterTeal,
                            selectedTextColor = WaterDark,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = WaterTeal.copy(alpha = 0.12f),
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
                    authClient = authClient,
                )
                2 -> FountainListScreen(
                    hasLocationPermission = hasLocationPermission,
                    authClient = authClient,
                )
                3 -> LeaderboardScreen(
                    authClient = authClient,
                )
            }
        }
    }
}
