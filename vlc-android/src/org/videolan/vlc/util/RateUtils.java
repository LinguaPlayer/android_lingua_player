package org.videolan.vlc.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.widget.Toast;

import org.videolan.vlc.BuildConfig;
import org.videolan.vlc.R;

/**
 * Created by habib on 7/25/17.
 */

public class RateUtils {
    public final static String TAG = "VLC/RateUtils";
    private static AlertDialog mRateAppDialog;
    public static void showRateAppDialog(final Context context) {
        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = mSettings.edit();

        mRateAppDialog = new AlertDialog.Builder(context)
                .setMessage(R.string.do_you_love_app)
                .setPositiveButton(R.string.I_love_it, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        editor.putBoolean("show_rate_request", false).commit();
                        rateApp(context);
                    }
                })
                .setNegativeButton(R.string.I_dont_love_it, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        editor.putBoolean("show_rate_request", false).commit();
                        emailMe(context);
                    }
                }) .create();

        mRateAppDialog.setCancelable(true);
        mRateAppDialog.show();
    }

    public static void emailMe(Context context){
        String emailAddresses[] = {"playerlingua@gmail.com"};
        Toast.makeText(context , R.string.send_email, Toast.LENGTH_LONG).show();

        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, emailAddresses);
        emailIntent.setType("plain/text");
        context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.send_email_in)));
    }
    public static void rateApp(Context context){
        String market = BuildConfig.FLAVOR_market;
        Toast.makeText(context , market.equals("bazaar") ? R.string.rate_app_request_bazaar : R.string.rate_app_request_myket,Toast.LENGTH_LONG).show();
        Intent intent = new Intent();
        if(market.equals("bazaar")) {
            intent.setAction(Intent.ACTION_EDIT);
            intent.setData(Uri.parse("bazaar://details?id=" + "ir.habibkazemi.linguaplayer" ));
            intent.setPackage("com.farsitel.bazaar");
            if(intent.resolveActivity(context.getPackageManager()) != null)
                context.startActivity(intent);
            else{
                Toast.makeText(context, R.string.bazaar_is_not_installed, Toast.LENGTH_SHORT);
            }
        }
        if(market.equals("myket")) {
            String url= "myket://comment?id=ir.habibkazemi.linguaplayer";
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            if(intent.resolveActivity(context.getPackageManager()) != null)
                context.startActivity(intent);
            else{
                Toast.makeText(context, R.string.myket_is_not_installed, Toast.LENGTH_SHORT);
            }
        }
    }
}
