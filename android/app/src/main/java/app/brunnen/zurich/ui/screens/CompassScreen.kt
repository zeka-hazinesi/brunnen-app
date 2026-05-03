package app.brunnen.zurich.ui.screens

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brunnen.zurich.data.AuthClient
import app.brunnen.zurich.data.Fountain
import app.brunnen.zurich.data.FountainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.brunnen.zurich.ui.theme.ErrorRed
import app.brunnen.zurich.ui.theme.GoldAccent
import app.brunnen.zurich.ui.theme.SuccessGreen
import app.brunnen.zurich.ui.theme.TextPrimary
import app.brunnen.zurich.ui.theme.TextSecondary
import app.brunnen.zurich.ui.theme.WaterDark
import app.brunnen.zurich.ui.theme.WaterTeal
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

    var visitedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(Unit) {
        try {
            visitedIds = withContext(Dispatchers.IO) { authClient.getVisitedFountainIds() }
        } catch (_: Throwable) {}
    }

    var checkInState by remember { mutableStateOf(CheckInState.IDLE) }
    var checkInCountdown by remember { mutableIntStateOf(0) }
    var checkInMessage by remember { mutableStateOf<String?>(null) }

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
                    .filter { it.first.id !in visitedIds }
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(WaterTeal.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = WaterTeal,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Text(
                        text = "Standort benötigt",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Die App braucht deinen Standort, um den nächsten Brunnen zu finden.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                    )
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WaterTeal),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Standort freigeben", fontWeight = FontWeight.SemiBold)
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
                CircularProgressIndicator(
                    color = WaterTeal,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Standort wird ermittelt...",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
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
    val isNearby = distanceToNearest <= CheckInRadiusMeters

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Fountain info card
        if (fountain != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(WaterTeal.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = WaterTeal,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fountain.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                        )
                        if (fountain.quartier != null) {
                            Text(
                                text = fountain.quartier,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.NearMe,
                        contentDescription = null,
                        tint = if (isNearby) SuccessGreen else TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Compass
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center,
        ) {
            CompassDial(angle = animatedAngle, isNearby = isNearby)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val distText = if (distanceToNearest < 1000) {
                    "${distanceToNearest.roundToInt()}"
                } else {
                    "${"%.1f".format(distanceToNearest / 1000)}"
                }
                val unitText = if (distanceToNearest < 1000) "m" else "km"
                Text(
                    text = distText,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isNearby) SuccessGreen else TextPrimary,
                    letterSpacing = (-1).sp,
                )
                Text(
                    text = unitText,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Check-in area
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (checkInState) {
                CheckInState.IDLE -> {
                    if (isNearby) {
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
                                    try {
                                        authClient.recordCheckIn(fountain!!, PointsPerCheckIn)
                                        visitedIds = visitedIds + fountain.id
                                        checkInState = CheckInState.SUCCESS
                                        checkInMessage = "+$PointsPerCheckIn Punkte!"
                                    } catch (e: Throwable) {
                                        Log.e(Tag, "Check-in failed", e)
                                        checkInState = CheckInState.IDLE
                                        checkInMessage = "Fehler: ${e.message}"
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 1.dp,
                            ),
                        ) {
                            Text(
                                text = "Einchecken",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(
                                text = "Nähere dich einem Brunnen auf ${CheckInRadiusMeters.roundToInt()}m",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            )
                        }
                    }
                }

                CheckInState.VERIFYING -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Verifizierung",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                letterSpacing = 1.sp,
                            )
                            Text(
                                text = "${checkInCountdown}s",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Bleib in der Nähe des Brunnens",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }

                CheckInState.SUCCESS -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Eingecheckt!",
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

            if (checkInState == CheckInState.IDLE && checkInMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = checkInMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorRed,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CompassDial(angle: Float, isNearby: Boolean) {
    val accentColor = if (isNearby) SuccessGreen else WaterTeal

    Canvas(modifier = Modifier.size(280.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Outer ring
        drawCircle(
            color = Color(0xFFE2E8F0),
            radius = radius,
            center = center,
            style = Stroke(width = 2f),
        )

        // Inner fill
        drawCircle(
            color = Color.White.copy(alpha = 0.6f),
            radius = radius - 2f,
            center = center,
        )

        // Secondary ring
        drawCircle(
            color = Color(0xFFF1F5F9),
            radius = radius * 0.82f,
            center = center,
            style = Stroke(width = 1.5f),
        )

        // Tick marks
        for (i in 0 until 72) {
            rotate(degrees = i * 5f, pivot = center) {
                val isMajor = i % 18 == 0
                val isMinor = i % 9 == 0
                val tickLength = if (isMajor) 18f else if (isMinor) 10f else 5f
                val tickWidth = if (isMajor) 2.5f else 1f
                val tickColor = if (isMajor) Color(0xFF64748B) else Color(0xFFCBD5E1)
                drawLine(
                    color = tickColor,
                    start = Offset(center.x, center.y - radius + 10),
                    end = Offset(center.x, center.y - radius + 10 + tickLength),
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round,
                )
            }
        }

        // Direction arrow
        rotate(degrees = angle, pivot = center) {
            val arrowPath = Path().apply {
                moveTo(center.x, center.y - radius * 0.62f)
                lineTo(center.x - 14f, center.y - radius * 0.30f)
                lineTo(center.x, center.y - radius * 0.38f)
                lineTo(center.x + 14f, center.y - radius * 0.30f)
                close()
            }
            drawPath(arrowPath, color = accentColor)

            drawCircle(
                color = accentColor,
                radius = 5f,
                center = Offset(center.x, center.y - radius * 0.62f),
            )
        }

        // Center dot
        drawCircle(
            color = Color(0xFFE2E8F0),
            radius = 10f,
            center = center,
        )
        drawCircle(
            color = accentColor,
            radius = 6f,
            center = center,
        )
    }
}

private enum class CheckInState {
    IDLE,
    VERIFYING,
    SUCCESS,
}
