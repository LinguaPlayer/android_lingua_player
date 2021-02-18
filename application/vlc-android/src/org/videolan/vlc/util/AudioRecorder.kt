package org.videolan.vlc.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import java.io.IOException


private const val TAG = "AudioRecorder"

class AudioRecorder(val context: Context): LifecycleObserver {
    private var recorder: MediaRecorder? = MediaRecorder()
    private var player: MediaPlayer? = MediaPlayer()
    private val audioFocusHelper by lazy { LinguaAudioFocusHelper(this) }
    val audioRecordEventsLiveData = MutableLiveData<AudioRecordEvents>(
        RecordNone
    )
    private val _amplitudeLiveData = MutableLiveData<Int>(0)
    val amplitudeLiveData: LiveData<Int>
        get() = _amplitudeLiveData

    var isRecording: Boolean = false
        private set

    var isRecordedPlaying: Boolean = false
        private set

    //TODO("At them moment it just returns same location, later add new locations")
    fun getAudioPath() = "${context.cacheDir.absolutePath}/speakingPractice.3gp"

    fun toggleAudioRecord() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording(getAudioPath())
        }
    }

    fun startRecording(fileName: String) {
        if (isRecording) stopRecording()
        recorder?.apply {
            try {
                reset()
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96000)
                setAudioSamplingRate(44100)
                setMaxDuration(120000) //60s
                setOutputFile(fileName)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        isRecording = false
                        audioRecordEventsLiveData.value =
                                RecordingStopped()
                    }
                }

                prepare()
                start()
                emitAmplitude(true)
                isRecording = true
                audioRecordEventsLiveData.value =
                        RecordingStarted
            } catch (e: IOException) {
                Log.e(TAG, "prepare() failed")
                isRecording = false
            }
        }
    }

    var r = mutableListOf<Int>()

    private var tickerChannel: ReceiveChannel<Unit>? = null
    private fun emitAmplitude(enable: Boolean) {
        if (enable) {
            tickerChannel = ticker(delayMillis = 100, initialDelayMillis = 0)
            _amplitudeLiveData.value = -1
            GlobalScope.launch {
                for (event in tickerChannel!!) {
                    recorder?.maxAmplitude?.run {
                        _amplitudeLiveData.postValue(this)
//                        Log.d(TAG, "emitAmplitude: $this")
                        r.add(this)
                    }
                }
            }
        } else {
//            _amplitudeLiveData.value = -1
            tickerChannel?.cancel()
            tickerChannel = null
        }
    }

    fun stopRecording(autoPlay: Boolean = true) {
        Log.d(TAG, "stopRecording: $r")
        if (!isRecording) return
        isRecording = false
        emitAmplitude(false)
        recorder?.stop()
        audioRecordEventsLiveData.value =
            RecordingStopped(audoPlay = autoPlay)
//        releaseRecorder()
    }

    fun togglePlayPauseRecord() {
         if (isRecordedPlaying) {
            stopPlaying()
        } else {
            startPlaying(getAudioPath())
        }
    }

    fun startPlaying(fileName: String) {
        Log.d(TAG, "startPlaying")
        player?.let { stopPlaying() } // stop previous one in case it was playing
        player?.setOnCompletionListener {  }
        player?.apply {
            try {
                reset()
                setDataSource(fileName)
                prepare()
                isLooping = false
                player?.setVolume(1f, 1f)
                start()
                setOnCompletionListener {
                    Log.d(TAG, "onComplete")
                    isRecordedPlaying = false
                    audioRecordEventsLiveData.value =
                        RecordedStopped
                }
                isRecordedPlaying = true
                audioRecordEventsLiveData.value =
                    RecordedPlaying
            } catch (e: IOException) {
                Log.e(TAG, "failed $e")
                isRecordedPlaying = false
            }
        }

        audioFocusHelper.changeAudioFocus(true)
    }

    private var playerVolume = 1.0f
    fun getVolume() = playerVolume

    fun setVolume(vol: Float) {
        playerVolume = vol
        player?.setVolume(playerVolume, playerVolume)
    }

    fun stopPlaying() {
        if (!isRecordedPlaying) return
        isRecordedPlaying = false
        audioRecordEventsLiveData.value = RecordedStopped
        player?.stop()
        audioFocusHelper.changeAudioFocus(false)
    }

    fun pausePlaying() {
        isRecordedPlaying = false
        audioRecordEventsLiveData.value = RecordedStopped
        player?.pause()
    }

    fun resumePlaying() {
        isRecordedPlaying = false
        audioRecordEventsLiveData.value = RecordedPlaying
        player?.start()
    }

    private fun releaseRecorder() {
        if (isRecording) {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null // To prevent calling stop on released
    }

    private fun releaseMediaPlayer() {
        if (isRecordedPlaying) {
            player?.stop()
            player?.release()
        }
        player = null // To prevent calling stop after it's released
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onActivityPause() {
        Log.d(TAG, "onActivityPause: ")
        if(isRecordedPlaying) stopPlaying()
        if (isRecording) stopRecording(false)
//        _amplitudeLiveData.value = -1
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun release() {
        releaseMediaPlayer()
        releaseRecorder()
    }

}


sealed class AudioRecordEvents
object RecordingStarted: AudioRecordEvents()
class RecordingStopped(val audoPlay: Boolean = true): AudioRecordEvents()
object RecordedPlaying: AudioRecordEvents()
object RecordedStopped: AudioRecordEvents()
object RecordNone: AudioRecordEvents()
