package app.brunnen.zurich.ui.screens

import android.graphics.drawable.GradientDrawable
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import app.brunnen.zurich.data.AuthClient
import app.brunnen.zurich.data.FountainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import com.google.android.gms.location.LocationServices

@Composable
fun MapScreen(hasLocationPermission: Boolean, authClient: AuthClient) {
    val context = LocalContext.current
    val fountains = remember { FountainRepository.loadFountains(context) }
    var visitedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(Unit) {
        try {
            visitedIds = withContext(Dispatchers.IO) { authClient.getVisitedFountainIds() }
        } catch (_: Throwable) {}
    }

    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                val cartoPositron = object : OnlineTileSourceBase(
                    "CartoPositron",
                    0, 20, 256, ".png",
                    arrayOf(
                        "https://a.basemaps.cartocdn.com/light_all/",
                        "https://b.basemaps.cartocdn.com/light_all/",
                        "https://c.basemaps.cartocdn.com/light_all/",
                    ),
                ) {
                    override fun getTileURLString(pMapTileIndex: Long): String {
                        val z = MapTileIndex.getZoom(pMapTileIndex)
                        val x = MapTileIndex.getX(pMapTileIndex)
                        val y = MapTileIndex.getY(pMapTileIndex)
                        return "$baseUrl$z/$x/$y$mImageFilenameEnding"
                    }
                }
                setTileSource(cartoPositron)
                setMultiTouchControls(true)
                controller.setZoom(18.0)
                controller.setCenter(GeoPoint(47.3769, 8.5417))

                if (hasLocationPermission) {
                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation()
                    overlays.add(locationOverlay)
                }

                val drinkableIcon = createCircleDrawable(ctx, 0xFF00897B.toInt(), 24)
                val nonDrinkableIcon = createCircleDrawable(ctx, 0xFFF59E0B.toInt(), 24)
                val abgestelltIcon = createCircleDrawable(ctx, 0xFFBDBDBD.toInt(), 24)
                val visitedIcon = createCircleDrawable(ctx, 0xFF7C3AED.toInt(), 24)

                for (fountain in fountains) {
                    val isVisited = fountain.id in visitedIds
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
                        if (isVisited) append("\n✓ Besucht")
                    }
                    marker.icon = when {
                        isVisited -> visitedIcon
                        fountain.isAbgestellt -> abgestelltIcon
                        fountain.isTrinkwasser -> drinkableIcon
                        else -> nonDrinkableIcon
                    }
                    overlays.add(marker)
                }

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
