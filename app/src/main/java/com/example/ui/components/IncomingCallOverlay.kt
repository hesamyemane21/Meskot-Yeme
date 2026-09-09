package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.data.CallSession
import com.example.data.MeskotStrings
import com.example.ui.theme.CrossRed
import com.example.ui.theme.Gold
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.InkDark
import com.example.util.CallAudioManager

@Composable
fun IncomingCallOverlay(
    session: CallSession,
    currentLanguage: AppLanguage,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val context = LocalContext.current

    // Audibly ring device while incoming call dialog is visible
    DisposableEffect(session.callId) {
        CallAudioManager.startIncomingRing(context)
        onDispose {
            CallAudioManager.stopRinging()
        }
    }

    // Ripple pulse animation around caller avatar
    val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkDark.copy(alpha = 0.96f))
            .padding(32.dp)
            .testTag("incoming_call_overlay")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Meskot & Call Type
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Text(
                    text = "MESKOT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Gold
                )

                Spacer(modifier = Modifier.height(10.dp))

                val callTypeLabel = if (session.callType == "video") {
                    "📹 " + MeskotStrings.get("videoCall", currentLanguage)
                } else {
                    "📞 " + MeskotStrings.get("audioCall", currentLanguage)
                }
                Text(
                    text = callTypeLabel,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Pulsing Avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(170.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.15f))
                    )
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale * 0.9f)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.25f))
                    )

                    UserAvatar(
                        photoUrl = session.callerPhoto,
                        name = session.callerName,
                        size = 110,
                        modifier = Modifier.border(3.dp, Gold, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = session.callerName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (session.callType == "video") "Incoming Video Call…" else "Incoming Audio Call…",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }

            // Bottom Buttons: Decline (Red) & Accept (Green)
            Row(
                modifier = Modifier.padding(bottom = 50.dp),
                horizontalArrangement = Arrangement.spacedBy(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(CrossRed)
                            .testTag("decline_call_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Decline",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Decline", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }

                // Accept Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(GreenAccent)
                            .testTag("accept_call_btn")
                    ) {
                        Icon(
                            imageVector = if (session.callType == "video") Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = "Accept",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Accept", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
