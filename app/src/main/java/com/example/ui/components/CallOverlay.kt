package com.example.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.AppLanguage
import com.example.data.MeskotStrings
import com.example.ui.ActiveCall
import com.example.ui.theme.CrossRed
import com.example.ui.theme.Gold
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.InkDark

@Composable
fun CallOverlay(
    activeCall: ActiveCall,
    currentLanguage: AppLanguage,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onFlipCamera: () -> Unit,
    onEndCall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkDark)
            .testTag("call_overlay")
    ) {
        if (activeCall.isRinging) {
            // Outgoing call ringing screen
            OutgoingRingingView(
                activeCall = activeCall,
                currentLanguage = currentLanguage,
                onEndCall = onEndCall
            )
        } else if (activeCall.callType == "video") {
            // Connected Video Call Screen
            VideoCallConnectedView(
                activeCall = activeCall,
                currentLanguage = currentLanguage,
                onToggleMute = onToggleMute,
                onToggleSpeaker = onToggleSpeaker,
                onToggleCamera = onToggleCamera,
                onFlipCamera = onFlipCamera,
                onEndCall = onEndCall
            )
        } else {
            // Connected Audio Call Screen
            AudioCallConnectedView(
                activeCall = activeCall,
                currentLanguage = currentLanguage,
                onToggleMute = onToggleMute,
                onToggleSpeaker = onToggleSpeaker,
                onEndCall = onEndCall
            )
        }
    }
}

@Composable
private fun OutgoingRingingView(
    activeCall: ActiveCall,
    currentLanguage: AppLanguage,
    onEndCall: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "outgoing_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outgoing_pulse_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            Text(
                text = "MESKOT CALL",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Gold
            )

            Spacer(modifier = Modifier.height(36.dp))

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
                    photoUrl = activeCall.otherUser.photoUrl,
                    name = activeCall.otherUser.displayName,
                    size = 110,
                    modifier = Modifier.border(3.dp, Gold, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = activeCall.otherUser.displayName,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = MeskotStrings.get("calling", currentLanguage) + "…",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (activeCall.callType == "video") "📹 Video Call" else "📞 Audio Call",
                fontSize = 13.sp,
                color = Gold
            )
        }

        // Cancel / End call button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 40.dp)
        ) {
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
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = MeskotStrings.get("endCall", currentLanguage),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun VideoCallConnectedView(
    activeCall: ActiveCall,
    currentLanguage: AppLanguage,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onFlipCamera: () -> Unit,
    onEndCall: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main WebRTC Video stream
        if (activeCall.roomUrl.isNotBlank()) {
            WebRtcCallView(
                roomUrl = activeCall.roomUrl,
                callType = "video",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(InkDark),
                contentAlignment = Alignment.Center
            ) {
                UserAvatar(
                    photoUrl = activeCall.otherUser.photoUrl,
                    name = activeCall.otherUser.displayName,
                    size = 120,
                    modifier = Modifier.border(3.dp, Gold, CircleShape)
                )
            }
        }

        // Top Gradient overlay with Participant Name and Call Timer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(InkDark.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 40.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        photoUrl = activeCall.otherUser.photoUrl,
                        name = activeCall.otherUser.displayName,
                        size = 40,
                        modifier = Modifier.border(1.5.dp, Gold, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = activeCall.otherUser.displayName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        val m = activeCall.durationSec / 60
                        val s = activeCall.durationSec % 60
                        Text(
                            text = "HD Video • ${String.format("%02d:%02d", m, s)}",
                            fontSize = 12.sp,
                            color = GreenAccent
                        )
                    }
                }
            }
        }

        // Floating Local PiP Camera Preview (CameraX live preview of self)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 120.dp)
                .size(width = 110.dp, height = 150.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Gold, RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            CameraPreview(
                isFrontCamera = activeCall.isFrontCamera,
                isCameraOff = activeCall.isCameraOff,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom Controls Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, InkDark.copy(alpha = 0.92f))
                    )
                )
                .padding(bottom = 32.dp, top = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Mute Toggle
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (activeCall.isMuted) CrossRed else Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = if (activeCall.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Speaker Toggle
                IconButton(
                    onClick = onToggleSpeaker,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (activeCall.isSpeakerOn) Gold else Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = if (activeCall.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        contentDescription = "Speaker",
                        tint = if (activeCall.isSpeakerOn) InkDark else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // End Call (Hang up)
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(64.dp)
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

                // Camera Toggle
                IconButton(
                    onClick = onToggleCamera,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (activeCall.isCameraOff) CrossRed else Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = if (activeCall.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        contentDescription = "Camera",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Flip Camera (Front/Back)
                IconButton(
                    onClick = onFlipCamera,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Flip Camera",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioCallConnectedView(
    activeCall: ActiveCall,
    currentLanguage: AppLanguage,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "audio_wave_scale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Invisible WebRtcCallView to handle live WebRTC audio transport
        if (activeCall.roomUrl.isNotBlank()) {
            WebRtcCallView(
                roomUrl = activeCall.roomUrl,
                callType = "audio",
                modifier = Modifier.size(1.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Text(
                    text = "MESKOT AUDIO CALL",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Gold
                )

                Spacer(modifier = Modifier.height(44.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(180.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(waveScale)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.12f))
                    )
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                            .scale(waveScale * 0.9f)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.22f))
                    )

                    UserAvatar(
                        photoUrl = activeCall.otherUser.photoUrl,
                        name = activeCall.otherUser.displayName,
                        size = 110,
                        modifier = Modifier.border(3.dp, Gold, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = activeCall.otherUser.displayName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                val m = activeCall.durationSec / 60
                val s = activeCall.durationSec % 60
                Text(
                    text = "Connected • ${String.format("%02d:%02d", m, s)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = GreenAccent
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (activeCall.isSpeakerOn) "🔊 Speakerphone On" else "👂 Earpiece Active",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }

            // Bottom Audio Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Mute Toggle
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (activeCall.isMuted) CrossRed else Color.White.copy(alpha = 0.18f))
                ) {
                    Icon(
                        imageVector = if (activeCall.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // End Call (Hang up)
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
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Speaker Toggle
                IconButton(
                    onClick = onToggleSpeaker,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (activeCall.isSpeakerOn) Gold else Color.White.copy(alpha = 0.18f))
                ) {
                    Icon(
                        imageVector = if (activeCall.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        contentDescription = "Speaker",
                        tint = if (activeCall.isSpeakerOn) InkDark else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// CameraX Camera Preview Composable
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = true,
    isCameraOff: Boolean = false
) {
    if (isCameraOff) {
        Box(
            modifier = modifier.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VideocamOff,
                contentDescription = "Camera off",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = if (isFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    Log.w("CameraPreview", "Failed to bind camera: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(context))
        },
        modifier = modifier
    )
}

// Secure WebRTC Stream View
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebRtcCallView(
    roomUrl: String,
    callType: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) {
                        request.grant(request.resources)
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): Boolean = false
                }
                loadUrl(roomUrl)
            }
        },
        modifier = modifier
    )
}
