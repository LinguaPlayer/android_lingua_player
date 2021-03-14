/*
 * *************************************************************************
 *  BasePreferenceFragment.java
 * **************************************************************************
 *  Copyright © 2015 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
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

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.videolan.vlc.R
import org.videolan.vlc.gui.preferences.hack.MultiSelectListPreferenceDialogFragmentCompat

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    protected abstract fun getXml(): Int
    protected abstract fun getTitleId(): Int

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(getXml())
    }

    override fun onStart() {
        super.onStart()
        val activity = activity as PreferencesActivity?
        if (activity != null) {
            activity.expandBar()
            if (activity.supportActionBar != null && getTitleId() != 0)
                activity.supportActionBar!!.title = getString(getTitleId())
        }
    }

    protected fun loadFragment(fragment: Fragment) {
        activity!!.supportFragmentManager.beginTransaction().replace(R.id.fragment_placeholder, fragment)
                .addToBackStack("main")
                .commit()
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is MultiSelectListPreference) {
            val dialogFragment = MultiSelectListPreferenceDialogFragmentCompat.newInstance(preference.getKey())
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(fragmentManager!!, DIALOG_FRAGMENT_TAG)
            return
        }
        super.onDisplayPreferenceDialog(preference)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getParcelable<PreferenceItem>(EXTRA_PREF_END_POINT)?.let { endPoint ->
            selectPreference(endPoint.key)
        }
    }

    companion object {

        private const val DIALOG_FRAGMENT_TAG = "android.support.v7.preference.PreferenceFragment.DIALOG"
    }

    private fun selectPreference(key: String) {
        scrollToPreference(key)
        findPreference<Preference>(key)?.isSelectable = true
        listView?.postDelayed({
            (listView?.adapter as? PreferenceGroup.PreferencePositionCallback)?.let { adapter ->
                listView?.findViewHolderForAdapterPosition(adapter.getPreferenceAdapterPosition(key))?.itemView?.let { itemView ->
                    listView?.postDelayed({
                        itemView.isPressed = true
                    }, 600)
                }
            }
        }, 200)
    }
}
