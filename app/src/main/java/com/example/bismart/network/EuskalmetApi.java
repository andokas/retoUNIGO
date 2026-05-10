package com.example.bismart.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface EuskalmetApi {

    // Obtener datos de una estación
    @GET("v1/stations/{stationId}")
    Call<EuskalmetStationsResponse> getStation(
            @Header("Authorization") String token,
            @Path("stationId") String stationId
    );

    // Obtener última lectura de una estación
    @GET("v1/stations/{stationId}/readings/lastValue")
    Call<EuskalmetReadingResponse> getLastReading(
            @Header("Authorization") String token,
            @Path("stationId") String stationId
    );

    // Obtener todas las estaciones
    @GET("v1/stations")
    Call<EuskalmetStationsResponse> getStations(
            @Header("Authorization") String token
    );
}