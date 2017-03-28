//package org.videolan.vlc.subs;
//
//import android.support.annotation.NonNull;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.lang.ref.WeakReference;
//
//import butter.droid.base.manager.vlc.PlayerManager;
//import butter.droid.base.providers.media.models.Media;
//import butter.droid.base.providers.subs.SubsProvider;
//import butter.droid.base.torrent.StreamInfo;
//import okhttp3.Call;
//import okhttp3.Callback;
//import okhttp3.Response;
//
//public class SubtitleDownloader {
//
//    private final SubsProvider subsProvider;
//    private final Media media;
//    private final PlayerManager playerManager;
//
//    private String subtitleLanguage;
//    private WeakReference<ISubtitleDownloaderListener> listenerReference;
//
//    public SubtitleDownloader(@NonNull SubsProvider subsProvider, @NonNull StreamInfo streamInfo,
//            PlayerManager playerManager, @NonNull String language) {
//
//        if (language.equals(SubsProvider.SUBTITLE_LANGUAGE_NONE)) throw new IllegalArgumentException("language must be specified");
//
//        this.subsProvider = subsProvider;
//        subtitleLanguage = language;
//        this.playerManager = playerManager;
//
//        media = streamInfo.getMedia();
//        if (media == null) throw new IllegalArgumentException("media from StreamInfo must not null");
//    }
//
//    public void downloadSubtitle() {
//        if (listenerReference == null) throw new IllegalArgumentException("listener must not null. Call setSubtitleDownloaderListener() to sets one");
//        subsProvider.download(media, subtitleLanguage, new Callback() {
//            @Override public void onFailure(Call call, IOException e) {
//                onSubtitleDownloadFailed();
//            }
//
//            @Override public void onResponse(Call call, Response response) throws IOException {
//                onSubtitleDownloadSuccess();
//            }
//        });
//    }
//
//
//    public void setSubtitleDownloaderListener(ISubtitleDownloaderListener listener) {
//        if (listener == null) throw new IllegalArgumentException("listener must not null");
//        listenerReference = new WeakReference<>(listener);
//    }
//
//    /**
//     * Invoked when subtitle download finished successfully.
//     */
//    private void onSubtitleDownloadSuccess() {
//        if (listenerReference.get() == null) return;
//
//        ISubtitleDownloaderListener listener = listenerReference.get();
//
//        try {
//            File subtitleFile = playerManager.getDownloadedSubtitleFile(media, subtitleLanguage);
//            SubtitleParseTask task = new SubtitleParseTask(subtitleLanguage, listener);
//            task.execute(subtitleFile);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            listener.onSubtitleDownloadCompleted(false, null);
//        }
//    }
//
//    /**
//     * Invoked when subtitle download failed.
//     */
//    private void onSubtitleDownloadFailed() {
//        subtitleLanguage = SubsProvider.SUBTITLE_LANGUAGE_NONE;
//        if (listenerReference.get() == null) return;
//        ISubtitleDownloaderListener listener = listenerReference.get();
//        listener.onSubtitleDownloadCompleted(false, null);
//    }
//
//
//    public interface ISubtitleDownloaderListener {
//        void onSubtitleDownloadCompleted(boolean isSuccessful, TimedTextObject subtitleFile);
//    }
//}
