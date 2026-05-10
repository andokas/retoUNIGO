package com.example.bismart.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Path;

public interface EuskalmetApi {

    @Headers("Accept: application/json")
    @GET("euskalmet/observations/stations")
    Call<EuskalmetStationsResponse> getStations(
            @Header("Authorization") String token
    );

    @Headers("Accept: application/json")
    @GET("euskalmet/meteorology/stations/{station}/readings")
    Call<EuskalmetReadingResponse> getLastReading(
            @Header("Authorization") String token,
            @Path("station") String stationId
    );
}