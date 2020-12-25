package org.videolan.vlc.media

import android.content.Context
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.kazemihabib.cueplayer.util.Event
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.StateFlow
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.RendererItem
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.interfaces.IMediaList
import org.videolan.libvlc.interfaces.IVLCVout
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.VLCInstance
import org.videolan.resources.VLCOptions
import org.videolan.tools.*
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.repository.SlaveRepository
import org.videolan.vlc.subs.CaptionsData
import kotlin.math.abs

private const val TAG = "PlayerController"

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
class PlayerController(val context: Context) : IVLCVout.Callback, MediaPlayer.EventListener, CoroutineScope {

    override val coroutineContext = Dispatchers.Main.immediate + SupervisorJob()


    //    private val exceptionHandler by lazy(LazyThreadSafetyMode.NONE) { CoroutineExceptionHandler { _, _ -> onPlayerError() } }
    private val playerContext by lazy(LazyThreadSafetyMode.NONE) { newSingleThreadContext("vlc-player") }
    private val settings by lazy(LazyThreadSafetyMode.NONE) { Settings.getInstance(context) }
    val progress by lazy(LazyThreadSafetyMode.NONE) { MutableLiveData<Progress>().apply { value = Progress() } }
    private val slaveRepository by lazy { SlaveRepository.getInstance(context) }

    private lateinit var subtitleController: SubtitleController

    var mediaplayer = newMediaPlayer()
        private set


    var switchToVideo = false
    var seekable = false
    var pausable = false
    var previousMediaStats: IMedia.Stats? = null
        private set
    @Volatile
    var hasRenderer = false
        private set

    fun cancelCoroutine() {
        cancel()
        subtitleController.cancel()
    }

    fun getVout(): IVLCVout? = mediaplayer.vlcVout

    fun canDoPassthrough() = mediaplayer.hasMedia() && !mediaplayer.isReleased && mediaplayer.canDoPassthrough()

    fun getMedia(): IMedia? = mediaplayer.media

    fun play() {
        if (mediaplayer.hasMedia() && !mediaplayer.isReleased) mediaplayer.play()
    }

    fun pause(): Boolean {
        if (isPlaying() && mediaplayer.hasMedia() && pausable) {
            mediaplayer.pause()
            return true
        }
        return false
    }

    fun stop() {
        if (mediaplayer.hasMedia() && !mediaplayer.isReleased) mediaplayer.stop()
        setPlaybackStopped()
    }

    private fun releaseMedia() = mediaplayer.media?.let {
        it.setEventListener(null)
        it.release()
    }

    private var mediaplayerEventListener: MediaPlayerEventListener? = null
    internal suspend fun startPlayback(media: IMedia, listener: MediaPlayerEventListener, time: Long) {
        mediaplayerEventListener = listener
        resetPlaybackState(time, media.duration)
        mediaplayer.setEventListener(null)
        withContext(Dispatchers.IO) { if (!mediaplayer.isReleased) mediaplayer.media = media.apply { if (hasRenderer) parse() } }
        mediaplayer.setEventListener(this@PlayerController)
        if (!mediaplayer.isReleased) {
            mediaplayer.setEqualizer(VLCOptions.getEqualizerSetFromSettings(context))
            mediaplayer.setVideoTitleDisplay(MediaPlayer.Position.Disable, 0)
            mediaplayer.play()
        }
    }

    private fun resetPlaybackState(time: Long, duration: Long) {
        seekable = true
        pausable = true
        lastTime = time
        updateProgress(time, duration)
    }

    @MainThread
    fun restart() {
        val mp = mediaplayer
        mediaplayer = newMediaPlayer()
        release(mp)
    }

    fun seek(position: Long, length: Double = getLength().toDouble()) {
        if (length > 0.0) setPosition((position / length).toFloat())
        else setTime(position)
    }

    fun setPosition(position: Float) {
        if (seekable && mediaplayer.hasMedia() && !mediaplayer.isReleased) {
            mediaplayer.position = position
            Log.d(TAG, "setPosition: length: ${getLength()} ${getLength().toDouble() / position}")
            Log.d(TAG, "setPosition: ${mediaplayer.length / position}")
            subtitleController.getCaption((getLength() * position).toLong())
        }
    }

    fun setTime(time: Long) {
        if (seekable && mediaplayer.hasMedia() && !mediaplayer.isReleased) {
            mediaplayer.time = time
            subtitleController.getCaption(time)
        }
    }

    fun setTimeAndUpdateProgress(time: Long) {
        setTime(time)
        updateProgress(time)
    }

    fun isPlaying() = playbackState == PlaybackStateCompat.STATE_PLAYING

    fun isVideoPlaying() = !mediaplayer.isReleased && mediaplayer.vlcVout.areViewsAttached()

    fun canSwitchToVideo() = getVideoTracksCount() > 0

    fun getVideoTracksCount() = if (!mediaplayer.isReleased && mediaplayer.hasMedia()) mediaplayer.videoTracksCount else 0

    fun getVideoTracks(): Array<out MediaPlayer.TrackDescription>? = if (!mediaplayer.isReleased && mediaplayer.hasMedia()) mediaplayer.videoTracks else emptyArray()

    fun getVideoTrack() = if (!mediaplayer.isReleased && mediaplayer.hasMedia()) mediaplayer.videoTrack else -1

    fun getCurrentVideoTrack(): IMedia.VideoTrack? = if (!mediaplayer.isReleased && mediaplayer.hasMedia()) mediaplayer.currentVideoTrack else null

    fun getAudioTracksCount() = if (!mediaplayer.isReleased && mediaplayer.hasMedia()) mediaplayer.audioTracksCount else 0

    fun getAudioTracks(): Array<out MediaPlayer.TrackDescription>? = if (!mediaplayer.isReleased && mediaplayer.hasMedia()) mediaplayer.audioTracks else emptyArray()

    fun getAudioTrack() = if (!mediaplayer.isReleased && mediaplayer.hasMedia()) mediaplayer.audioTrack else -1

    fun setVideoTrack(index: Int) = !mediaplayer.isReleased && mediaplayer.hasMedia() && mediaplayer.setVideoTrack(index)

    fun setAudioTrack(index: Int) = !mediaplayer.isReleased && mediaplayer.hasMedia() && mediaplayer.setAudioTrack(index)

    fun setAudioDigitalOutputEnabled(enabled: Boolean) = !mediaplayer.isReleased && mediaplayer.setAudioDigitalOutputEnabled(enabled)

    fun getAudioDelay() = if (mediaplayer.hasMedia() && !mediaplayer.isReleased) mediaplayer.audioDelay else 0L

    fun getSpuDelay(): Long {
        return subtitleController.getSpuDelay()
//        return mediaplayer.spuDelay
    }

    fun getRate() = if (mediaplayer.hasMedia() && !mediaplayer.isReleased && playbackState != PlaybackStateCompat.STATE_STOPPED) mediaplayer.rate else 1.0f

    fun setSpuDelay(delay: Long): Boolean {
        return subtitleController.setSpuDelay(delay)
//        return mediaplayer.setSpuDelay(delay)
    }

    var isShadowingModeEnabled: Boolean = false
        private set

    private val shadowingABRepeat = ABRepeat()

    fun setABRepeat(start: Long, stop: Long) {
        shadowingABRepeat.start = start
        shadowingABRepeat.stop = stop
    }

    fun clearABRepeat() {
        shadowingABRepeat.start = -1
        shadowingABRepeat.stop = -1
    }

    fun setShadowingMode(enabled: Boolean) {
        isShadowingModeEnabled = enabled
        if (enabled) loopOverCaption(mediaplayer?.time ?: 0)
        else clearABRepeat()
    }


    private fun loopOver(lst: List<CaptionsData>) {
        val start = lst.minByOrNull { it.minStartTime }
        val stop = lst.maxByOrNull { it.maxEndTime }
        if (start != null && stop != null)
            setABRepeat( start = start.minStartTime, stop = stop.maxEndTime)
    }

    private fun loopOverCaption(time: Long) {
        if (numberOfParsedSubs == 0) return

        val captionsDataList = subtitleController.getCaption(time)

        if (captionsDataList.isNotEmpty()) { loopOver(captionsDataList) }
        else { if (!loopOverNextCaption()) loopOverPreviousCaption() }
    }

    fun loopOverNextCaption(): Boolean {
        if (numberOfParsedSubs > 0)
            getNextCaption(alsoSeekThere = true).apply {
                return if (this.isNotEmpty()) { loopOver(this); true }
                else { false }
            }
        else return false
    }

    fun loopOverPreviousCaption(): Boolean {
        if (numberOfParsedSubs > 0)
            getPreviousCaption(alsoSeekThere = true).apply {
                return if (this.isNotEmpty()) { loopOver(this); true }
                else { false }
            }
        else return false
    }

    fun setVideoTrackEnabled(enabled: Boolean) = mediaplayer.setVideoTrackEnabled(enabled)

    suspend fun addSubtitleTrack(videoUri: Uri?, path: String, select: Boolean): Boolean {
        return subtitleController.addSubtitleTrack(videoUri, path, select)
//        return mediaplayer.addSlave(IMedia.Slave.Type.Subtitle, path, select)
    }

    suspend fun addSubtitleTrack(videoUri: Uri?, uri: Uri, select: Boolean): Boolean {
        return subtitleController.addSubtitleTrack(videoUri, uri, select)
//        return mediaplayer.addSlave(IMedia.Slave.Type.Subtitle, uri, select)
    }

    val numberOfParsedSubs: Int
        get() = subtitleController.getNumberOfParsedSubs

    val numberOfParsedSubsFlow: StateFlow<Int>
        get() = subtitleController.getNumberOfParsedSubsFlow

    suspend fun parseSubtitles(subtitlePaths: List<String>) = subtitleController.parseSubtitle(subtitlePaths)

    val subtitleCaption: LiveData<ShowCaption>
        get() = subtitleController.subtitleCaption

    val subtitleInfo: LiveData<Event<ShowInfo>>
        get() = subtitleController.subtitleInfo

    suspend fun getSpuTracks(videoUri: Uri?): Array<out MediaPlayer.TrackDescription>? {
        return subtitleController.getSpuTracks(videoUri).toTypedArray()
//        return mediaplayer.spuTracks
    }

    suspend fun getSelectedSpuTracks(videoUri: Uri?): List<Int> {
        return subtitleController.getSpuTrack(videoUri)
//        return mediaplayer.spuTrack
    }

    fun updateCurrentCaption() {
        subtitleController.getCaption(mediaplayer.time)
    }

    fun getNextCaption(alsoSeekThere: Boolean) =
        subtitleController.getNextCaption(alsoSeekThere, ::setTimeAndUpdateProgress)

    fun getPreviousCaption(alsoSeekThere: Boolean) =
        subtitleController.getPreviousCaption(alsoSeekThere, ::setTimeAndUpdateProgress)

    fun enableSmartSubtitle() {
        subtitleController.isSmartSubtitleEnabled = true
        if (!mediaplayer.isPlaying) updateCurrentCaption()
    }

    fun disableSmartSubtitle() {
        subtitleController.isSmartSubtitleEnabled = false
        if (!mediaplayer.isPlaying) updateCurrentCaption()
    }

    fun isSmartSubtitleEnabled(): Boolean {
        return subtitleController.isSmartSubtitleEnabled
    }

    fun setSpuTrack(index: Int): Boolean {
        return subtitleController.setSpuTrack(index)
//        return mediaplayer.setSpuTrack(index)
    }

    suspend fun toggleSpuTrack(index: Int): Boolean {
        return subtitleController.toggleSpuTrack(index)
    }

    suspend fun getSpuTracksCount(videoUri: Uri?): Int {
        return subtitleController.getSpuTracksCount(videoUri)
//        return mediaplayer.spuTracksCount
    }

    suspend fun getEmbeddedSubsWhichAreUnattemptedToExtract(videoUri: Uri): List<SubtitleStream> =
            subtitleController.getEmbeddedSubsWhichAreUnattemptedToExtract(videoUri)

    suspend fun extractEmbeddedSubtitle(videoUri: Uri, index: Int): FFmpegResult =
            subtitleController.extractEmbeddedSubtitle(videoUri, index)

    fun setAudioDelay(delay: Long) = mediaplayer.setAudioDelay(delay)

    fun setEqualizer(equalizer: MediaPlayer.Equalizer?) = mediaplayer.setEqualizer(equalizer)

    @MainThread
    fun setVideoScale(scale: Float) {
        mediaplayer.scale = scale
    }

    fun setVideoAspectRatio(aspect: String?) {
        mediaplayer.aspectRatio = aspect
    }

    fun setRenderer(renderer: RendererItem?) {
        if (!mediaplayer.isReleased) mediaplayer.setRenderer(renderer)
        hasRenderer = renderer !== null
    }

    fun release(player: MediaPlayer = mediaplayer) {
        player.setEventListener(null)
        if (isVideoPlaying()) player.vlcVout.detachViews()
        releaseMedia()
        launch(Dispatchers.IO) {
            if (BuildConfig.DEBUG) { // Warn if player release is blocking
                try {
                    withTimeout(5000) { player.release() }
                } catch (exception: TimeoutCancellationException) {
                    launch { Toast.makeText(context, "media stop has timeouted!", Toast.LENGTH_LONG).show() }
                }
            } else player.release()
        }
        setPlaybackStopped()
    }

    fun setSlaves(media: IMedia, mw: MediaWrapper) = launch {
        if (mediaplayer.isReleased) return@launch
        val slaves = mw.slaves
        slaves?.let { it.forEach { slave -> media.addSlave(slave) } }
        media.release()
        slaveRepository.getSlaves(mw.location).forEach { slave ->
            if (!slaves.contains(slave)) mediaplayer.addSlave(slave.type, slave.uri.toUri(), false)

        }
        slaves?.let { slaveRepository.saveSlaves(mw) }
    }

    private fun newMediaPlayer(): MediaPlayer {
        return MediaPlayer(VLCInstance.getInstance(context)).apply {
            setAudioDigitalOutputEnabled(VLCOptions.isAudioDigitalOutputEnabled(settings))
            VLCOptions.getAout(settings)?.let { setAudioOutput(it) }
            setRenderer(PlaybackService.renderer.value)
            this.vlcVout.addCallback(this@PlayerController)
        }.also {
            // I instantiate here, so I everytime new MediaPlayer is created I have the new one
            subtitleController = SubtitleController(context, it)
        }
    }

    override fun onSurfacesCreated(vlcVout: IVLCVout?) {}

    override fun onSurfacesDestroyed(vlcVout: IVLCVout?) {
        switchToVideo = false
    }

    fun getCurrentTime() = progress.value?.time ?: 0L

    fun getLength() = progress.value?.length ?: 0L

    fun setRate(rate: Float, save: Boolean) {
        if (mediaplayer.isReleased) return
        mediaplayer.rate = rate
        if (save && settings.getBoolean(KEY_PLAYBACK_SPEED_PERSIST, false))
            settings.putSingle(KEY_PLAYBACK_RATE, rate)
    }

    /**
     * Update current media meta and return true if player needs to be updated
     *
     * @param id of the Meta event received, -1 for none
     * @return true if UI needs to be updated
     */
    internal fun updateCurrentMeta(id: Int, mw: MediaWrapper?): Boolean {
        if (id == IMedia.Meta.Publisher) return false
        mw?.updateMeta(mediaplayer)
        return id != IMedia.Meta.NowPlaying || mw?.nowPlaying !== null
    }

    /**
     * When changing current media, setPreviousStats is called to store statistics related to the
     * media. SetCurrentStats is called in the case where repeating is set to
     * PlaybackStateCompat.REPEAT_MODE_ONE, and the current media should not be released, as
     * it is still in use.
     */
    fun setCurrentStats() {
        val media = mediaplayer.media ?: return
        previousMediaStats = media.stats
    }

    fun setPreviousStats() {
        val media = mediaplayer.media ?: return
        previousMediaStats = media.stats
        media.release()
    }

    fun updateViewpoint(yaw: Float, pitch: Float, roll: Float, fov: Float, absolute: Boolean) = mediaplayer.updateViewpoint(yaw, pitch, roll, fov, absolute)

    fun navigate(where: Int) = mediaplayer.navigate(where)

    fun getChapters(title: Int): Array<out MediaPlayer.Chapter>? = if (!mediaplayer.isReleased) mediaplayer.getChapters(title) else emptyArray()

    fun getTitles(): Array<out MediaPlayer.Title>? = if (!mediaplayer.isReleased) mediaplayer.titles else emptyArray()

    fun getChapterIdx() = if (!mediaplayer.isReleased) mediaplayer.chapter else -1

    fun setChapterIdx(chapter: Int) {
        if (!mediaplayer.isReleased) mediaplayer.chapter = chapter
    }

    fun getTitleIdx() = if (!mediaplayer.isReleased) mediaplayer.title else -1

    fun setTitleIdx(title: Int) {
        if (!mediaplayer.isReleased) mediaplayer.title = title
    }

    fun getVolume() = if (!mediaplayer.isReleased) mediaplayer.volume else 100

    fun setVolume(volume: Int) = if (!mediaplayer.isReleased) mediaplayer.setVolume(volume) else -1

    suspend fun expand(): IMediaList? {
        return mediaplayer.media?.let {
            return withContext(playerContext) {
                mediaplayer.setEventListener(null)
                val items = it.subItems()
                it.release()
                mediaplayer.setEventListener(this@PlayerController)
                items
            }
        }
    }

    private var lastTime = 0L
    private val eventActor = actor<MediaPlayer.Event>(capacity = Channel.UNLIMITED, start = CoroutineStart.UNDISPATCHED) {
        for (event in channel) {
            when (event.type) {
                MediaPlayer.Event.Playing -> playbackState = PlaybackStateCompat.STATE_PLAYING
                MediaPlayer.Event.Paused -> playbackState = PlaybackStateCompat.STATE_PAUSED
                MediaPlayer.Event.EncounteredError -> setPlaybackStopped()
                MediaPlayer.Event.PausableChanged -> pausable = event.pausable
                MediaPlayer.Event.SeekableChanged -> seekable = event.seekable
                MediaPlayer.Event.LengthChanged -> updateProgress(newLength = event.lengthChanged)
                MediaPlayer.Event.TimeChanged -> {
                    val time = event.timeChanged
                    if (abs(time - lastTime) > 950L) {
                        updateProgress(newTime = time)
                        lastTime = time
                    }
                    subtitleController.getCaption(time)

                    if (shadowingABRepeat.start != -1L) {
                        if (time > shadowingABRepeat.stop) setTime(shadowingABRepeat.start)
                    }
                }
            }
            mediaplayerEventListener?.onEvent(event)
        }
    }

    @JvmOverloads
    fun updateProgress(newTime: Long = progress.value?.time
            ?: 0L, newLength: Long = progress.value?.length ?: 0L) {
        progress.value = progress.value?.apply { time = newTime; length = newLength }
    }

    override fun onEvent(event: MediaPlayer.Event?) {
        if (event != null) eventActor.offer(event)
    }

    private fun setPlaybackStopped() {
        playbackState = PlaybackStateCompat.STATE_STOPPED
        updateProgress(0L, 0L)
        lastTime = 0L
    }

    //    private fun onPlayerError() {
//        launch(UI) {
//            restart()
//            Toast.makeText(context, context.getString(R.string.feedback_player_crashed), Toast.LENGTH_LONG).show()
//        }
//    }
    companion object {
        @Volatile
        var playbackState = PlaybackStateCompat.STATE_NONE
            private set
    }
}

class Progress(var time: Long = 0L, var length: Long = 0L)

internal interface MediaPlayerEventListener {
    suspend fun onEvent(event: MediaPlayer.Event)
}

private fun Array<IMedia.Slave>?.contains(item: IMedia.Slave): Boolean {
    if (this == null) return false
    for (slave in this) if (slave.uri == item.uri) return true
    return false
}