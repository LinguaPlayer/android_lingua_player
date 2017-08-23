package org.videolan.vlc.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.widget.Toast;

import net.hockeyapp.android.FeedbackManager;

import org.videolan.vlc.BuildConfig;
import org.videolan.vlc.R;
import org.videolan.vlc.gui.MainActivity;

/**
 * Created by habib on 7/25/17.
 */

public class RateUtils {
    public final static String TAG = "VLC/RateUtils";
    private static AlertDialog mRateAppDialog;
    public static void showUserLoveAppDialog(final Context context) {
        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = mSettings.edit();

        mRateAppDialog = new AlertDialog.Builder(context)
                .setMessage(R.string.do_you_love_app)
                .setPositiveButton(R.string.I_love_it, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        showRateAppDialog(context);
                    }
                })
                .setNegativeButton(R.string.I_dont_love_it, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        editor.putBoolean("show_rate_request", false).commit();
                        Toast.makeText(context , R.string.feedback, Toast.LENGTH_LONG).show();
                        feedback(context);
                    }
                }) .create();

        mRateAppDialog.setCancelable(true);
        mRateAppDialog.show();
    }

    public static void showRateAppDialog(final Context context) {
        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = mSettings.edit();

        mRateAppDialog = new AlertDialog.Builder(context)
                .setMessage(R.string.ask_to_rate)
                .setPositiveButton(R.string.rate_now, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        editor.putBoolean("show_rate_request", false).commit();
                        rateApp(context);
                    }
                })
                .setNegativeButton(R.string.rate_later, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(context , R.string.feedback, Toast.LENGTH_LONG).show();
                        feedback(context);
                    }

                })
                .setNeutralButton(R.string.rate_never, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        editor.putBoolean("show_rate_request", false).commit();
                    }
                })
                .create();

        mRateAppDialog.setCancelable(true);
        mRateAppDialog.show();
    }

    public static void feedback(Context context){
        FeedbackManager.showFeedbackActivity(context);
    }
    public static void rateApp(Context context){
        String market = BuildConfig.FLAVOR_market;
        Intent intent = new Intent();
        if(market.equals("bazaar")) {
            intent.setAction(Intent.ACTION_EDIT);
            intent.setData(Uri.parse("bazaar://details?id=" + "ir.habibkazemi.linguaplayer.pro" ));
            intent.setPackage("com.farsitel.bazaar");
            if(intent.resolveActivity(context.getPackageManager()) != null)
                context.startActivity(intent);
            else{
                Toast.makeText(context, R.string.bazaar_is_not_installed, Toast.LENGTH_SHORT);
            }
        }
        else if(market.equals("myket")) {
            String url= "myket://comment?id=ir.habibkazemi.linguaplayer.pro";
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            if(intent.resolveActivity(context.getPackageManager()) != null)
                context.startActivity(intent);
            else{
                Toast.makeText(context, R.string.myket_is_not_installed, Toast.LENGTH_SHORT);
            }
        }
        else if(market.equals(("googleplay"))){
            Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            // To count with Play market backstack, After pressing back button,
            // to taken back to our application, we need to add following flags to intent.
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try {
                context.startActivity(goToMarket);
            } catch (ActivityNotFoundException e) {
                context.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
            }

        }
    }
}
