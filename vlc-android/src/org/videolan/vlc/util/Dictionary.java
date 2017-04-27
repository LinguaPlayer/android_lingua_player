package org.videolan.vlc.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by habib on 4/23/17.
 */

public class Dictionary {

    private String DB_PATH = "/data/data/org.videolan.vlc.debug/databases/";

    private static Dictionary dictionaryInstance;
    private SQLiteDatabase db  = null;
    private TextToSpeech tts;

    private Dictionary(){

    }
    private Dictionary(Context context){
        String packageName = context.getPackageName();
        DB_PATH = "/data/data/"+ packageName + "/databases/";
    }
    public static Dictionary getInstance(Context context, String dbName) throws IOException {

        if (dictionaryInstance == null)
            dictionaryInstance = new Dictionary(context);

        boolean databaseExist = dictionaryInstance.doesDatabaseExist(context, dbName + ".dict");

        if(!databaseExist){
            dictionaryInstance.unpackZip(context,dbName);
        }
        dictionaryInstance.openDatabase(dbName);
        //TODO:get local from dictionary type
        dictionaryInstance.initTts(context,Locale.US);

        return dictionaryInstance;
    }


    public String getDbName(){
        if(db!= null) {
            String dbName = Uri.parse(db.getPath()).getLastPathSegment();
            return dbName;
        }
        return null;
    }

    private boolean unpackZip(Context context, String dbName)
    {
        InputStream is;
        ZipInputStream zis;
        String outFileName = DB_PATH + dbName + ".dict";
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

    private void copyDataBase(Context context, String dbName) throws IOException
    {
        InputStream mInput = context.getAssets().open(dbName);
        String outFileName = DB_PATH+dbName;

        OutputStream mOutput = new FileOutputStream(outFileName);
        byte[] mBuffer = new byte[1024];
        int mLength;
        while ((mLength = mInput.read(mBuffer))>0)
        {
            mOutput.write(mBuffer, 0, mLength);
        }
        mOutput.flush();
        mOutput.close();
        mInput.close();
    }

    private static boolean doesDatabaseExist(Context context, String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        return dbFile.exists();
    }

    private void openDatabase(String dbName){
        db = SQLiteDatabase.openDatabase(DB_PATH+dbName+".dict", null, SQLiteDatabase.OPEN_READONLY);
    }

    public String getTranslation(String word){
        //TODO:fix this later
        if(db == null)
            return "";
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

        return result;
    }

    public void initTts(Context context, final Locale locale){
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    int result = tts.setLanguage(locale);

                    if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("language not support", locale.toString());
                    }

                } else {
                    Log.d("TTS","Initialiazation Failed");
                }

            }
        });
    }

    public void speakOut(String toSpeak){
        tts.speak(toSpeak,TextToSpeech.QUEUE_FLUSH,null);
    }

}
