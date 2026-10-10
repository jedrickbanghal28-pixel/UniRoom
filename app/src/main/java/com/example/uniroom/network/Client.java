package com.example.uniroom.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Client {

    /*
     * Address of the UNIROOM API on Vercel (same API the admin website uses).
     * It must end with "/".
     */
    public static final String BASE_URL = "https://uniroom-api.vercel.app/api/";

    private static Retrofit r;

    public static Api api() {

        if (r == null) {

            // The first request after the API has been idle can take a few
            // seconds while Vercel starts it, so allow more time than the default.
            OkHttpClient http = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            r = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return r.create(Api.class);
    }
}
