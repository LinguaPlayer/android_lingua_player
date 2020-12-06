package org.videolan.vlc.media

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.kazemihabib.cueplayer.util.Event
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.collect
import org.videolan.libvlc.MediaPlayer
import org.videolan.vlc.repository.SubtitlesRepository
import org.videolan.vlc.subs.CaptionsData
import org.videolan.vlc.subs.SubtitleParser
import org.videolan.vlc.util.FileUtils
import org.videolan.vlc.util.ReflectionHelper

private const val TAG = "SubtitleController"
private const val INFO_TIMEOUT = 2000L

class SubtitleController(val context: Context, val mediaplayer: MediaPlayer): CoroutineScope {

    override val coroutineContext = Dispatchers.Main.immediate + SupervisorJob()

    private val subtitleParser = SubtitleParser()

    fun getSpuDelay(): Long = mediaplayer.spuDelay

    // Habib: right now I'm not sure about an apropriate UI/UX for delay for each individual
    // subtitle, so for now I just use the VLC medialibrary
    // But I'll put the delay field for each subttile in the database
    fun setSpuDelay(delay: Long): Boolean {
        subtitleParser.setSubtitleDelay(delay / 1000)
        return mediaplayer.setSpuDelay(delay)
    }

    suspend fun addSubtitleTrack(path: String, select: Boolean): Boolean {
        mediaplayer.media?.uri?.run {
            return SubtitlesRepository.getInstance(context).addSubtitleTrack(mediaPath = this, subtitlePath = Uri.parse(path), selected = select)
        }
        return false
    }

    suspend fun addSubtitleTrack(uri: Uri, select: Boolean): Boolean {
        mediaplayer.media?.uri?.run {
            return SubtitlesRepository.getInstance(context).addSubtitleTrack(mediaPath = this, subtitlePath = uri, selected = select)
        }
        return false
    }


    suspend fun getSpuTracks(): Array<out MediaPlayer.TrackDescription>? {
        val embeddedSpuTracks = mediaplayer.spuTracks ?: arrayOf()

        val addedSpuTracks = mediaplayer.media?.uri?.run {
            SubtitlesRepository.getInstance(context).getSpuTracks(this)
        } ?: listOf()

        val addedTrackDescriptions = addedSpuTracks.map { subtitle ->

            val name = FileUtils.getFileNameFromPath(subtitle.subtitlePath.path)
            ReflectionHelper.instantiateTrackDescription(subtitle.id, name)
        }

        val embeddedTrackDescription = if (isFFmpegAvailable()) listOf<MediaPlayer.TrackDescription>()
        else embeddedSpuTracks.filter { it.id != -1 }.map { espu ->
            ReflectionHelper.instantiateTrackDescription(-1 - espu.id, espu.name)
        }

//        val trackDisable = ReflectionHelper.instantiateTrackDescription(-1, "Disable")

//        val tracks: MutableList<MediaPlayer.TrackDescription> = (embeddedTrackDescription + addedTrackDescriptions) as MutableList<MediaPlayer.TrackDescription>
//        if (tracks.isNotEmpty()) tracks.add(0, trackDisable)
        return (embeddedTrackDescription + addedTrackDescriptions).toTypedArray()
    }

    suspend fun getSpuTrack(): List<Int> {
        return mediaplayer.media?.uri?.run {
            SubtitlesRepository.getInstance(context).getSelectedSpuTracks(this).map { it.id }
        } ?: listOf()
    }

    fun setSpuTrack(index: Int): Boolean {
        return if (index < -1) {
            Log.i(TAG, "setSpuTrack: Embedded subtitles are not available, please install ffmpeg")
            false
        } else {
            mediaplayer.setSpuTrack(index)
        }
    }

    suspend fun toggleSpuTrack(index: Int): Boolean {
        return if (index < -1) {
            false
        } else {
            SubtitlesRepository.getInstance(context).toggleSelected(index)
            true
        }
    }

    suspend fun getSpuTracksCount(): Int {
        return mediaplayer.media?.uri?.run {
            SubtitlesRepository.getInstance(context).getSpuTracks(this)?.size
        } ?: 0
    }

    private val isSubtitleInDelayedMode = false
    private val _subtitleCaption = MutableLiveData<ShowCaption>()
    val subtitleCaption: LiveData<ShowCaption>
        get() = _subtitleCaption

    private val _subtitleInfo = MutableLiveData<Event<ShowInfo>>()
    val subtitleInfo: LiveData<Event<ShowInfo>>
        get() = _subtitleInfo


    //This acotr puts a delay between infos
    private val infoActor = actor<ShowInfo> {
        for (event in channel) {
            _subtitleInfo.value = Event(event)
            delay(2000)
        }
    }

    suspend fun parseSubtitle(subtitlePaths: List<String>) {
        subtitleParser.parseAsTimedTextObject(context, subtitlePaths).collect {
            infoActor.send(ShowInfo(it.error, autoHide = true))
        }

        subtitleParser.setSubtitleDelay(getSpuDelay() / 1000)

        // To update immediately in pause mode
        getCaption(mediaplayer.time)
    }

    private var prevCaption = ""
    fun getCaption(time: Long): List<CaptionsData> {
        val captionData = subtitleParser.getCaption(isSubtitleInDelayedMode, time)

        val stringCaptionData = captionData.flatMap {
            it.captionsOfThisTime.map { caption -> caption.content }
        }.joinToString(separator = "<br>")

        if (prevCaption == stringCaptionData) return captionData

        prevCaption = stringCaptionData

        _subtitleCaption.value = ShowCaption( caption = stringCaptionData, isTouchable = false )

        return captionData
    }

    fun getNextCaption(alsoSeekThere: Boolean): List<CaptionsData> {
        val captionsDataList = subtitleParser.getNextCaption(false)
        _subtitleCaption.value = ShowCaption(caption = captionsDataList.apply {
            if (alsoSeekThere)
                minBy { it.minStartTime }?.minStartTime?.run { /*seek(this, false) */}
        }.flatMap { it.captionsOfThisTime.map { caption -> caption.content }}.joinToString(separator = "<br>"),
                isTouchable = false
        )
        return captionsDataList
    }

    fun getPreviousCaption(alsoSeekThere: Boolean): List<CaptionsData> {
        val captionsDataList = subtitleParser.getPreviousCaption(isSubtitleInDelayedMode)
        _subtitleCaption.value = ShowCaption(caption = captionsDataList.apply {
            if (alsoSeekThere) minBy { it.minStartTime }?.minStartTime?.run {
//                seek( this, false )
            }
        }.flatMap { it.captionsOfThisTime .map {caption ->  caption.content }}.joinToString(separator = "<br>")
                ,isTouchable = false
        )
        return captionsDataList
    }

    val getNumberOfParsedSubs: Int
        get() = subtitleParser.getNumberOfParsedSubs()

}

fun MediaPlayer.TrackDescription.isParseable() = id >= -1

//TODO: HABIB: Check really is available
fun isFFmpegAvailable() = false

data class ShowCaption(val caption: String, val isTouchable: Boolean)
data class ShowInfo(val message: String, val autoHide: Boolean, val autoHideTimeOut: Long= INFO_TIMEOUT)
