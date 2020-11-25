package org.videolan.vlc.repository

import android.content.Context
import android.net.Uri
import org.videolan.tools.SingletonHolder
import org.videolan.vlc.mediadb.models.Subtitle

class SubtitlesRepository() {
    private val subtitleList = mutableListOf<Subtitle>()
    var id = 0

    fun addSubtitleTrack(mediaPath: Uri, subtitlePath: Uri): Boolean {
        val name = nameGenerator(subtitlePath)
        val language = getLanguageFromPath(subtitlePath)
        val s = Subtitle(id= id++,mediaPath = mediaPath, subtitlePath = subtitlePath, language = language, name = name, selected = false, delay = 0L)
        subtitleList.add(s)
        return true
    }

    fun getSpuTracks(mediaPath: Uri): List<Subtitle>? {
        return subtitleList.filter {
            it.mediaPath == mediaPath
        }
    }

    private fun getLanguageFromPath(subtitlePath: Uri): String {
       return "DUMMY_EN"
    }

    private fun nameGenerator(subtitlePath: Uri): String {
       return "DummyName"
    }

    companion object : SingletonHolder<SubtitlesRepository, Context>({ SubtitlesRepository(/*MediaDatabase.getInstance(it).subtitleDao()*/) })
}

