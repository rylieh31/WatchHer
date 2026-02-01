package com.watchher.watch.presentation

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.watchher.messages.WatchToPhone
import com.watchher.watch.PhoneReceiverService
import com.watchher.watch.R
import com.watchher.watch.sensors.AccelerometerService
import com.watchher.watch.sensors.HeartRateService
import com.watchher.watch.sensors.StepCounterService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.math.pow
import java.time.LocalTime
import java.time.ZoneId

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val watchHerPink = Color(0xFFE0008A)
            val watchHerGreen = Color(0xFF32CD32)
            val context = LocalContext.current

            fun requiredPermissions(): List<String> {
                val permissions = mutableListOf(
                    Manifest.permission.BODY_SENSORS,
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.BODY_SENSORS_BACKGROUND)
                }
                if (Build.VERSION.SDK_INT >= 34) {
                    permissions.add("android.permission.health.READ_HEART_RATE")
                }
                return permissions
            }

            fun hasAllPermissions(): Boolean {
                return requiredPermissions().all { permission ->
                    context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
                }
            }

            var heartRate by remember { mutableIntStateOf(0) }
            var confidencePercent by remember { mutableIntStateOf(0) }
            var cooldownUntilMs by remember { mutableLongStateOf(0L) }
            var accelX by remember { mutableFloatStateOf(0f) }
            var accelY by remember { mutableFloatStateOf(0f) }
            var accelZ by remember { mutableFloatStateOf(0f) }
            var stepCounter by remember { mutableFloatStateOf(0f) }
            var hasPermission by remember { mutableStateOf(hasAllPermissions()) }
            var permissionRequested by remember { mutableStateOf(false) }
            var alertActive by remember { mutableStateOf(false) }
            var alertSent by remember { mutableStateOf(false) }
            val lifecycleOwner = LocalLifecycleOwner.current

            val hrSamples = remember { mutableStateListOf<Double>() }
            val accelSamples = remember { mutableStateListOf<Double>() }
            val ppgSamples = remember { mutableStateListOf<Double>() }
            val stepDeltas = remember { mutableStateListOf<Int>() }
            var lastStepCounter by remember { mutableStateOf<Float?>(null) }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                hasPermission = results.values.all { it }
                permissionRequested = true
            }

            // Request permission once if needed
            LaunchedEffect(hasPermission) {
                if (!hasPermission && !permissionRequested) {
                    permissionRequested = true
                    permissionLauncher.launch(requiredPermissions().toTypedArray())
                }
            }

            LaunchedEffect(hasPermission) {
                if (hasPermission) {
                    val hrServiceIntent = Intent(context, HeartRateService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(hrServiceIntent)
                    } else {
                        context.startService(hrServiceIntent)
                    }
                }
            }

            LaunchedEffect(Unit) {
                val accelIntent = Intent(context, AccelerometerService::class.java)
                context.startService(accelIntent)
            }

            LaunchedEffect(hasPermission) {
                if (hasPermission) {
                    val stepIntent = Intent(context, StepCounterService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(stepIntent)
                    } else {
                        context.startService(stepIntent)
                    }
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        hasPermission = hasAllPermissions()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            // Receive heart rate updates from sensor
            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val newHeartRate = intent?.getIntExtra(
                            HeartRateService.EXTRA_HEART_RATE,
                            0
                        )
                        if (newHeartRate != null && newHeartRate > 0) {
                            Log.d("MainActivity", "HR update: $newHeartRate")
                            heartRate = newHeartRate
                        }
                    }
                }

                val filter = IntentFilter(HeartRateService.ACTION_HEART_RATE_SENSOR_UPDATE)
                LocalBroadcastManager.getInstance(context)
                    .registerReceiver(receiver, filter)

                onDispose {
                    LocalBroadcastManager.getInstance(context)
                        .unregisterReceiver(receiver)
                }
            }

            // Receive confidence updates from phone
            DisposableEffect(Unit) {
                val confidenceReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val rawConfidence = intent?.getDoubleExtra(
                            PhoneReceiverService.EXTRA_HEART_RATE,
                            -1.0
                        ) ?: -1.0
                        if (rawConfidence >= 0) {
                            val percent = if (rawConfidence <= 1.0) {
                                (rawConfidence * 100.0)
                            } else {
                                rawConfidence
                            }
                            confidencePercent = percent.toInt().coerceIn(0, 100)
                        }
                    }
                }

                val filter = IntentFilter(PhoneReceiverService.ACTION_HEART_RATE_UPDATE)
                LocalBroadcastManager.getInstance(context)
                    .registerReceiver(confidenceReceiver, filter)

                onDispose {
                    LocalBroadcastManager.getInstance(context)
                        .unregisterReceiver(confidenceReceiver)
                }
            }

            LaunchedEffect(confidencePercent, alertSent, alertActive, cooldownUntilMs) {
                val now = SystemClock.elapsedRealtime()
                if (
                    confidencePercent > 60 &&
                    !alertActive &&
                    !alertSent &&
                    now >= cooldownUntilMs
                ) {
                    alertActive = true
                } else if (confidencePercent <= 60 && (alertActive || alertSent)) {
                    alertActive = false
                    alertSent = false
                    cooldownUntilMs = now + 30_000L
                }
            }

            // Receive accelerometer updates
            DisposableEffect(Unit) {
                val accelReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        accelX = intent?.getFloatExtra(AccelerometerService.EXTRA_AX, 0f) ?: 0f
                        accelY = intent?.getFloatExtra(AccelerometerService.EXTRA_AY, 0f) ?: 0f
                        accelZ = intent?.getFloatExtra(AccelerometerService.EXTRA_AZ, 0f) ?: 0f
                    }
                }

                val filter = IntentFilter(AccelerometerService.ACTION_ACCEL_UPDATE)
                LocalBroadcastManager.getInstance(context)
                    .registerReceiver(accelReceiver, filter)

                onDispose {
                    LocalBroadcastManager.getInstance(context)
                        .unregisterReceiver(accelReceiver)
                }
            }

            // Receive step counter updates
            DisposableEffect(Unit) {
                val stepReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        stepCounter = intent?.getFloatExtra(
                            StepCounterService.EXTRA_STEP_COUNT,
                            0f
                        ) ?: 0f
                    }
                }

                val filter = IntentFilter(StepCounterService.ACTION_STEP_UPDATE)
                LocalBroadcastManager.getInstance(context)
                    .registerReceiver(stepReceiver, filter)

                onDispose {
                    LocalBroadcastManager.getInstance(context)
                        .unregisterReceiver(stepReceiver)
                }
            }

            fun computeRmssd(samples: List<Double>): Double {
                if (samples.size < 2) return 0.0
                val diffs = samples.zipWithNext { a, b -> (b - a).pow(2) }
                return sqrt(diffs.average())
            }

            fun computeStd(samples: List<Double>): Double {
                if (samples.isEmpty()) return 0.0
                val mean = samples.average()
                val variance = samples.map { (it - mean).pow(2) }.average()
                return sqrt(variance)
            }

            fun timeOfDayNormalized(): Double {
                val now = LocalTime.now(ZoneId.systemDefault())
                val seconds = now.toSecondOfDay().toDouble()
                val anchor = 2 * 3600 + 30 * 60 // 2:30 a.m.
                val shifted = (seconds - anchor + 86400) % 86400
                return 1.0 - (shifted / 86400.0)
            }

            LaunchedEffect(Unit) {
                var tick = 0
                var nextSampleTime = SystemClock.elapsedRealtime()
                while (true) {
                    val now = SystemClock.elapsedRealtime()
                    if (now < nextSampleTime) {
                        delay(nextSampleTime - now)
                    }
                    nextSampleTime += 1000

                    val accelMag = sqrt(
                        accelX.toDouble().pow(2) +
                            accelY.toDouble().pow(2) +
                            accelZ.toDouble().pow(2)
                    )
                    accelSamples.add(accelMag)

                    val hrValue = heartRate.toDouble()
                    if (hrValue > 0) {
                        hrSamples.add(hrValue)
                        ppgSamples.add(hrValue)
                    }

                    val last = lastStepCounter
                    val delta = if (last != null) {
                        (stepCounter - last).coerceAtLeast(0f).toInt()
                    } else {
                        0
                    }
                    stepDeltas.add(delta)
                    lastStepCounter = stepCounter

                    while (hrSamples.size > 20) hrSamples.removeAt(0)
                    while (accelSamples.size > 20) accelSamples.removeAt(0)
                    while (ppgSamples.size > 20) ppgSamples.removeAt(0)
                    while (stepDeltas.size > 20) stepDeltas.removeAt(0)

                    tick += 1
                    if (tick % 10 == 0) {
                        val hrMean = if (hrSamples.isEmpty()) 0.0 else hrSamples.average()
                        val hrStd = computeRmssd(hrSamples)
                        val hrSlope = if (hrSamples.size >= 2) {
                            (hrSamples.last() - hrSamples.first()) / (hrSamples.size - 1)
                        } else {
                            0.0
                        }
                        val steps20s = stepDeltas.sum()
                        val accelRms = if (accelSamples.isEmpty()) 0.0 else {
                            sqrt(accelSamples.map { it * it }.average())
                        }
                        val accelPeak = accelSamples.maxOrNull() ?: 0.0
                        val ppgStd = computeStd(ppgSamples)
                        val tod = timeOfDayNormalized()

                        Wearable.getNodeClient(context)
                            .connectedNodes
                            .addOnSuccessListener { nodes ->
                                nodes.forEach { node ->
                                    val payload = WatchToPhone(
                                        hrMean = hrMean,
                                        hrStd = hrStd,
                                        hrSlope = hrSlope,
                                        steps20s = steps20s,
                                        accelRms = accelRms,
                                        accelPeak = accelPeak,
                                        ppgStd = ppgStd,
                                        timeOfDay = tod,
                                        needsHelp = alertSent
                                    )
                                    Wearable.getMessageClient(context)
                                        .sendMessage(
                                            node.id,
                                            "/watch_her/watch_to_phone",
                                            payload.encodeJson().toByteArray()
                                        )
                                        .addOnSuccessListener {
                                            Log.d("WatchHerWear", "Sent metrics")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("WatchHerWear", "Failed to send metrics", e)
                                        }
                                }
                            }
                    }
                }
            }

            MaterialTheme {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(12.dp)
                    ) {

                        Image(
                            painter = painterResource(id = R.drawable.watchher_logo),
                            contentDescription = "WatchHer Logo",
                            modifier = Modifier.size(48.dp)
                        )

                        Text(
                            "WatchHer",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        run {
                            val monitoringText =
                                if (heartRate > 0) "Monitoring... $heartRate BPM" else "Monitoring..."

                            Text(
                                monitoringText,
                                fontSize = 14.sp,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                "Confidence: $confidencePercent%",
                                fontSize = 12.sp,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            val confidenceProgress = (confidencePercent / 100f)

                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(130.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.DarkGray)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(confidenceProgress)
                                        .background(watchHerGreen)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (alertActive) {
                                val progress = remember { Animatable(0f) }

                                val vibrator = remember {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val manager =
                                            context.getSystemService(
                                                Context.VIBRATOR_MANAGER_SERVICE
                                            ) as VibratorManager
                                        manager.defaultVibrator
                                    } else {
                                        @Suppress("DEPRECATION")
                                        context.getSystemService(
                                            Context.VIBRATOR_SERVICE
                                        ) as Vibrator
                                    }
                                }

                                LaunchedEffect(alertActive) {
                                    if (alertActive) {
                                        progress.snapTo(0f)
                                        val vibrationJob: Job = launch {
                                            while (isActive) {
                                                vibrator.vibrate(
                                                    VibrationEffect.createOneShot(
                                                        200,
                                                        VibrationEffect.DEFAULT_AMPLITUDE
                                                    )
                                                )
                                                delay(2500)
                                            }
                                        }

                                        val result = progress.animateTo(
                                            1f,
                                            tween(
                                                durationMillis = 10_000,
                                                easing = LinearEasing
                                            )
                                        )

                                        if (result.endReason ==
                                            AnimationEndReason.Finished
                                        ) {
                                            alertActive = false
                                            alertSent = true
                                        }

                                        vibrationJob.cancel()
                                    }
                                }

                                Text(
                                    "DETECTING DANGER...",
                                    fontSize = 16.sp,
                                    color = watchHerPink,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(Color(0xFF2E2E2E))
                                        .clickable {
                                            alertActive = false
                                            alertSent = false
                                            cooldownUntilMs =
                                                SystemClock.elapsedRealtime() + 15_000L
                                        }
                                        .drawWithContent {
                                            drawContent()
                                            drawRect(
                                                Color.Black.copy(alpha = 0.3f),
                                                size = size.copy(
                                                    width = size.width * progress.value
                                                )
                                            )
                                        }
                                ) {
                                    Text(
                                        "Cancel",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(
                                            horizontal = 32.dp,
                                            vertical = 10.dp
                                        )
                                    )
                                }

                            } else if (alertSent) {
                                Text(
                                    "ALERT SENT",
                                    fontSize = 16.sp,
                                    color = watchHerPink,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    "SAFE",
                                    fontSize = 16.sp,
                                    color = watchHerGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        startService(Intent(this, PhoneReceiverService::class.java))

    }
}
