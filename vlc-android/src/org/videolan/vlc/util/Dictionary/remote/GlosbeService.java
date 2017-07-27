package org.videolan.vlc.util.Dictionary.remote;

import org.videolan.vlc.util.Dictionary.model.Glosbe;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by habib on 7/18/17.
 */

public interface GlosbeService {
    @GET("/gapi/translate?&format=json")
    Call<Glosbe> getMeaning(@Query("from") String from, @Query("dest") String dest, @Query("phrase") String phrase);
}
