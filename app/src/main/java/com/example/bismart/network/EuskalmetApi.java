package com.example.bismart.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface EuskalmetApi {

    // Obtener lista de estaciones
    @GET("v1/observations/stations")
    Call<EuskalmetStationsResponse> getStations(
            @Header("Authorization") String token
    );

    // Obtener última lectura de una estación
    @GET("v1/observations/stations/{stationId}/readings/lastValue")
    Call<EuskalmetReadingResponse> getLastReading(
            @Header("Authorization") String token,
            @Path("stationId") String stationId
    );
}