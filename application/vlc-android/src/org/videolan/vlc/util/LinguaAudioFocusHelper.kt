package org.videolan.vlc.util

import android.annotation.TargetApi
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.resources.AndroidDevices
import org.videolan.vlc.BuildConfig

private const val TAG = "VLCAudioFocusHelper"

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@Suppress("DEPRECATION")
class LinguaAudioFocusHelper(private val audioRecorder: AudioRecorder) {

    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusRequest : AudioFocusRequest
    private var hasAudioFocus = false
    @Volatile
    internal var lossTransient = false

    private val audioFocusListener = createOnAudioFocusChangeListener()

    internal fun changeAudioFocus(acquire: Boolean) {
        if (!this::audioManager.isInitialized) audioManager = audioRecorder.context.getSystemService() ?: return

        if (acquire) {
            if (!hasAudioFocus) {
                val result = requestAudioFocus()
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    audioManager.setParameters("bgm_state=true")
                    hasAudioFocus = true
                }
            }
        } else if (hasAudioFocus) {
            abandonAudioFocus()
            audioManager.setParameters("bgm_state=false")
            hasAudioFocus = false
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun abandonAudioFocus() = if (AndroidUtil.isOOrLater) {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    } else {
        audioManager.abandonAudioFocus(audioFocusListener)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun requestAudioFocus() = if (AndroidUtil.isOOrLater) {
        val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build()
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .setAudioAttributes(attributes)
                .build()
        audioManager.requestAudioFocus(audioFocusRequest)
    } else {
        audioManager.requestAudioFocus(audioFocusListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    }

    private fun createOnAudioFocusChangeListener(): AudioManager.OnAudioFocusChangeListener {
        return object : AudioManager.OnAudioFocusChangeListener {
            private var lossTransientVolume = -1f
            private var wasPlaying = false

            override fun onAudioFocusChange(focusChange: Int) {
                /*
             * Pause playback during alerts and notifications
             */
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        if (BuildConfig.DEBUG) Log.i(TAG, "AUDIOFOCUS_LOSS")
                        // Pause playback
                        changeAudioFocus(false)
                        audioRecorder.pausePlaying()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        if (BuildConfig.DEBUG) Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT")
                        // Pause playback
                        pausePlayback()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        if (BuildConfig.DEBUG) Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK")
                        // Lower the volume
                        if (audioRecorder.isRecordedPlaying) {
                            if (AndroidDevices.isAmazon) {
                                pausePlayback()
                            } else {
                                val volume = audioRecorder.getVolume()
                                lossTransientVolume = volume
                                audioRecorder.setVolume(volume/3)
                            }
                        }
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (BuildConfig.DEBUG) Log.i(TAG, "AUDIOFOCUS_GAIN: ")
                        // Resume playback
                        if (lossTransientVolume != -1f) {
                            audioRecorder.setVolume(lossTransientVolume)
                            lossTransientVolume = -1f
                        }
                        if (lossTransient) {
                            if (wasPlaying)
                                audioRecorder.resumePlaying()
                            lossTransient = false
                        }
                    }
                }
            }

            private fun pausePlayback() {
                if (lossTransient) return
                lossTransient = true
                wasPlaying = audioRecorder.isRecordedPlaying
                if (wasPlaying) audioRecorder.pausePlaying()
            }
        }
    }
}
