package org.videolan.vlc.mediadb.models

import android.net.Uri
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["mediaPath", "subtitlePath"], unique = true)])
data class Subtitle(
        @PrimaryKey(autoGenerate = true)
        val id: Int,
        val mediaPath: Uri,
        val subtitlePath: Uri,
        val language: String,
        val name: String,
        val selected: Boolean,
        val delay: Long
)
