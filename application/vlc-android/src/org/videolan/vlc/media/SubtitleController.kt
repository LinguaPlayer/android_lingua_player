package org.videolan.vlc.media

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.github.kazemihabib.cueplayer.util.Event
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.collect
import org.videolan.libvlc.MediaPlayer
import org.videolan.tools.*
import org.videolan.vlc.gui.dialogs.SubtitleItem
import org.videolan.vlc.mediadb.models.Status
import org.videolan.vlc.repository.EmbeddedSubRepository
import org.videolan.vlc.repository.SubtitlesRepository
import org.videolan.vlc.subs.CaptionsData
import org.videolan.vlc.subs.SubtitleParser
import org.videolan.vlc.util.FileUtils
import org.videolan.vlc.util.ReflectionHelper
import java.util.*


private const val TAG = "SubtitleController"
private const val INFO_TIMEOUT = 2000L

class SubtitleController(val context: Context, val mediaplayer: MediaPlayer) : CoroutineScope {

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

    suspend fun getSpuTracks(): List<out MediaPlayer.TrackDescription> {
        val videoUri = mediaplayer.media?.uri
        val pendingToExtractEmbeddedSubs = pendingExtractionTracksLiveData.value

        val addedSpuTracks = videoUri?.run {
            SubtitlesRepository.getInstance(context).getSpuTracks(this)
        } ?: listOf()

        val addedTrackDescriptions = addedSpuTracks.map { subtitle ->
            val name = if (subtitle.embedded) "Track ${subtitle.embeddedIndex}-[${subtitle.language}]"
            else FileUtils.getFileNameFromPath(subtitle.subtitlePath.path)
            ReflectionHelper.instantiateTrackDescription(subtitle.id, name)
        }

        val unExtractedEmbeddedTrackDescription = pendingToExtractEmbeddedSubs?.map { espu ->
            ReflectionHelper.instantiateTrackDescription(convertIndexToPending(espu.index), "Track ${espu.index}-[${espu.language}]")
        } ?: listOf()

        return (unExtractedEmbeddedTrackDescription + addedTrackDescriptions)
    }

    suspend fun getEmbeddedSubsWhichAreUnattemptedToExtract(videoUri: Uri): List<SubtitleStream> {
        val embeddedSpuTracks = getSubtitleStreams(videoUri)
        val dbEmbeddedSubs = EmbeddedSubRepository.getInstance(context).getEmbeddedSubtitles(videoUri)

        return embeddedSpuTracks.filter { subtitleStream -> dbEmbeddedSubs.find { it.embeddedIndex == subtitleStream.index } == null }
    }

    private suspend fun getFailedToExtractEmbeddedSubs(videoUri: Uri): List<SubtitleStream> {
        val embeddedSpuTracks = getSubtitleStreams(videoUri)
        val dbEmbeddedSubs = EmbeddedSubRepository.getInstance(context).getEmbeddedSubtitles(videoUri)

        return embeddedSpuTracks.filter { subtitleStream ->
            val embedded = dbEmbeddedSubs.find { it.embeddedIndex == subtitleStream.index }

            embedded != null && embedded.status == Status.FAILED
        }
    }

    suspend fun extractEmbeddedSubtitle(videoUri: Uri, index: Int): FFmpegResult {
        val embeddedRepo = EmbeddedSubRepository.getInstance(context)
        return try {
            val ffmpegResult = extractSubtitles(context = context, videoUri = videoUri, index = index)
            embeddedRepo.addEmbeddedSubtitle(mediaPath = videoUri, status = Status.SUCCESSFUL, embeddedIndex = index)
            SubtitlesRepository.getInstance(context).addSubtitleTrack(mediaPath = videoUri, subtitlePath = ffmpegResult.extractedPath, selected = true, lang = ffmpegResult.language, isEmbedded = true, embeddedIndex = ffmpegResult.index)
            ffmpegResult
        } catch (cancelled: FFmpegUserCancelledException) {
            cancelled.ffmpegResult

        } catch (failed: FFmpegFailedException) {
            embeddedRepo.addEmbeddedSubtitle(mediaPath = videoUri, status = Status.FAILED, embeddedIndex = index)
            failed.ffmpegResult
        }
    }

    suspend fun getSpuTrack(): List<Int> {
        return mediaplayer.media?.uri?.run {
            SubtitlesRepository.getInstance(context).getSelectedSpuTracks(this).map { it.id }
        } ?: listOf()
    }

    fun setSpuTrack(index: Int): Boolean {
        return when {
            isPending(index) -> {
                Log.i(TAG, "toggleSpuTrack: Embedded subtitle is pending, please wait")
                false
            }
            isFailed(index) -> {
                Log.i(TAG, "toggleSpuTrack: preparing this subtitle is failed")
                false
            }
            else -> {
                mediaplayer.setSpuTrack(index)
            }
        }
    }

    suspend fun toggleSpuTrack(index: Int): Boolean {
        return when {
            isPending(index) -> {
                Log.i(TAG, "toggleSpuTrack: Embedded subtitle is pending, please wait")
                false
            }
            isFailed(index) -> {
                Log.i(TAG, "toggleSpuTrack: preparing this subtitle is failed")
                false

            }
            else -> {
                SubtitlesRepository.getInstance(context).toggleSelected(index)
                true
            }
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
            //TODO: delete subtitle from database if failed to parse
            if (!it.successful)
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

        _subtitleCaption.value = ShowCaption(caption = stringCaptionData, isTouchable = false)

        return captionData
    }

    fun getNextCaption(alsoSeekThere: Boolean): List<CaptionsData> {
        val captionsDataList = subtitleParser.getNextCaption(false)
        _subtitleCaption.value = ShowCaption(caption = captionsDataList.apply {
            if (alsoSeekThere)
                minByOrNull { it.minStartTime }?.minStartTime?.run { /*seek(this, false) */ }
        }.flatMap { it.captionsOfThisTime.map { caption -> caption.content } }.joinToString(separator = "<br>"),
                isTouchable = false
        )
        return captionsDataList
    }

    fun getPreviousCaption(alsoSeekThere: Boolean): List<CaptionsData> {
        val captionsDataList = subtitleParser.getPreviousCaption(isSubtitleInDelayedMode)
        _subtitleCaption.value = ShowCaption(caption = captionsDataList.apply {
            if (alsoSeekThere) minByOrNull { it.minStartTime }?.minStartTime?.run {
//                seek( this, false )
            }
        }.flatMap { it.captionsOfThisTime.map { caption -> caption.content } }.joinToString(separator = "<br>"), isTouchable = false
        )
        return captionsDataList
    }

    val getNumberOfParsedSubs: Int
        get() = subtitleParser.getNumberOfParsedSubs()

}

fun MediaPlayer.TrackDescription.isReady() = id >= -1
fun MediaPlayer.TrackDescription.isPending() = isPending(this.id)
fun MediaPlayer.TrackDescription.isFailed() = isFailed(this.id)
private fun isPending(index: Int) = index < -100 && index > -200
private fun isFailed(index: Int) = index < -200
private fun convertIndexToPending(index: Int) = -100 - index
private fun convertPendingToIndex(pendingIndex: Int) = -100 - pendingIndex
private fun convertIndexToFailed(index: Int) = -200 - index
private fun convertFailedToIndex(failedIndex: Int) = -200 - failedIndex

//TODO: HABIB: Check really is available
fun isFFmpegAvailable() = false

data class ShowCaption(val caption: String, val isTouchable: Boolean)
data class ShowInfo(val message: String, val autoHide: Boolean, val autoHideTimeOut: Long = INFO_TIMEOUT)
