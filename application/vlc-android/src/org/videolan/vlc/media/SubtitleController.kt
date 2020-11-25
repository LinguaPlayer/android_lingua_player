package org.videolan.vlc.media

import android.content.Context
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia

class SubtitleController(val context: Context, val mediaplayer: MediaPlayer) {

    fun getSpuDelay(): Long = if (mediaplayer.hasMedia() && !mediaplayer.isReleased) mediaplayer.spuDelay else 0L

    fun getRate(): Float = if (mediaplayer.hasMedia() && !mediaplayer.isReleased && PlayerController.playbackState != PlaybackStateCompat.STATE_STOPPED) mediaplayer.rate else 1.0f

    fun setSpuDelay(delay: Long): Boolean = mediaplayer.setSpuDelay(delay)

    fun setVideoTrackEnabled(enabled: Boolean): Unit = mediaplayer.setVideoTrackEnabled(enabled)

    fun addSubtitleTrack(path: String, select: Boolean): Boolean = mediaplayer.addSlave(IMedia.Slave.Type.Subtitle, path, select)

    fun addSubtitleTrack(uri: Uri, select: Boolean): Boolean = mediaplayer.addSlave(IMedia.Slave.Type.Subtitle, uri, select)

    fun getSpuTracks(): Array<out MediaPlayer.TrackDescription>? = mediaplayer.spuTracks

    fun getSpuTrack(): Int = mediaplayer.spuTrack

    fun setSpuTrack(index: Int): Boolean = mediaplayer.setSpuTrack(index)

    fun getSpuTracksCount(): Int = mediaplayer.spuTracksCount
}