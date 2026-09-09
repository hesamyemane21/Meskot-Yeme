package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.util.Log

object CallAudioManager {
    private const val TAG = "CallAudioManager"

    private var audioManager: AudioManager? = null
    private var toneGenerator: ToneGenerator? = null
    private var incomingMediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private var originalMode: Int = AudioManager.MODE_NORMAL
    private var originalSpeakerphone: Boolean = false
    private var isSpeakerphone: Boolean = false
    private var isMuted: Boolean = false

    fun init(context: Context) {
        if (audioManager == null) {
            audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        }
    }

    // Play ringing tone for outgoing calls (caller hears the line ringing)
    fun startOutgoingRing() {
        stopAllTones()
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 75)
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start outgoing ringback tone: ${e.message}")
        }
    }

    // Play ringtone for incoming calls (receiver's device rings)
    fun startIncomingRing(context: Context) {
        stopAllTones()
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            incomingMediaPlayer = MediaPlayer().apply {
                setDataSource(context, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play incoming ringtone: ${e.message}")
            try {
                // Fallback tone
                toneGenerator = ToneGenerator(AudioManager.STREAM_RING, 85)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_NETWORK_USA_RINGBACK)
            } catch (_: Exception) {}
        }
    }

    // Stop all ringing sounds
    fun stopRinging() {
        stopAllTones()
    }

    // Call connected: enter communication audio mode
    fun startCommunication(isSpeakerDefault: Boolean = false) {
        stopAllTones()
        val am = audioManager ?: return
        try {
            originalMode = am.mode
            originalSpeakerphone = am.isSpeakerphoneOn

            // Request audio focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                audioFocusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            }

            am.mode = AudioManager.MODE_IN_COMMUNICATION
            setSpeakerphone(isSpeakerDefault)
            setMicrophoneMute(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting communication mode: ${e.message}")
        }
    }

    fun setSpeakerphone(enable: Boolean) {
        val am = audioManager ?: return
        try {
            isSpeakerphone = enable
            am.isSpeakerphoneOn = enable
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle speakerphone: ${e.message}")
        }
    }

    fun toggleSpeakerphone(): Boolean {
        setSpeakerphone(!isSpeakerphone)
        return isSpeakerphone
    }

    fun isSpeakerOn(): Boolean = isSpeakerphone

    fun setMicrophoneMute(mute: Boolean) {
        val am = audioManager ?: return
        try {
            isMuted = mute
            am.isMicrophoneMute = mute
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set mic mute: ${e.message}")
        }
    }

    fun toggleMicrophoneMute(): Boolean {
        setMicrophoneMute(!isMuted)
        return isMuted
    }

    fun isMicMuted(): Boolean = isMuted

    // Call ended: play busy/hangup sound and restore audio state
    fun endCall() {
        stopAllTones()
        try {
            // Play short busy/disconnect tone
            val endTone = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 70)
            endTone.startTone(ToneGenerator.TONE_SUP_BUSY, 600)
        } catch (_: Exception) {}

        val am = audioManager ?: return
        try {
            am.isMicrophoneMute = false
            am.isSpeakerphoneOn = originalSpeakerphone
            am.mode = AudioManager.MODE_NORMAL

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error resetting audio mode: ${e.message}")
        }
    }

    private fun stopAllTones() {
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}

        try {
            if (incomingMediaPlayer?.isPlaying == true) {
                incomingMediaPlayer?.stop()
            }
            incomingMediaPlayer?.release()
            incomingMediaPlayer = null
        } catch (_: Exception) {}
    }

    // Play subtle audio cue when a message is received in real time
    fun playMessageReceivedSound(context: Context) {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, alertUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play message notification chime: ${e.message}")
        }
    }

    // Play crisp confirmation tone when user sends a message
    fun playMessageSentSound() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_SYSTEM, 50)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        } catch (_: Exception) {}
    }
}
