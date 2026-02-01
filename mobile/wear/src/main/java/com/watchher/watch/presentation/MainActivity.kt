package com.watchher.watch.presentation

import android.Manifest
import android.content.*
import android.net.Uri
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.Wearable
import com.watchher.messages.WatchToPhone
import com.watchher.watch.PhoneReceiverService
import com.watchher.watch.R
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

            var heartRate by remember { mutableIntStateOf(0) }
            var hasPermission by remember { mutableStateOf(false) }
            var permissionRequestDone by remember { mutableStateOf(false) }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    hasPermission = isGranted
                    permissionRequestDone = true
                }
            )

            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val newHeartRate = intent?.getIntExtra(PhoneReceiverService.EXTRA_HEART_RATE, 0)
                        if (newHeartRate != null && newHeartRate > 0) {
                            heartRate = newHeartRate
                        }
                    }
                }
                val filter = IntentFilter(PhoneReceiverService.ACTION_HEART_RATE_UPDATE)
                LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)

                onDispose {
                    LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
                }
            }

            LaunchedEffect(Unit) {
                permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
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

                        if (permissionRequestDone && !hasPermission) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Permission Denied",
                                    color = watchHerPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Heart rate data is required for WatchHer to function.",
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(Color(0xFF2E2E2E))
                                        .clickable {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            val uri = Uri.fromParts("package", context.packageName, null)
                                            intent.data = uri
                                            context.startActivity(intent)
                                        }
                                        .padding(horizontal = 32.dp, vertical = 10.dp)
                                ) {
                                    Text("Open Settings", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        } else {
                            val monitoringText = if (heartRate > 0) "$heartRate BPM" else "Monitoring..."
                            Text(
                                monitoringText,
                                fontSize = 14.sp,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            val hrProgress = ((heartRate - 50f) / 130f).coerceIn(0f, 1f)

                            // Custom progress bar
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
                                        val vibratorManager =
                                            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                        vibratorManager.defaultVibrator
                                    } else {
                                        @Suppress("DEPRECATION")
                                        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    }
                                }

                                LaunchedEffect(alertActive) {
                                    if (alertActive) {
                                        val vibrationJob: Job = launch {
                                            while (isActive) {
                                                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                                                delay(2500)
                                            }
                                        }

                                        val result = progress.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(
                                                durationMillis = 10000,
                                                easing = LinearEasing
                                            )
                                        )
                                        if (result.endReason == AnimationEndReason.Finished) {
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
                                                color = Color.Black.copy(alpha = 0.3f),
                                                size = size.copy(width = size.width * progress.value)
                                            )
                                        }
                                ) {
                                    Text(
                                        "Cancel",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 10.dp)
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
