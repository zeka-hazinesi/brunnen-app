package com.example.brunnenapp.ui.screens

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.brunnenapp.data.AuthClient
import com.example.brunnenapp.data.Fountain
import com.example.brunnenapp.data.FountainRepository
import com.example.brunnenapp.ui.theme.ErrorRed
import com.example.brunnenapp.ui.theme.GoldAccent
import com.example.brunnenapp.ui.theme.SuccessGreen
import com.example.brunnenapp.ui.theme.TextPrimary
import com.example.brunnenapp.ui.theme.TextSecondary
import com.example.brunnenapp.ui.theme.WaterDark
import com.example.brunnenapp.ui.theme.WaterTeal
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.roundToInt

private const val Tag = "CompassScreen"
private const val CheckInRadiusMeters = 20f
private const val CheckInDurationMs = 10_000L
private const val PointsPerCheckIn = 10

@Composable
fun CompassScreen(
    authClient: AuthClient,
    hasLocationPermission: Boolean,
    onRequestPermission: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fountains = remember { FountainRepository.loadFountains(context) }

    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }
    var azimuth by remember { mutableFloatStateOf(0f) }
    var nearestFountain by remember { mutableStateOf<Fountain?>(null) }
    var distanceToNearest by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var bearingToNearest by remember { mutableFloatStateOf(0f) }

    var checkInState by remember { mutableStateOf(CheckInState.IDLE) }
    var checkInCountdown by remember { mutableIntStateOf(0) }
    var checkInMessage by remember { mutableStateOf<String?>(null) }

    // Compass sensor
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val orientation = FloatArray(3)
        val rotationMatrix = FloatArray(9)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // GPS location updates
    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            return@DisposableEffect onDispose {}
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                userLat = loc.latitude
                userLng = loc.longitude

                val sorted = FountainRepository.sortedByDistance(fountains, loc.latitude, loc.longitude)
                if (sorted.isNotEmpty()) {
                    nearestFountain = sorted[0].first
                    distanceToNearest = sorted[0].second

                    val results = FloatArray(2)
                    Location.distanceBetween(
                        loc.latitude, loc.longitude,
                        sorted[0].first.latitude, sorted[0].first.longitude,
                        results,
                    )
                    bearingToNearest = results[1]
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(locationRequest, callback, context.mainLooper)
        } catch (e: SecurityException) {
            Log.e(Tag, "Location permission missing", e)
        }

        onDispose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    if (!hasLocationPermission) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Standort benötigt",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Die App braucht deinen Standort, um den nächsten Brunnen zu finden.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = WaterTeal),
                    ) {
                        Text("Standort freigeben")
                    }
                }
            }
        }
        return
    }

    if (userLat == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = WaterTeal)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Standort wird ermittelt...", color = TextSecondary)
            }
        }
        return
    }

    val fountain = nearestFountain
    val relativeAngle = bearingToNearest - azimuth
    val animatedAngle by animateFloatAsState(
        targetValue = relativeAngle,
        animationSpec = tween(durationMillis = 300),
        label = "compass",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Fountain info
        if (fountain != null) {
            Text(
                text = fountain.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = fountain.quartier ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Compass
        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center,
        ) {
            CompassDial(angle = animatedAngle)

            // Distance in center
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val distText = if (distanceToNearest < 1000) {
                    "${distanceToNearest.roundToInt()}m"
                } else {
                    "${"%.1f".format(distanceToNearest / 1000)}km"
                }
                Text(
                    text = distText,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (distanceToNearest <= CheckInRadiusMeters) SuccessGreen else TextPrimary,
                )
                Text(
                    text = "Entfernung",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Check-in area
        when (checkInState) {
            CheckInState.IDLE -> {
                if (distanceToNearest <= CheckInRadiusMeters) {
                    Button(
                        onClick = {
                            checkInState = CheckInState.VERIFYING
                            checkInCountdown = 10
                            checkInMessage = null
                            scope.launch {
                                for (i in 10 downTo 1) {
                                    checkInCountdown = i
                                    delay(1000L)
                                    if (distanceToNearest > CheckInRadiusMeters) {
                                        checkInState = CheckInState.IDLE
                                        checkInMessage = "Check-in abgebrochen — du hast den Radius verlassen."
                                        return@launch
                                    }
                                }
                                // Success
                                try {
                                    authClient.recordCheckIn(fountain!!, PointsPerCheckIn)
                                    checkInState = CheckInState.SUCCESS
                                    checkInMessage = "+$PointsPerCheckIn Punkte!"
                                } catch (e: Throwable) {
                                    Log.e(Tag, "Check-in failed", e)
                                    checkInState = CheckInState.IDLE
                                    checkInMessage = "Fehler: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(
                            text = "Einchecken",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                } else {
                    Text(
                        text = "Nähere dich einem Brunnen auf ${CheckInRadiusMeters.roundToInt()}m, um einzuchecken.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            CheckInState.VERIFYING -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Verifizierung läuft...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                        )
                        Text(
                            text = "${checkInCountdown}s",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Bleib in der Nähe des Brunnens!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }

            CheckInState.SUCCESS -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Check-in erfolgreich!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                        )
                        Text(
                            text = checkInMessage ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    delay(3000)
                    checkInState = CheckInState.IDLE
                    checkInMessage = null
                }
            }
        }

        // Show error/status message
        if (checkInState == CheckInState.IDLE && checkInMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = checkInMessage!!,
                style = MaterialTheme.typography.bodyMedium,
                color = ErrorRed,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CompassDial(angle: Float) {
    Canvas(modifier = Modifier.size(260.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Outer circle
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = radius,
            center = center,
            style = Stroke(width = 3f),
        )

        // Inner circle
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = radius * 0.85f,
            center = center,
        )

        // Tick marks
        for (i in 0 until 72) {
            rotate(degrees = i * 5f, pivot = center) {
                val tickLength = if (i % 18 == 0) 20f else if (i % 9 == 0) 12f else 6f
                val tickWidth = if (i % 18 == 0) 3f else 1.5f
                drawLine(
                    color = Color(0xFF546E7A).copy(alpha = 0.6f),
                    start = Offset(center.x, center.y - radius + 8),
                    end = Offset(center.x, center.y - radius + 8 + tickLength),
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round,
                )
            }
        }

        // Arrow pointing to fountain
        rotate(degrees = angle, pivot = center) {
            val arrowPath = Path().apply {
                moveTo(center.x, center.y - radius * 0.65f)
                lineTo(center.x - 18f, center.y - radius * 0.25f)
                lineTo(center.x, center.y - radius * 0.35f)
                lineTo(center.x + 18f, center.y - radius * 0.25f)
                close()
            }
            drawPath(arrowPath, color = Color(0xFF0097A7))

            // Small dot at arrow tip
            drawCircle(
                color = Color(0xFF0097A7),
                radius = 6f,
                center = Offset(center.x, center.y - radius * 0.65f),
            )
        }

        // Center dot
        drawCircle(
            color = Color(0xFF00606F),
            radius = 8f,
            center = center,
        )
    }
}

private enum class CheckInState {
    IDLE,
    VERIFYING,
    SUCCESS,
}
