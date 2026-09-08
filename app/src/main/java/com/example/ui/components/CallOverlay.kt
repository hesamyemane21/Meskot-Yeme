package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.data.MeskotStrings
import com.example.ui.ActiveCall
import com.example.ui.theme.CrossRed
import com.example.ui.theme.Gold
import com.example.ui.theme.InkDark

@Composable
fun CallOverlay(
    activeCall: ActiveCall,
    currentLanguage: AppLanguage,
    onToggleMute: () -> Unit,
    onToggleCamera: () -> Unit,
    onEndCall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkDark)
            .padding(32.dp)
            .testTag("call_overlay")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Call Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                UserAvatar(
                    photoUrl = activeCall.otherUser.photoUrl,
                    name = activeCall.otherUser.displayName,
                    size = 110,
                    modifier = Modifier.border(3.dp, Gold, CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = activeCall.otherUser.displayName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                val statusText = if (activeCall.isRinging) {
                    MeskotStrings.get("calling", currentLanguage)
                } else {
                    val m = activeCall.durationSec / 60
                    val s = activeCall.durationSec % 60
                    "${MeskotStrings.get("callConnected", currentLanguage)} (${String.format("%02d:%02d", m, s)})"
                }

                Text(
                    text = statusText,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (activeCall.callType == "video") "📹 " + MeskotStrings.get("videoCall", currentLanguage) else "📞 " + MeskotStrings.get("audioCall", currentLanguage),
                    fontSize = 13.sp,
                    color = Gold
                )
            }

            // Bottom Controls
            Row(
                modifier = Modifier.padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute Mic Toggle
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (activeCall.isMuted) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = if (activeCall.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // End Call (Red Hangup Button)
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(CrossRed)
                        .testTag("end_call_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = MeskotStrings.get("endCall", currentLanguage),
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Camera Toggle (for video calls)
                if (activeCall.callType == "video") {
                    IconButton(
                        onClick = onToggleCamera,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (activeCall.isCameraOff) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = if (activeCall.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = "Camera",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
