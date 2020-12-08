/*
 * ************************************************************************
 *  TrackAdapter.kt
 * *************************************************************************
 * Copyright © 2020 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 *
 *
 */

package org.videolan.vlc.gui.dialogs.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.videolan.libvlc.MediaPlayer
import org.videolan.vlc.databinding.VideoTrackItemBinding
import org.videolan.vlc.media.isPending
import org.videolan.vlc.media.isReady

class TrackAdapter(private val tracks: Array<MediaPlayer.TrackDescription>, val selectedTracks: List<MediaPlayer.TrackDescription>, val multiSelect: Boolean) : RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

    val mutableSelectedTracks = selectedTracks.toMutableSet()
    lateinit var trackSelectedListener: (MediaPlayer.TrackDescription) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = VideoTrackItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    fun setOnTrackSelectedListener(listener: (MediaPlayer.TrackDescription) -> Unit) {
        trackSelectedListener = listener
    }

    override fun getItemCount() = tracks.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tracks[position], mutableSelectedTracks.contains(tracks[position]))
    }

    inner class ViewHolder(val binding: VideoTrackItemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {

            itemView.setOnClickListener {
                if (tracks[layoutPosition].isReady()) {
                    if (multiSelect) {
                        val selected = mutableSelectedTracks.find { it == tracks[layoutPosition] }
                        if (selected != null) mutableSelectedTracks.remove(selected)
                        else mutableSelectedTracks.add(tracks[layoutPosition])
                    } else mutableSelectedTracks.apply {
                        clear()
                        add(tracks[layoutPosition])
                    }
                    notifyDataSetChanged()
                }
                trackSelectedListener.invoke(tracks[layoutPosition])
            }
        }

        fun bind(trackDescription: MediaPlayer.TrackDescription, selected: Boolean) {
            binding.track = trackDescription
            binding.selected = selected
            binding.pendingProgressBar.visibility = if (trackDescription.isPending()) View.VISIBLE else View.INVISIBLE
            binding.executePendingBindings()
        }
    }
}