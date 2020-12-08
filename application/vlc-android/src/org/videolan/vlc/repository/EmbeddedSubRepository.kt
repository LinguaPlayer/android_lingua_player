package org.videolan.vlc.repository

import android.content.Context
import android.net.Uri
import org.videolan.tools.SingletonHolder
import org.videolan.vlc.database.MediaDatabase
import org.videolan.vlc.mediadb.EmbeddedSubtitleDao
import org.videolan.vlc.mediadb.models.EmbeddedSub
import org.videolan.vlc.mediadb.models.Status

class EmbeddedSubRepository(private val embeddedSubtitleDao: EmbeddedSubtitleDao) {

    suspend fun addEmbeddedSubtitle(mediaPath: Uri, status: Status, embeddedIndex: Int) {
        val es = EmbeddedSub(mediaPath=mediaPath, status=status, embeddedIndex = embeddedIndex)
        embeddedSubtitleDao.insert(es)
    }

    suspend fun getEmbeddedSubtitles(mediaPath: Uri): List<EmbeddedSub> {
        return embeddedSubtitleDao.getEmbeddedSubtitles(mediaPath)
    }

    companion object : SingletonHolder<EmbeddedSubRepository, Context>({ EmbeddedSubRepository(MediaDatabase.getInstance(it).embeddedSubtitleDao()) })
}
