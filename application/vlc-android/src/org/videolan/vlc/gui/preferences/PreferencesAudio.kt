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

package org.videolan.vlc.gui.preferences

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import kotlinx.coroutines.*
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.libvlc.util.HWDecoderUtil
import org.videolan.vlc.R
import org.videolan.tools.AUDIO_DUCKING
import org.videolan.resources.AndroidDevices
import org.videolan.tools.RESUME_PLAYBACK
import org.videolan.resources.VLCInstance
import org.videolan.vlc.gui.browser.EXTRA_MRL
import org.videolan.vlc.gui.browser.FilePickerActivity
import org.videolan.vlc.gui.browser.KEY_PICKER_TYPE
import org.videolan.vlc.gui.helpers.UiTools
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.providers.PickerType

private const val FILE_PICKER_RESULT_CODE = 10000

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class PreferencesAudio : BasePreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun getXml() = R.xml.preferences_audio

    override fun getTitleId() = R.string.audio_prefs_category

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findPreference<Preference>(AUDIO_DUCKING)?.isVisible = !AndroidUtil.isOOrLater
        findPreference<Preference>(RESUME_PLAYBACK)?.isVisible = AndroidDevices.isPhone
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

    override fun onStop() {
        super.onStop()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == null) return false
        when (preference.key) {
            "enable_headset_detection" -> {
                (requireActivity() as PreferencesActivity).detectHeadset((preference as TwoStatePreference).isChecked)
                return true
            }
            "soundfont" -> {
                val filePickerIntent = Intent(requireContext(), FilePickerActivity::class.java)
                filePickerIntent.putExtra(KEY_PICKER_TYPE, PickerType.SOUNDFONT.ordinal)
                startActivityForResult(filePickerIntent, FILE_PICKER_RESULT_CODE)
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return
        if (requestCode == FILE_PICKER_RESULT_CODE) {
            if (data.hasExtra(EXTRA_MRL)) {
                lifecycleScope.launch {
                    MediaUtils.useAsSoundFont(requireActivity(), Uri.parse(data.getStringExtra(EXTRA_MRL)))
                }
                VLCInstance.restart()
                UiTools.restartDialog(requireActivity())
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val activity = activity ?: return
        when (key) {
            "aout" -> {
                VLCInstance.restart()
                (activity as PreferencesActivity).restartMediaPlayer()
                val opensles = "1" == preferenceManager.sharedPreferences.getString("aout", "0")
                if (opensles) findPreference<CheckBoxPreference>("audio_digital_output")?.isChecked = false
                findPreference<Preference>("audio_digital_output")?.isVisible = !opensles
            }
            "audio_digital_output" -> updatePassThroughSummary()
        }
    }
}