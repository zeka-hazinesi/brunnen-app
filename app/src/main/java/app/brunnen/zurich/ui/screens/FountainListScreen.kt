package app.brunnen.zurich.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brunnen.zurich.data.AuthClient
import app.brunnen.zurich.data.Fountain
import app.brunnen.zurich.data.FountainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.brunnen.zurich.ui.theme.SuccessGreen
import app.brunnen.zurich.ui.theme.TextPrimary
import app.brunnen.zurich.ui.theme.TextSecondary
import app.brunnen.zurich.ui.theme.WaterTeal
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.util.Log
import kotlin.math.roundToInt

private const val PageSize = 25

@Composable
fun FountainListScreen(hasLocationPermission: Boolean, authClient: AuthClient) {
    val context = LocalContext.current
    var fountains by remember { mutableStateOf<List<Fountain>>(emptyList()) }
    var sortedFountains by remember { mutableStateOf<List<Pair<Fountain, Float>>>(emptyList()) }
    var selectedFountain by remember { mutableStateOf<Pair<Fountain, Float>?>(null) }
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }

    var visitedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showUnvisitedOnly by remember { mutableStateOf(false) }
    var visibleCount by remember { mutableIntStateOf(PageSize) }

    LaunchedEffect(Unit) {
        fountains = withContext(Dispatchers.IO) { FountainRepository.loadFountains(context) }
        try {
            visitedIds = withContext(Dispatchers.IO) { authClient.getVisitedFountainIds() }
        } catch (e: Throwable) {
            Log.w("FountainList", "Failed to load visited ids", e)
        }
    }

    DisposableEffect(hasLocationPermission, fountains) {
        if (fountains.isEmpty()) return@DisposableEffect onDispose {}
        if (!hasLocationPermission) {
            sortedFountains = fountains.map { it to Float.MAX_VALUE }
            return@DisposableEffect onDispose {}
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000L).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                userLat = loc.latitude
                userLng = loc.longitude
                sortedFountains = FountainRepository.sortedByDistance(fountains, loc.latitude, loc.longitude)
            }
        }

        try {
            fusedClient.requestLocationUpdates(locationRequest, callback, context.mainLooper)
        } catch (e: SecurityException) {
            Log.w("FountainList", "Location permission missing", e)
            sortedFountains = fountains.map { it to Float.MAX_VALUE }
        }

        onDispose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    if (selectedFountain != null) {
        FountainDetailScreen(
            fountain = selectedFountain!!.first,
            distance = selectedFountain!!.second,
            isVisited = selectedFountain!!.first.id in visitedIds,
            onBack = { selectedFountain = null },
        )
        return
    }

    if (sortedFountains.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = WaterTeal,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Brunnen werden geladen...",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        return
    }

    val filteredFountains by remember(sortedFountains, showUnvisitedOnly, visitedIds) {
        derivedStateOf {
            if (showUnvisitedOnly) {
                sortedFountains.filter { it.first.id !in visitedIds }
            } else {
                sortedFountains
            }
        }
    }

    val displayedFountains by remember(filteredFountains, visibleCount) {
        derivedStateOf { filteredFountains.take(visibleCount) }
    }

    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= visibleCount - 5 && visibleCount < filteredFountains.size
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            visibleCount += PageSize
        }
    }

    // Reset pagination when filter changes
    LaunchedEffect(showUnvisitedOnly) {
        visibleCount = PageSize
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${filteredFountains.size}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showUnvisitedOnly) "von ${sortedFountains.size} Brunnen" else "Brunnen",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Normal,
                    )
                }

                FilterChip(
                    selected = showUnvisitedOnly,
                    onClick = { showUnvisitedOnly = !showUnvisitedOnly },
                    label = {
                        Text(
                            "Nicht besucht",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    leadingIcon = if (showUnvisitedOnly) {
                        {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WaterTeal.copy(alpha = 0.12f),
                        selectedLabelColor = WaterTeal,
                        selectedLeadingIconColor = WaterTeal,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color(0xFFE2E8F0),
                        selectedBorderColor = WaterTeal.copy(alpha = 0.3f),
                        enabled = true,
                        selected = showUnvisitedOnly,
                    ),
                )
            }
        }

        items(displayedFountains, key = { it.first.id }) { (fountain, distance) ->
            val isVisited = fountain.id in visitedIds
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
            ) {
                FountainListItem(
                    fountain = fountain,
                    distance = distance,
                    isVisited = isVisited,
                    onClick = { selectedFountain = fountain to distance },
                )
            }
        }

        if (visibleCount < filteredFountains.size) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = WaterTeal,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FountainListItem(
    fountain: Fountain,
    distance: Float,
    isVisited: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(
                    color = if (fountain.isTrinkwasser) WaterTeal.copy(alpha = 0.08f) else Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(13.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = if (fountain.isTrinkwasser) WaterTeal else Color(0xFFF59E0B),
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fountain.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isVisited) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = "Besucht",
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Medium,
                    )
                    Text("·", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Text(
                    text = if (fountain.isTrinkwasser) "Trinkwasser" else "Kein Trinkwasser",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (fountain.isTrinkwasser) SuccessGreen else Color(0xFFD97706),
                    fontWeight = FontWeight.Medium,
                )
                if (fountain.isAbgestellt) {
                    Text("·", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = "Abgestellt",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        if (distance < Float.MAX_VALUE) {
            val distText = if (distance < 1000) {
                "${distance.roundToInt()}m"
            } else {
                "${"%.1f".format(distance / 1000)}km"
            }
            Text(
                text = distText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = WaterTeal,
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(20.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FountainDetailScreen(
    fountain: Fountain,
    distance: Float,
    isVisited: Boolean,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = TextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    text = fountain.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 30.sp,
                )
                if (fountain.quartier != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = fountain.quartier,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                }
                if (isVisited) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Besucht",
                            style = MaterialTheme.typography.labelMedium,
                            color = SuccessGreen,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    fountain.location?.let {
                        DetailRow(label = "Ort", value = it)
                    }
                    fountain.baujahr?.let {
                        DetailRow(label = "Baujahr", value = it.toString())
                    }
                    DetailRow(
                        label = "Trinkwasser",
                        value = if (fountain.isTrinkwasser) "Ja" else "Nein",
                        valueColor = if (fountain.isTrinkwasser) SuccessGreen else Color(0xFFD97706),
                    )
                    fountain.wasserart?.let {
                        DetailRow(label = "Wasserart", value = it)
                    }
                    fountain.brunnenNummer?.let {
                        DetailRow(label = "Brunnen-Nr.", value = it)
                    }
                    if (fountain.isAbgestellt) {
                        DetailRow(label = "Status", value = "Abgestellt", valueColor = Color(0xFFDC2626))
                    }
                    if (distance < Float.MAX_VALUE) {
                        val distText = if (distance < 1000) {
                            "${distance.roundToInt()}m"
                        } else {
                            "${"%.1f".format(distance / 1000)}km"
                        }
                        DetailRow(label = "Entfernung", value = distText, valueColor = WaterTeal)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(WaterTeal.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = WaterTeal,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${"%.6f".format(fountain.latitude)}, ${"%.6f".format(fountain.longitude)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor,
        )
    }
}
