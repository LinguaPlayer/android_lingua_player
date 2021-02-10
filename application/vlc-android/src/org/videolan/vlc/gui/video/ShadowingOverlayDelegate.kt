package org.videolan.vlc.gui.video

import android.content.DialogInterface
import android.net.Uri
import android.widget.ImageButton
import androidx.appcompat.widget.ViewStubCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.visualizer.amplitude.AudioRecordView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.tools.setGone
import org.videolan.tools.setVisible
import org.videolan.vlc.R
import org.videolan.vlc.databinding.PlayerShadowingOverlayBinding
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.util.*
import java.io.File
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

private const val TAG = "ShadowingOverlayDelegat"
class ShadowingOverlayDelegate(private val player: VideoPlayerActivity) {

    private var shadowing: ImageButton? = null
    val audioRecorder = AudioRecorder(player.applicationContext)

    private val audioRecordEventsObserver = Observer<AudioRecordEvents> {
        when(it) {
            is RecordingStarted -> {
                recordingStarted()
            }
            is RecordingStopped -> {
                recordingStopped(it.audoPlay)
            }
            is RecordedStopped -> {
                recordedStopping()
            }
            is RecordedPlaying -> {
                recordedPlaying()
            }
            is RecordNone -> {
            }
        }
    }

    fun initShadowing() {
        shadowing = player.findViewById(R.id.shadowing_mode)
        shadowing?.setOnClickListener {
            initShadowingUI()
            player.service?.playlistManager?.player?.apply {
                if (numberOfParsedSubs == 0) {
                    UiTools.shadowingModeAddASubtitle(player, DialogInterface.OnClickListener { _, _ ->
                        player.overlayDelegate.showTracks()
                    },
                            DialogInterface.OnClickListener { _, _ -> }

                    )
                } else {
                    toggleShadowing()
                }

            }
        }
        audioRecorder.audioRecordEventsLiveData.observeForever(audioRecordEventsObserver)
        player.lifecycle.addObserver(audioRecorder)
    }

    private val _shadowingMode = MutableLiveData<Boolean>()
    val shadowingMode: LiveData<Boolean>
        get() = _shadowingMode

    val isPlaying = ObservableField(true)

    val isRecording = ObservableField(false)
    val isRecordedPlaying = ObservableField(false)
    val isRecordedFileExist = ObservableField(isRecordedFileExist())

    val amplitudeLiveData: LiveData<Int>
        get() = audioRecorder.amplitudeLiveData


    lateinit var playerSpeakingOverlayBinding: PlayerShadowingOverlayBinding
    private fun initShadowingUI() {
        val shadowingModeViewStub = player.findViewById<ViewStubCompat>(R.id.shadowing_overlay_stub) ?: return
        shadowingModeViewStub.inflate()

        playerSpeakingOverlayBinding = DataBindingUtil.bind(player.findViewById(R.id.player_shadowing_overlay)) ?: return
        playerSpeakingOverlayBinding.vm = this
        playerSpeakingOverlayBinding.lifecycleOwner = player
        playerSpeakingOverlayBinding.progress = player?.service?.playlistManager?.player?.progress
        playerSpeakingOverlayBinding.player = player
        playerSpeakingOverlayBinding.shadowingAB = player?.service?.playlistManager?.player?.shadowingABRepeat
        playerSpeakingOverlayBinding.recordBtn.setOnClickListener {
            player.handleMICPermission(false)
        }
    }


    fun loopOverNextCaption(): Boolean {
        player.service?.playlistManager?.player?.loopOverNextCaption()
        return true
    }

    fun appendNextCaption(): Boolean {
        player.service?.playlistManager?.player?.appendNextCaption()
        return true
    }

    fun loopOverPreviousCaption(): Boolean {
        player.service?.playlistManager?.player?.loopOverPreviousCaption()
        return true
    }

    fun appendPreviousCaption(): Boolean {
        player.service?.playlistManager?.player?.appendPreviousCaption()
        return true
    }

    private val increaseDecreaseValue = 100L
    fun increaseStartByOne(): Boolean {
        player.service?.playlistManager?.player?.increaseShadowingABStart(increaseDecreaseValue)
        return true
    }

    fun decreaseStartByOne(): Boolean {
        player.service?.playlistManager?.player?.decreaseShadowingABStart(increaseDecreaseValue)
        return true
    }

    fun increaseStopByOne(): Boolean {
        player.service?.playlistManager?.player?.increaseShadowingABStop(increaseDecreaseValue)
        return true
    }

    fun decreaseStopByOne(): Boolean {
        player.service?.playlistManager?.player?.decreaseShadowingABStop(increaseDecreaseValue)
        return true
    }

    fun togglePlayPause() {
        if (isRecording.get() == true)  audioRecorder.stopRecording(autoPlay = false)
        if (isRecordedPlaying.get() == true) audioRecorder.stopPlaying()

        player.service?.playlistManager?.player?.apply {
            if (isPlaying()) pause()
            else play()
        }
    }

    fun togglePlayPauseRecord() {
        player.lifecycleScope.launch {
            if (isPlaying.get() == true) player.service?.playlistManager?.pause()

            if (isRecording.get() == true) {
                audioRecorder.stopRecording()
                delay(10)
            }

            audioRecorder.togglePlayPauseRecord()
        }
    }

    fun toggleAudioRecord() {
        if (isPlaying.get() == true)  player.service?.playlistManager?.player?.pause()
        if (isRecordedPlaying.get() == true) audioRecorder.stopPlaying()
        audioRecorder.toggleAudioRecord()
        if (isRecordedFileExist.get() != true)
            isRecordedFileExist.set(isRecordedFileExist())
    }


    private fun recordingStarted() {
        isRecording.set(true)
    }

    private fun recordingStopped(autoPlay: Boolean) {
        isRecording.set(false)
        player.lifecycleScope.launch {
            // user played pause when recording, so recording stopped
            if (autoPlay && isPlaying.get() != true) {
                delay(100)
                audioRecorder.startPlaying(audioRecorder.getAudioPath())
            }
        }
    }



    private fun isRecordedFileExist() = File(audioRecorder.getAudioPath()).exists()

    private fun recordedPlaying() {
        isRecordedPlaying.set(true)
    }

    private fun recordedStopping() {
        isRecordedPlaying.set(false)
    }


    private fun toggleShadowing() {
        player.service?.playlistManager?.player?.apply {
            if (isShadowingModeEnabled) {
                disableShadowingMode()
            } else {
                enableShadowingMode()
//                player.overlayDelegate.updateSubtitlePositionWhenPlayerControllsIsVisible()
            }
        }
        player.service?.isPlaying?.let {
            player.subtitleDelegate.decideAboutCaptionButtonVisibility(it)
        }
    }

    private fun enableShadowingMode() {
        prevVideoUri = player.videoUri
        _shadowingMode.value = true
        player.subtitleDelegate.shadowingModeEnabled()
        player.service?.playlistManager?.player?.apply {
            shadowing?.isSelected = true
            setShadowingMode(true)
            player.overlayDelegate.showInfo(R.string.shadowing_mode_enabled, 1000)
            player.overlayDelegate.enableMinimizeMode()
            if (::playerSpeakingOverlayBinding.isInitialized) {
                playerSpeakingOverlayBinding.playerShadowingOverlay.setVisible()
                playerSpeakingOverlayBinding.playerOverlaySeekbar.setOnSeekBarChangeListener(player.seekListener)
            }
        }
    }

    private fun disableShadowingMode() {
        _shadowingMode.value = false
        if (isRecording.get() == true)  audioRecorder.stopRecording(autoPlay = false)
        player.subtitleDelegate.shadowingModeDisabled()
        player.service?.playlistManager?.player?.apply {
            shadowing?.isSelected = false
            setShadowingMode(false)
            player.overlayDelegate.showInfo(R.string.shadowin_mode_disabled, 1000)
            if (::playerSpeakingOverlayBinding.isInitialized) {
                playerSpeakingOverlayBinding.playerShadowingOverlay.setGone()
                playerSpeakingOverlayBinding.playerOverlaySeekbar.setOnSeekBarChangeListener(null)
            }

            player.overlayDelegate.disableMinimizeMode()
        }
    }

    private var prevVideoUri: Uri? = null
    fun newVideoUriPlaying(videoUri: Uri?) {
        // Disable shadowing mode when next video in playlist is playing
        if (_shadowingMode.value == true && prevVideoUri != videoUri) {
            disableShadowingMode()
        }

        prevVideoUri = videoUri
    }

    fun activityPaused() {
        if (isRecording.get() == true)  audioRecorder.stopRecording(autoPlay = false)
    }

    private val TWO_DIGITS: ThreadLocal<NumberFormat?> = object : ThreadLocal<NumberFormat?>() {
        override fun initialValue(): NumberFormat {
            val fmt = NumberFormat.getInstance(Locale.US)
            if (fmt is DecimalFormat) fmt.applyPattern("00")
            return fmt
        }
    }

    private val THREE_DIGITS: ThreadLocal<NumberFormat?> = object : ThreadLocal<NumberFormat?>() {
        override fun initialValue(): NumberFormat {
            val fmt = NumberFormat.getInstance(Locale.US)
            if (fmt is DecimalFormat) fmt.applyPattern("000")
            return fmt
        }
    }



    fun millisToStringWith(millis: Long): String {
        var millis = millis
        val sb = StringBuilder()
        if (millis < 0) {
            millis = -millis
            sb.append("-")
        }
        val mills = (millis % 1000).toInt()
        millis /= 1000
        val sec = (millis % 60).toInt()
        millis /= 60
        val min = (millis % 60).toInt()
        millis /= 60
        val hours = millis.toInt()
        sb.append(TWO_DIGITS.get()!!.format(sec.toLong())).append(":").append(THREE_DIGITS.get()!!.format(mills.toLong()))
        return sb.toString()
    }
}

@BindingAdapter("update")
fun setAmplitude(audioRecordView: AudioRecordView, data: Int) {
    if (data == -1) audioRecordView.recreate()
    else audioRecordView.update(data)
}

