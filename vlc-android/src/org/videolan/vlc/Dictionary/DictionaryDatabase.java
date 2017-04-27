//package org.videolan.vlc.Dictionary;
//
//
//public class DictionaryDatabase {
//    :
//
//    @Override
//    public boolean equals(Object obj) {
//        return super.equals(obj);
//    }
//}


//import android.content.Context;
//import android.database.SQLException;
//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteException;
//import android.database.sqlite.SQLiteOpenHelper;
//import android.net.Uri;
//
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;

/**
 * Created by habib on 4/19/17.
 */

//public class DictionaryDatabase extends SQLiteOpenHelper{
//
//    private static String DB_PATH = "/data/data/myPackage/databases";
////    private static String DB_NAME = "myDBName";
//    private SQLiteDatabase myDatabase;
//    private static DictionaryDatabase sInstance;
//
//
//    private DictionaryDatabase(Context context,String dbName){
//        super(context, dbName, null, 0 );
//    }
//
//    public static synchronized DictionaryDatabase getInstance(Context context,String dbName){
//        if(sInstance == null)
//            sInstance = new DictionaryDatabase(context.getApplicationContext(),dbName);
//        return null;
//    }
//
//
//    public void createDataBase(String dbPath) throws IOException{
//        boolean dbExist = checkDatabse(dbPath);
//
//        if(dbExist){
//
//        }else{
//            this.getReadableDatabase();
//            try{
//                copyDataBase();
//
//            }catch (IOException e){
//                throw new Error("Error copying database");
//            }
//        }
//    }
//
//    private boolean checkDatabse(String dpPath){
//        SQLiteDatabase checkDB = null;
//        String dbName= Uri.parse(dpPath).getLastPathSegment();
//
//        try{
//            String pathInDatabasesDirectory = DB_PATH + dbName;
//            checkDB = SQLiteDatabase.openDatabase(pathInDatabasesDirectory, null, SQLiteDatabase.OPEN_READONLY);
//
//        }catch (SQLiteException e){
//            //database doesn't exist yet.
//        }
//
//        if(checkDB != null )
//            checkDB.close();
//
//        return checkDB != null ? true : false;
//    }
//
//    private void copyDataBase() throws IOException{
//        InputStream myInput = myContext.getAssets().open(DB_NAME);
//
//        String outFileName = DB_PATH + DB_NAME;
//
//        OutputStream myOutput = new FileOutputStream(outFileName);
//
//        byte[] buffer = new byte[1024];
//        int length;
//        while((length = myInput.read(buffer))>0){
//            myOutput.write(buffer,0,length);
//        }
//
//        myOutput.flush();
//        myOutput.close();
//        myInput.close();
//    }
//    public void openDataBase() throws SQLException{
//        String myPath= DB_PATH + DB_NAME;
//        myDatabase = SQLiteDatabase.openDatabase(myPath,null,SQLiteDatabase.OPEN_READONLY);
//    }
//
//    @Override
//    public synchronized void close(){
//        if(myDatabase != null)
//            myDatabase.close();
//        super.close();
//    }
//
//    @Override
//    public void onCreate(SQLiteDatabase db){
//
//    }
//
//    @Override
//    public void onUpgrade(SQLiteDatabase db ,int oldVersion, int newVersion){
//
//    }
//}
