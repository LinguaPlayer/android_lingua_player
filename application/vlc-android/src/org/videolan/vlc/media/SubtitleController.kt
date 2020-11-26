package org.videolan.vlc.media

import android.content.Context
import android.net.Uri
import android.util.Log
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.vlc.repository.SubtitlesRepository
import org.videolan.vlc.util.ReflectionHelper

private const val TAG = "SubtitleController"

class SubtitleController(val context: Context, val mediaplayer: MediaPlayer) {
    init {
        Log.d(TAG, "initialized ")
    }

    fun getSpuDelay(): Long = mediaplayer.spuDelay

    // Habib: right now I'm not sure about an apropriate UI/UX for delay for each individual
    // subtitle, so for now I just use the VLC medialibrary
    // But I'll put the delay field for each subttile in the database
    fun setSpuDelay(delay: Long): Boolean {
        return mediaplayer.setSpuDelay(delay)
    }

    fun addSubtitleTrack(path: String, select: Boolean): Boolean {
        return mediaplayer.addSlave(IMedia.Slave.Type.Subtitle, path, select)
    }

    fun addSubtitleTrack(uri: Uri, select: Boolean): Boolean {
        mediaplayer.media?.uri?.run {
            return SubtitlesRepository.getInstance(context).addSubtitleTrack(this, uri)
        }
        return false
//        mediaplayer.addSlave(IMedia.Slave.Type.Subtitle, uri, select)
    }


    fun getSpuTracks(): Array<out MediaPlayer.TrackDescription>? {
        val embeddedSpuTracks = mediaplayer.spuTracks ?: arrayOf()

        val addedSpuTracks = mediaplayer.media?.uri?.run {
            SubtitlesRepository.getInstance(context).getSpuTracks(this)
        } ?: listOf()

        val addedTrackDescriptions = addedSpuTracks.map { subtitle ->
            Log.d(TAG, "getSpuTracks: added ${subtitle.id}  ${subtitle.name}")
            ReflectionHelper.instantiateTrackDescription(subtitle.id, subtitle.name)
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
