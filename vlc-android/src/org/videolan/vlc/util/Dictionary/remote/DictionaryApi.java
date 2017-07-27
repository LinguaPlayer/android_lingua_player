package org.videolan.vlc.util.Dictionary.remote;

/**
 * Created by habib on 7/18/17.
 */

public class DictionaryApi {
    public static final String BASE_URL = "https://glosbe.com/";

    public static GlosbeService getGlosbeService(){
        return RetrofitClient.getClient(BASE_URL).create(GlosbeService.class);

    }
}
