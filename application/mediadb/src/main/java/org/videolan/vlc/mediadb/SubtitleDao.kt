package org.videolan.vlc.mediadb

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.videolan.vlc.mediadb.models.ExternalSub
import org.videolan.vlc.mediadb.models.Subtitle

@Dao
interface SubtitleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(subtitle: Subtitle)

    @Query("SELECT * from Subtitle where mediaPath = :mediaPath")
    suspend fun getSubtitles(mediaPath: Uri): List<Subtitle>

    @Query("SELECT * from Subtitle where mediaPath = :mediaPath")
    fun getSubtitlesLiveData(mediaPath: Uri): LiveData<List<Subtitle>>

    @Query("SELECT * from Subtitle where mediaPath = :mediaPath and selected = 1")
    fun getSelectedSubtitlesLiveData(mediaPath: Uri): LiveData<List<Subtitle>>

    @Query("UPDATE Subtitle SET selected = :selected WHERE id = :id")
    fun updateSelected(id: Int, selected: Boolean)

    @Query("UPDATE Subtitle SET delay = :delay WHERE id = :id")
    suspend fun updateDelay(id: Int, delay: Long)




}
