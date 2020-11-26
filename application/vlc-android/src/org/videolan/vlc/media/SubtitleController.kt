package org.videolan.vlc.media

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.vlc.repository.SubtitlesRepository
import org.videolan.vlc.util.FileUtils
import org.videolan.vlc.util.ReflectionHelper

private const val TAG = "SubtitleController"

class SubtitleController(val context: Context, val mediaplayer: MediaPlayer): CoroutineScope {
    override val coroutineContext = Dispatchers.Main.immediate + SupervisorJob()

    fun getSpuDelay(): Long = mediaplayer.spuDelay

    // Habib: right now I'm not sure about an apropriate UI/UX for delay for each individual
    // subtitle, so for now I just use the VLC medialibrary
    // But I'll put the delay field for each subttile in the database
    fun setSpuDelay(delay: Long): Boolean {
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
            Log.d(TAG, "getSpuTracks: added ${subtitle.subtitlePath} ${subtitle.id}  ${name}")

            ReflectionHelper.instantiateTrackDescription(subtitle.id, name)
        }

        val embeddedTrackDescription = if (isFFmpegAvailable()) listOf<MediaPlayer.TrackDescription>()
        else embeddedSpuTracks.filter { it.id != -1 }.map { espu ->
            Log.d(TAG, "getSpuTracks: embedded ${espu.name} ${espu.id}")
            ReflectionHelper.instantiateTrackDescription(-1 - espu.id, espu.name)
        }

        val trackDisable = ReflectionHelper.instantiateTrackDescription(-1, "Disable")

        val tracks: MutableList<MediaPlayer.TrackDescription> = (embeddedTrackDescription + addedTrackDescriptions) as MutableList<MediaPlayer.TrackDescription>
        if (tracks.isNotEmpty()) tracks.add(0, trackDisable)
        return tracks.toTypedArray()
    }

    // TODO: HABIB: UPDATE THIS LATER TO SUPPORT MULTIPLE SUBTITLE
    fun getSpuTrack(): Int = mediaplayer.spuTrack

    fun setSpuTrack(index: Int): Boolean {
        return if (index < -1) {
            Log.i(TAG, "setSpuTrack: Embedded subtitles are not available, please install ffmpeg")
            false
        } else {
            mediaplayer.setSpuTrack(index)
        }
    }

    fun getSpuTracksCount(): Int = mediaplayer.spuTracksCount
}

fun MediaPlayer.TrackDescription.isParseable() = id >= -1

//TODO: HABIB: Check really is available
fun isFFmpegAvailable() = false