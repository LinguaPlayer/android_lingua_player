package org.videolan.vlc.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.videolan.tools.SingletonHolder
import org.videolan.vlc.database.MediaDatabase
import org.videolan.vlc.mediadb.SubtitleDao
import org.videolan.vlc.mediadb.models.Subtitle

class SubtitlesRepository(private val subtitleDao: SubtitleDao) {

    suspend fun addSubtitleTrack(mediaPath: Uri, subtitlePath: Uri, selected: Boolean): Boolean {
        val language = getLanguageFromPath(subtitlePath)
        val s = Subtitle(id= 0, mediaPath = mediaPath, subtitlePath = subtitlePath, language = language, selected = /*TODO HABIB UPDATE THIS LATER*/ false, delay = 0L)
        subtitleDao.insert(s)
        return true
    }

    suspend fun getSpuTracks(mediaPath: Uri): List<Subtitle>? {
        return subtitleDao.getSubtitles(mediaPath)
    }

    fun getSelectedSpuTracksLiveData(mediaPath: Uri): LiveData<List<Subtitle>> {
        return subtitleDao.getSelectedSubtitlesLiveData(mediaPath)
    }

    fun updateSelected(id: Int, selected: Boolean){
        return subtitleDao.updateSelected(id, selected)
    }

    suspend fun setDelay(id: Int, delay: Long) {
        subtitleDao.updateDelay(id, delay)
    }


    private fun getLanguageFromPath(subtitlePath: Uri): String {
       return "DUMMY_EN"
    }

    companion object : SingletonHolder<SubtitlesRepository, Context>({ SubtitlesRepository(MediaDatabase.getInstance(it).subtitleDao()) })
}

