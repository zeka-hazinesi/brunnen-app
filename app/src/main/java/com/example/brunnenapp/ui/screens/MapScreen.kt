package com.example.brunnenapp.ui.screens

import android.graphics.drawable.GradientDrawable
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.brunnenapp.data.Fountain
import com.example.brunnenapp.data.FountainRepository
import com.example.brunnenapp.ui.theme.TextSecondary
import com.example.brunnenapp.ui.theme.WaterTeal
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun MapScreen(hasLocationPermission: Boolean) {
    val context = LocalContext.current
    val fountains = remember { FountainRepository.loadFountains(context) }

    // Configure osmdroid
    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(18.0)
                controller.setCenter(GeoPoint(47.3769, 8.5417)) // Zurich center

                // My location overlay
                if (hasLocationPermission) {
                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation()
                    overlays.add(locationOverlay)
                }

                // Add fountain markers
                val drinkableIcon = createCircleDrawable(ctx, 0xFF0097A7.toInt(), 24)
                val nonDrinkableIcon = createCircleDrawable(ctx, 0xFFFF9800.toInt(), 24)
                val abgestelltIcon = createCircleDrawable(ctx, 0xFFBDBDBD.toInt(), 24)

                for (fountain in fountains) {
                    val marker = Marker(this)
                    marker.position = GeoPoint(fountain.latitude, fountain.longitude)
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    marker.title = fountain.name
                    marker.snippet = buildString {
                        if (fountain.quartier != null) append(fountain.quartier)
                        if (fountain.baujahr != null) {
                            if (isNotEmpty()) append(" | ")
                            append("Baujahr ${fountain.baujahr}")
                        }
                        if (isNotEmpty()) append("\n")
                        append(if (fountain.isTrinkwasser) "Trinkwasser" else "Kein Trinkwasser")
                        if (fountain.isAbgestellt) append(" (Abgestellt)")
                    }
                    marker.icon = when {
                        fountain.isAbgestellt -> abgestelltIcon
                        fountain.isTrinkwasser -> drinkableIcon
                        else -> nonDrinkableIcon
                    }
                    overlays.add(marker)
                }

                // Also get location from fused provider for more accuracy
                if (hasLocationPermission) {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)
                    try {
                        fusedClient.lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) {
                                controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                            }
                        }
                    } catch (_: SecurityException) {}
                }
            }
        },
        update = { mapView ->
            mapView.invalidate()
        },
    )
}

private fun createCircleDrawable(context: android.content.Context, color: Int, sizeDp: Int): android.graphics.drawable.Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt()
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setSize(sizePx, sizePx)
        setColor(color)
        setStroke((2 * density).toInt(), 0xFFFFFFFF.toInt())
    }
}
