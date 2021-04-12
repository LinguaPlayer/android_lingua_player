/*
 * *************************************************************************
 *  PreferencesAudio.java
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
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.libvlc.util.HWDecoderUtil
import org.videolan.vlc.R
import org.videolan.tools.AUDIO_DUCKING
import org.videolan.tools.RESUME_PLAYBACK
import org.videolan.resources.VLCInstance

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class PreferencesAudio : BasePreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun getXml(): Int {
        return R.xml.preferences_audio
    }

    override fun getTitleId(): Int {
        return R.string.audio_prefs_category
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findPreference<Preference>("enable_headset_detection")?.isVisible = false
        findPreference<Preference>("enable_play_on_headset_insertion")?.isVisible = false
        findPreference<Preference>("headset_prefs_category")?.isVisible = false
        findPreference<Preference>(RESUME_PLAYBACK)?.isVisible = false
        findPreference<Preference>(AUDIO_DUCKING)?.isVisible = !AndroidUtil.isOOrLater

        val aout = HWDecoderUtil.getAudioOutputFromDevice()
        if (aout != HWDecoderUtil.AudioOutput.ALL) {
            /* no AudioOutput choice */
            findPreference<Preference>("aout")?.isVisible = false
        }
        updatePassThroughSummary()
        val opensles = "1" == preferenceManager.sharedPreferences.getString("aout", "0")
        if (opensles) findPreference<Preference>("audio_digital_output")?.isVisible = false
    }

    private fun updatePassThroughSummary() {
        val pt = preferenceManager.sharedPreferences.getBoolean("audio_digital_output", false)
        findPreference<Preference>("audio_digital_output")?.setSummary(if (pt) R.string.audio_digital_output_enabled else R.string.audio_digital_output_disabled)
    }

    override fun onStart() {
        super.onStart()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            "aout" -> {
                VLCInstance.restart()
                if (activity != null) (activity as PreferencesActivity).restartMediaPlayer()
                val opensles = "1" == preferenceManager.sharedPreferences.getString("aout", "0")
                if (opensles) findPreference<CheckBoxPreference>("audio_digital_output")?.isChecked = false
                findPreference<Preference>("audio_digital_output")?.isVisible = !opensles
            }
            "audio_digital_output" -> updatePassThroughSummary()
        }
    }
}