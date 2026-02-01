package com.watchher.watch.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.watchher.watch.R

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val WatchHerPink = Color(0xFFE0008A)
            val WatchHerGreen = Color(0xFF32CD32)

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
                            modifier = Modifier.size(52.dp)
                        )

                        Text(
                            "WatchHer",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "Monitoring",
                            fontSize = 14.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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
                                    .fillMaxWidth(0.6f)
                                    .background(WatchHerGreen)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (alertActive) {
                            val progress = remember { Animatable(0f) }

                            LaunchedEffect(key1 = Unit) {
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
                            }

                            Text(
                                "DETECTING DANGER...",
                                fontSize = 16.sp,
                                color = WatchHerPink,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

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
                                color = WatchHerPink,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "SAFE",
                                fontSize = 16.sp,
                                color = WatchHerGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
