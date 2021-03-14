/*****************************************************************************
 * PreferencesActivity.java
 *
 * Copyright © 2011-2014 VLC authors and VideoLAN
 *
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
 */

package org.videolan.vlc.gui.preferences

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.videolan.tools.RESULT_RESTART
import org.videolan.tools.RESULT_RESTART_APP
import org.videolan.tools.RESULT_UPDATE_ARTISTS
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.gui.BaseActivity
import org.videolan.vlc.gui.preferences.search.PreferenceSearchActivity

const val EXTRA_PREF_END_POINT = "extra_pref_end_point"
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class PreferencesActivity : BaseActivity() {

    private val searchRequestCode = 167
    private var mAppBarLayout: AppBarLayout? = null
    override val displayTitle = true
    override fun getSnackAnchorView(): View? = findViewById(android.R.id.content)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.preferences_activity)
        setSupportActionBar(findViewById<View>(R.id.main_toolbar) as Toolbar)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_placeholder, PreferencesFragment().apply { if (intent.hasExtra(EXTRA_PREF_END_POINT)) arguments = bundleOf(EXTRA_PREF_END_POINT to intent.getParcelableExtra(EXTRA_PREF_END_POINT)) })
                    .commit()
        }
        mAppBarLayout = findViewById(R.id.appbar)
        mAppBarLayout!!.post { ViewCompat.setElevation(mAppBarLayout!!, resources.getDimensionPixelSize(R.dimen.default_appbar_elevation).toFloat()) }
    }

    internal fun expandBar() {
        mAppBarLayout!!.setExpanded(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_prefs, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (!supportFragmentManager.popBackStackImmediate())
                    finish()
                return true
            }
            R.id.menu_pref_search -> {
                startActivityForResult(Intent(this, PreferenceSearchActivity::class.java), searchRequestCode)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == searchRequestCode && resultCode == RESULT_OK) {
            data?.extras?.getParcelable<PreferenceItem>(EXTRA_PREF_END_POINT)?.let {
                supportFragmentManager.popBackStack()
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_placeholder, PreferencesFragment().apply { arguments = bundleOf(EXTRA_PREF_END_POINT to it) })
                        .commit()
            }
        }

    }

    fun restartMediaPlayer() {
        val le = PlaybackService.restartPlayer
        if (le.hasObservers()) le.value = true
    }

    fun exitAndRescan() {
        setRestart()
        val intent = intent
        finish()
        startActivity(intent)
    }

    fun setRestart() {
        setResult(RESULT_RESTART)
    }

    fun setRestartApp() {
        setResult(RESULT_RESTART_APP)
    }

    fun updateArtists() {
        setResult(RESULT_UPDATE_ARTISTS)
    }

    fun detectHeadset(detect: Boolean) {
        val le = PlaybackService.headSetDetection
        if (le.hasObservers()) le.value = detect
    }
}
