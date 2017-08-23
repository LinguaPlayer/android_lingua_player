package org.videolan.vlc.util.Dictionary;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import org.videolan.vlc.util.Dictionary.remote.DictionaryApi;
import org.videolan.vlc.util.Dictionary.remote.GlosbeService;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by habib on 4/23/17.
 */

public class Dictionary {

    private static Dictionary dictionaryInstance;
    private SQLiteDatabase db  = null;
    private TextToSpeech tts;

    private static String lastDbName = null;
    private static GlosbeService mGlosbeService;
    public GlosbeService getGlosbeService(){
        return mGlosbeService;
    }

    private Dictionary(){

    }
    private static String getDatabaseDirectory(Context context){
        String packageName = context.getPackageName();
        return "/data/data/"+ packageName + "/databases/dictionaries";
    }
    private Dictionary(Context context, String dbName){
        if(db != null)
            db.close();
        db = openDatabase(dbName, context);
    }
    public static Dictionary getInstance(Context context, String fromLanguage, String toLanguage) throws IOException {

        String dbName = "";
        if(fromLanguage.equals("en") && toLanguage.equals("en"))
            dbName = "gcide";
        else if (fromLanguage.equals("en") && toLanguage.equals("fa"))
            dbName = "ENG_PER";

        if (dictionaryInstance == null || !dbName.equals(getDbName()))
            dictionaryInstance = new Dictionary(context, dbName);
        mGlosbeService = DictionaryApi.getGlosbeService(context);
        if(dictionaryInstance != null)
            dictionaryInstance.initTts(context, new Locale(fromLanguage));

        return dictionaryInstance;
    }


    private static String getDbName(){
        return lastDbName;
    }

    public static boolean unpackZip(Context context, String dbName)
    {
        InputStream is;
        ZipInputStream zis;
        String outFileName = getDatabaseDirectory(context) + dbName + ".dict";
        try
        {
            is = context.getAssets().open(dbName + ".zip");
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;

            while((ze = zis.getNextEntry()) != null)
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int count;

                String filename = ze.getName();
                context.openOrCreateDatabase(dbName+".dict",Context.MODE_PRIVATE,null);
                FileOutputStream fout = new FileOutputStream(outFileName);

                // reading and writing
                while((count = zis.read(buffer)) != -1)
                {
                    baos.write(buffer, 0, count);
                    byte[] bytes = baos.toByteArray();
                    fout.write(bytes);
                    baos.reset();
                }

                fout.close();
                zis.closeEntry();
            }

            zis.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private SQLiteDatabase openDatabase(String dbName, Context context){
        try {
            return SQLiteDatabase.openDatabase(getDatabaseDirectory(context) + dbName + ".dict", null, SQLiteDatabase.OPEN_READONLY);
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getTranslation(String word){
        //TODO:fix this later
        if(db == null)
            return "Offline dictionary not exist for this language pair";
        String result = word;
        Cursor cursor = db.rawQuery("select definition from words where word=?", new String[] {word.toLowerCase()});
        if(cursor!=null && cursor.getCount()!=0){
            result = "";
            if(cursor.moveToFirst()){
                do{
                    result += cursor.getString(0);
                    result += "\n";
                }while(cursor.moveToNext());
            }
        }
        cursor.close();
        return result;
    }

    public boolean ttsForLanguageAvailable = false;
    public void initTts(Context context, final Locale locale){
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    int result = tts.setLanguage(locale);

                    if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                        ttsForLanguageAvailable = false;
                        Log.e("language not support", locale.toString());
                    }
                    else{
                        ttsForLanguageAvailable = true;
                    }

                } else {
                    Log.d("TTS","Initialiazation Failed");
                }

            }
        });
    }

    public void speakOut(String toSpeak){
        if(tts != null)
            tts.speak(toSpeak,TextToSpeech.QUEUE_FLUSH,null);
    }

}
