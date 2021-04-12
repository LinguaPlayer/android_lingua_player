/*
 * *************************************************************************
 *  PreferencesVideo.java
 * **************************************************************************
 *  Copyright © 2016 VLC authors and VideoLAN
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.television.ui.preferences

import android.annotation.TargetApi
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.preference.Preference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.videolan.resources.AndroidDevices
import org.videolan.resources.VLCInstance
import org.videolan.tools.*
import org.videolan.vlc.R
import org.videolan.vlc.gui.preferences.PreferencesActivity

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class PreferencesVideo : BasePreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun getXml() = R.xml.preferences_video

    override fun getTitleId() = R.string.video_prefs_category

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findPreference<Preference>("secondary_display_category")?.isVisible = false
        findPreference<Preference>("secondary_display_category_summary")?.isVisible = false
        findPreference<Preference>("enable_clone_mode")?.isVisible = false
        findPreference<Preference>(SAVE_BRIGHTNESS)?.isVisible = false
        findPreference<Preference>(ENABLE_DOUBLE_TAP_SEEK)?.isVisible = false
        findPreference<Preference>(ENABLE_VOLUME_GESTURE)?.isVisible = AndroidDevices.hasTsp
        findPreference<Preference>(ENABLE_BRIGHTNESS_GESTURE)?.isVisible = AndroidDevices.hasTsp
        findPreference<Preference>(POPUP_KEEPSCREEN)?.isVisible = false
        findPreference<Preference>(POPUP_FORCE_LEGACY)?.isVisible = false
    }

    override fun onStart() {
        super.onStart()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        preferenceScreen.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            VIDEO_HUD_TIMEOUT -> {
                Settings.videoHudDelay = sharedPreferences.getString(VIDEO_HUD_TIMEOUT, "2")?.toInt() ?: 2
            }
            "preferred_resolution" -> {
                VLCInstance.restart()
                (activity as? PreferencesActivity)?.restartMediaPlayer()
            }
        }
    }
}
