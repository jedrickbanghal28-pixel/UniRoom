package com.example.uniroom.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Client {

    private static Retrofit r;

    public static Api api() {

        if (r == null) {

            r = new Retrofit.Builder()
                    .baseUrl("http://192.168.254.117/php_api/")
                    .addConverterFactory(
                            GsonConverterFactory.create()
                    )
                    .build();
        }

        return r.create(Api.class);
    }
}