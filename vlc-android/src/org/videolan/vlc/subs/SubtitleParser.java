package org.videolan.vlc.subs;

/**
 * Created by habib on 3/27/17.
 */
import android.os.AsyncTask;
import android.support.annotation.NonNull;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class SubtitleParser {

    private SubtitleParser(){ }

    private static SubtitleParser mSubtitleParser = null;

    public static SubtitleParser getInstance(){
        if(mSubtitleParser == null)
            mSubtitleParser = new SubtitleParser();
        return mSubtitleParser;
    }

    private WeakReference<ISubtitleParserListener> listenerReference;

    public interface ISubtitleParserListener {
        void onSubtitleParseCompleted(boolean isSuccessful, TimedTextObject subtitleFile,String subtitlePath);
    }

    public void parseSubtitle(@NonNull String subtitleFilePath ,String subtitleLanguage, String manualEncoding) {
        if (listenerReference == null) throw new IllegalArgumentException("listener must not null. Call setSubtitleParserListener() to sets one");
        if (listenerReference.get() == null) return;
        ISubtitleParserListener listener = listenerReference.get();
        if(mSubtitleParseTask != null )
            mSubtitleParseTask.cancel(true);

        mSubtitleParseTask = new SubtitleParseTask(subtitleLanguage,manualEncoding, listener );
        mSubtitleParseTask.execute(subtitleFilePath);
    }

    private SubtitleParseTask mSubtitleParseTask = null;
    private class SubtitleParseTask extends AsyncTask<String, SubtitleParseTask.Wrapper, TimedTextObject> {
        public class Wrapper{
            public String subtitleFilePath;
            public  TimedTextObject text;
        }

        String subtitleLanguage;
        String subtitleManualEncoding;
        WeakReference<ISubtitleParserListener> listenerReference;

        public SubtitleParseTask(String language, String manualEncoding, ISubtitleParserListener listener ) {
            subtitleLanguage = language;
            subtitleManualEncoding = manualEncoding;
            listenerReference = new WeakReference<>(listener);
        }

        @Override
        protected TimedTextObject doInBackground(String... filePaths) {
            for (String filePath : filePaths) {
//                File file = new File(filePath);
                try {
                    TimedTextObject text = parseAsTimedTextObject(new File(filePath));
                    if(isCancelled())
                        break;
                    Wrapper w = new Wrapper();
                    w.subtitleFilePath = filePath;
                    w.text = text;
                    publishProgress(w);
                }
                catch (FileNotFoundException e) {
                    if (e.getMessage().contains("EBUSY")) {
                        try {
                            TimedTextObject text = parseAsTimedTextObject(new File(filePath));
                            if(isCancelled())
                                break;
                            Wrapper w = new Wrapper();
                            w.subtitleFilePath = filePath;
                            w.text = text;
                            publishProgress(w);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                            return null;
                        }
                    }
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Wrapper... values) {
            super.onProgressUpdate(values);
            for (Wrapper w : values ) {
                if (listenerReference.get() == null) break;
                listenerReference.get().onSubtitleParseCompleted(true, w.text,w.subtitleFilePath);
            }
        }

        private TimedTextObject parseAsTimedTextObject(File file) throws IOException {
            FileInputStream fileInputStream = new FileInputStream(file);
            FormatSRT formatSRT = new FormatSRT();
            TimedTextObject result = formatSRT.parseFile(
                    file.toString(),
                    SubUtils.inputstreamToCharsetString(
                            fileInputStream,
                            subtitleLanguage,subtitleManualEncoding).split("\n"));
            return result;
        }
    }

    public void setSubtitleParserListener(ISubtitleParserListener listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not null");
        listenerReference = new WeakReference<>(listener);
    }
}
