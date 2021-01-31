package org.videolan.vlc.gui.video

import android.content.DialogInterface
import android.net.Uri
import android.widget.ImageButton
import androidx.appcompat.widget.ViewStubCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.tools.setGone
import org.videolan.tools.setVisible
import org.videolan.vlc.R
import org.videolan.vlc.databinding.PlayerShadowingOverlayBinding
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.util.*
import java.io.File

private const val TAG = "ShadowingOverlayDelegat"
class ShadowingOverlayDelegate(private val player: VideoPlayerActivity) {

    private val shadowing: ImageButton? = player.findViewById(R.id.shadowing_mode)
    val audioRecorder = AudioRecorder(player.applicationContext)

    private val audioRecordEventsObserver = Observer<AudioRecordEvents> {
        when(it) {
            is RecordingStarted -> { recordingStarted() }
            is RecordingStopped -> { recordingStopped(it.audoPlay) }
            is RecordedStopped -> { recordedStopping() }
            is RecordedPlaying -> { recordedPlaying() }
            is RecordNone -> {}
        }
    }

    fun initShadowing() {
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
}
