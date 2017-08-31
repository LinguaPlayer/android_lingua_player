/*
 * *************************************************************************
 *  StartActivity.java
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

package org.videolan.vlc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

//import com.appodeal.ads.Appodeal;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.magnetadservices.sdk.MagnetSDK;


import org.videolan.libvlc.util.AndroidUtil;
import org.videolan.vlc.gui.AudioPlayerContainerActivity;
import org.videolan.vlc.gui.MainActivity;
import org.videolan.vlc.gui.SearchActivity;
import org.videolan.vlc.gui.tv.MainTvActivity;
import org.videolan.vlc.gui.tv.audioplayer.AudioPlayerActivity;
import org.videolan.vlc.gui.video.VideoPlayerActivity;
import org.videolan.vlc.media.MediaUtils;
import org.videolan.vlc.util.AndroidDevices;
import org.videolan.vlc.util.Dictionary.Dictionary;
import org.videolan.vlc.util.FileUtils;
import org.videolan.vlc.util.Permissions;

public class StartActivity extends Activity {

    public final static String TAG = "VLC/StartActivity";

    private static final String PREF_FIRST_RUN = "first_run";
    public static final String EXTRA_FIRST_RUN = "extra_first_run";
    public static final String EXTRA_UPGRADE = "extra_upgrade";
    public static final String DICTIONARY_STATUS = "dictionary_status";
    public static final float DICTIONAEY_SIZE = 73;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        boolean tv =  showTvUi();
        String action = intent != null ? intent.getAction(): null;

        //Initialize Magnet
        if(TextUtils.equals(BuildConfig.FLAVOR_type,"free")){
            if(!TextUtils.equals(BuildConfig.FLAVOR_market,"googleplay")) {
                MagnetSDK.initialize(getApplicationContext());
//                MagnetSDK.getSettings().setTestMode(true);
            }
//            else{
//                String appKey = "ddf381d16f86da3228c6ccfb6aaa68824020728c10980169";
//                Appodeal.disableLocationPermissionCheck();
//                Appodeal.setTesting(true);
//                Appodeal.initialize(this, appKey, Appodeal.SKIPPABLE_VIDEO);
//                Appodeal.setLogLevel(com.appodeal.ads.utils.Log.LogLevel.debug);
//
//            }
        }

        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            intent.setDataAndType(intent.getData(), intent.getType());
            if (intent.getType() != null && intent.getType().startsWith("video"))
                startActivity(intent.setClass(this, VideoPlayerActivity.class));
            else
                MediaUtils.openMediaNoUi(intent.getData());
            finish();
            return;
        }

        // Start application
        /* Get the current version from package */
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        int currentVersionNumber = BuildConfig.VERSION_CODE;
        int savedVersionNumber = settings.getInt(PREF_FIRST_RUN, -1);
        /* Check if it's the first run */
        boolean firstRun = savedVersionNumber == -1;
        boolean upgrade = firstRun || savedVersionNumber != currentVersionNumber;
        if (upgrade) {
            settings.edit().putInt(PREF_FIRST_RUN, currentVersionNumber).apply();
            prepareFFmpeg();
        }

        //0 : not ready
        //1 : error
        //2 : not enough storage error
        //3 : done
        final int dictionaryState = settings.getInt(DICTIONARY_STATUS, 0);
        if(upgrade || (dictionaryState != 3) ){
            final SharedPreferences fSettings = PreferenceManager.getDefaultSharedPreferences(this);
            VLCApplication.runBackground(new Runnable() {
                @Override
                public void run() {
                    if(FileUtils.getAvailableInternalMemorySize() < DICTIONAEY_SIZE) {
                        fSettings.edit().putInt(DICTIONARY_STATUS, 2).apply();
                        return;
                    }
                    String[] dictionaryValues = getResources().getStringArray(R.array.dictionaries_values);
                    boolean b1 = Dictionary.unpackZip(getApplicationContext(),dictionaryValues[0]);
                    boolean b2 = Dictionary.unpackZip(getApplicationContext(),dictionaryValues[1]);
                    if(b1 && b2)
                        fSettings.edit().putInt(DICTIONARY_STATUS, 3).apply();
                    else
                        fSettings.edit().putInt(DICTIONARY_STATUS, 1).apply();
                }
            });
        }

        startMedialibrary(firstRun, upgrade);
        // Route search query
        if (Intent.ACTION_SEARCH.equals(action) || "com.google.android.gms.actions.SEARCH_ACTION".equals(action)) {
            startActivity(intent.setClass(this, tv ? org.videolan.vlc.gui.tv.SearchActivity.class : SearchActivity.class));
            finish();
            return;
        } else if (MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH.equals(action)) {
            Intent serviceInent = new Intent(PlaybackService.ACTION_PLAY_FROM_SEARCH, null, this, PlaybackService.class)
                    .putExtra(PlaybackService.EXTRA_SEARCH_BUNDLE, intent.getExtras());
            startService(serviceInent);
        } else if (AudioPlayerContainerActivity.ACTION_SHOW_PLAYER.equals(action)) {
            startActivity(new Intent(this, tv ? AudioPlayerActivity.class : MainActivity.class));
        } else {
            startActivity(new Intent(this, tv ? MainTvActivity.class : MainActivity.class)
                    .putExtra(EXTRA_FIRST_RUN, firstRun)
                    .putExtra(EXTRA_UPGRADE, upgrade));
        }
        finish();
    }



    private void startMedialibrary(boolean firstRun, boolean upgrade) {
        if (!VLCApplication.getMLInstance().isInitiated() && Permissions.canReadStorage())
            startService(new Intent(MediaParsingService.ACTION_INIT, null, this, MediaParsingService.class)
                    .putExtra(EXTRA_FIRST_RUN, firstRun)
                    .putExtra(EXTRA_UPGRADE, upgrade));
    }

    private boolean showTvUi() {
        return AndroidUtil.isJellyBeanMR1OrLater && (AndroidDevices.isAndroidTv || !AndroidDevices.hasTsp ||
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean("tv_ui", false));
    }

    private void prepareFFmpeg(){
        FFmpeg ffmpeg = FFmpeg.getInstance(getApplicationContext());
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onStart() { }
                @Override
                public void onFailure() { }
                @Override
                public void onSuccess() { }
                @Override
                public void onFinish() { }
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
            Log.d("ffmpeg","NOTSupported");
        }
    }
}
