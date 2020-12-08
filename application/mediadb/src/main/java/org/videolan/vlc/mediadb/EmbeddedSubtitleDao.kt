package org.videolan.vlc.mediadb

import android.net.Uri
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.videolan.vlc.mediadb.models.EmbeddedSub

@Dao
interface EmbeddedSubtitleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subtitle: EmbeddedSub)


    @Query("SELECT * from EmbeddedSub where mediaPath = :mediaPath")
    suspend fun getEmbeddedSubtitles(mediaPath: Uri): List<EmbeddedSub>
}
