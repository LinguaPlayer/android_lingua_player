package org.videolan.vlc.mediadb.models

import android.net.Uri
import androidx.room.Entity

@Entity(primaryKeys = ["mediaPath", "embeddedIndex"])
class EmbeddedSub(
        val mediaPath: Uri,
        val embeddedIndex: Int,
        val status: Status,
)

enum class Status {
        SUCCESSFUL,
        FAILED
}