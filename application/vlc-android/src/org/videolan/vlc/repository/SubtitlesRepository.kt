package org.videolan.vlc.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import org.videolan.tools.SingletonHolder
import org.videolan.vlc.database.MediaDatabase
import org.videolan.vlc.mediadb.SubtitleDao
import org.videolan.vlc.mediadb.models.Subtitle

private const val TAG = "SubtitlesRepository"

class SubtitlesRepository(private val subtitleDao: SubtitleDao) {

    suspend fun addSubtitleTrack(mediaPath: Uri, subtitlePath: Uri, selected: Boolean): Boolean {
        val language = getLanguageFromPath(subtitlePath)
        val s = Subtitle(id= 0, mediaPath = mediaPath, subtitlePath = subtitlePath, language = language, selected = selected, delay = 0L)
        subtitleDao.insert(s)
        return true
    }

    suspend fun getSpuTracks(mediaPath: Uri): List<Subtitle>? {
        return subtitleDao.getSubtitles(mediaPath)
    }

    suspend fun getSelectedSpuTracks(mediaPath: Uri): List<Subtitle> {
        return subtitleDao.getSelectedSubtitles(mediaPath)
    }

    fun getSelectedSpuTracksLiveData(mediaPath: Uri): LiveData<List<Subtitle>> {
        return subtitleDao.getSelectedSubtitlesLiveData(mediaPath)
    }

    suspend fun toggleSelected(id: Int) {
        val isSelected = subtitleDao.getSubtitle(id).selected
        return subtitleDao.updateSelected(id, !isSelected)
    }

    suspend fun setDelay(id: Int, delay: Long) {
        subtitleDao.updateDelay(id, delay)
    }


    private fun getLanguageFromPath(subtitlePath: Uri): String {
       return "DUMMY_EN"
    }

    companion object : SingletonHolder<SubtitlesRepository, Context>({ SubtitlesRepository(MediaDatabase.getInstance(it).subtitleDao()) })
}

