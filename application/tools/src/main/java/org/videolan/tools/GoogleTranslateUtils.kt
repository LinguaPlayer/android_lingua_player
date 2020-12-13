package org.videolan.tools

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.arthenica.mobileffmpeg.Config.getPackageName


private const val TAG = "GoogleTranslateUtils"

fun installGoogleTranslate(context: Context) {
    val appPackageName = "com.google.android.apps.translate"
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
    } catch (anfe: ActivityNotFoundException) {
//        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
        Log.e(TAG, "installGoogleTranslate: $anfe")
    }
}

fun translate(text: String, context: Context): Boolean {
    val intent = Intent()
    intent.type = "text/plain"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        intent.action = Intent.ACTION_PROCESS_TEXT
        intent.putExtra(Intent.EXTRA_PROCESS_TEXT, text)
    } else {
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_TEXT, text)
    }

    for (resolveInfo in context.packageManager.queryIntentActivities(intent, 0)) {
        Log.d(TAG, "translate: we reached inside for loop ${resolveInfo.activityInfo.packageName}")
        if (resolveInfo.activityInfo.packageName.contains("com.google.android.apps.translate")) {
            intent.component = ComponentName(
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name)
            context.startActivity(intent)
            return true
        }
    }

    return false
}