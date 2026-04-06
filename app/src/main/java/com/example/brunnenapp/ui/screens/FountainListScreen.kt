package com.example.brunnenapp.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.brunnenapp.data.Fountain
import com.example.brunnenapp.data.FountainRepository
import com.example.brunnenapp.ui.theme.SuccessGreen
import com.example.brunnenapp.ui.theme.TextPrimary
import com.example.brunnenapp.ui.theme.TextSecondary
import com.example.brunnenapp.ui.theme.WaterTeal
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.util.Log
import kotlin.math.roundToInt

@Composable
fun FountainListScreen(hasLocationPermission: Boolean) {
    val context = LocalContext.current
    val fountains = remember { FountainRepository.loadFountains(context) }
    var sortedFountains by remember { mutableStateOf<List<Pair<Fountain, Float>>>(emptyList()) }
    var selectedFountain by remember { mutableStateOf<Pair<Fountain, Float>?>(null) }
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }

    // Location updates
    DisposableEffect(hasLocationPermission) {
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
                CircularProgressIndicator(color = WaterTeal)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Brunnen werden geladen...", color = TextSecondary)
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "${sortedFountains.size} Brunnen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        items(sortedFountains, key = { it.first.id }) { (fountain, distance) ->
            FountainListItem(
                fountain = fountain,
                distance = distance,
                onClick = { selectedFountain = fountain to distance },
            )
        }
    }
}

@Composable
private fun FountainListItem(
    fountain: Fountain,
    distance: Float,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Water icon as thumbnail placeholder
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (fountain.isTrinkwasser) Color(0xFFE0F7FA) else Color(0xFFFFF3E0),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = if (fountain.isTrinkwasser) WaterTeal else Color(0xFFFF9800),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fountain.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (fountain.baujahr != null) {
                    Text(
                        text = "Baujahr ${fountain.baujahr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (fountain.isTrinkwasser) "Trinkwasser" else "Kein Trinkwasser",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (fountain.isTrinkwasser) SuccessGreen else Color(0xFFE65100),
                    )
                    if (fountain.isAbgestellt) {
                        Text(
                            text = " | Abgestellt",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB42318),
                        )
                    }
                }
            }

            // Distance
            if (distance < Float.MAX_VALUE) {
                Column(horizontalAlignment = Alignment.End) {
                    val distText = if (distance < 1000) {
                        "${distance.roundToInt()}m"
                    } else {
                        "${"%.1f".format(distance / 1000)}km"
                    }
                    Text(
                        text = distText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WaterTeal,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FountainDetailScreen(
    fountain: Fountain,
    distance: Float,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Brunnen-Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = fountain.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )

                    fountain.location?.let {
                        DetailRow(label = "Ort", value = it)
                    }
                    fountain.quartier?.let {
                        DetailRow(label = "Quartier", value = it)
                    }
                    fountain.baujahr?.let {
                        DetailRow(label = "Baujahr", value = it.toString())
                    }
                    DetailRow(
                        label = "Trinkwasser",
                        value = if (fountain.isTrinkwasser) "Ja" else "Nein",
                    )
                    fountain.wasserart?.let {
                        DetailRow(label = "Wasserart", value = it)
                    }
                    fountain.brunnenNummer?.let {
                        DetailRow(label = "Brunnen-Nr.", value = it)
                    }
                    if (fountain.isAbgestellt) {
                        DetailRow(label = "Status", value = "Abgestellt")
                    }
                    if (distance < Float.MAX_VALUE) {
                        val distText = if (distance < 1000) {
                            "${distance.roundToInt()}m"
                        } else {
                            "${"%.1f".format(distance / 1000)}km"
                        }
                        DetailRow(label = "Entfernung", value = distText)
                    }
                }
            }

            // Location card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(24.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = WaterTeal,
                        modifier = Modifier.size(24.dp),
                    )
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
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            color = TextPrimary,
        )
    }
}
