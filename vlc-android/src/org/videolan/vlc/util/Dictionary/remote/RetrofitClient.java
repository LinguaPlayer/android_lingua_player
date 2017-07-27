package org.videolan.vlc.util.Dictionary.remote;

/**
 * Created by habib on 7/18/17.
 */

import android.content.Context;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context, String baseUrl){
        if(retrofit == null){
            retrofit = new Retrofit.Builder()
                    .client(new OkHttpClient.Builder()
                            .addInterceptor(new ConnectivityInterceptor(context))
                            .build())
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return  retrofit;

    }
}
