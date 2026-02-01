package com.watchher.watch.presentation

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
            var accelX by remember { mutableFloatStateOf(0f) }
            var accelY by remember { mutableFloatStateOf(0f) }
            var accelZ by remember { mutableFloatStateOf(0f) }
            var hasPermission by remember { mutableStateOf(hasAllPermissions()) }
            var permissionRequested by remember { mutableStateOf(false) }
            val lifecycleOwner = LocalLifecycleOwner.current

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

            // Receive heart rate updates
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

                        var alertActive by remember { mutableStateOf(true) }
                        var alertSent by remember { mutableStateOf(false) }

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
                                "Accel: %.2f, %.2f, %.2f".format(accelX, accelY, accelZ),
                                fontSize = 12.sp,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            val hrProgress =
                                ((heartRate - 50f) / 130f).coerceIn(0f, 1f)

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
                                        .fillMaxWidth(hrProgress)
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

        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                Log.d("WatchHerWear", "Found node: ${node.displayName}")

                val data = WatchToPhone(
                    accelPeak = 10.0,
                    accelRms = 2.0,
                    heartRateMean = 5.0,
                    heartRateStd = 2.5,
                    needsHelp = true,
                    ppg = 11.11,
                    steps = 25,
                    timeOfDay = 0.78
                )

                Wearable.getMessageClient(this)
                    .sendMessage(
                        node.id,
                        "/watch_her/watch_to_phone",
                        data.encodeJson().toByteArray()
                    )
                    .addOnSuccessListener {
                        Log.d("WatchHerWear", "Message Sent!!!")
                    }
                    .addOnFailureListener { e ->
                        Log.e("WatchHerWear", "Failed to send message", e)
                    }
            }
        }
    }
}
