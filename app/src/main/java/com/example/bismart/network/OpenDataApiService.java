package com.example.bismart.network;

import java.util.List;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenDataApiService {
    // Ejemplo: Obtener predicción meteorológica de Bilbao
    @GET("weather/forecast")
    Observable<Object> getClimaBilbao(@Query("municipio") String municipio);
}