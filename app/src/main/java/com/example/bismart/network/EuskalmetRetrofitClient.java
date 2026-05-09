package com.example.bismart.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EuskalmetRetrofitClient {
    private static Retrofit retrofit;
    private static final String BASE_URL = "https://api.euskadi.eus/euskalmet/";

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}