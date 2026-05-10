package com.example.bismart.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface AireApi {

    @GET("contenidos/ds_informes_estudios/calidad_aire_2026/es_def/adjuntos/estaciones.json")
    Call<java.util.List<AireEstacionResponse>> getEstaciones();

    @GET("contenidos/ds_informes_estudios/calidad_aire_2026/es_def/adjuntos/datos_indice/{nombre}.json")
    Call<java.util.List<AireMedicionResponse>> getMedicion(
            @Path("nombre") String nombre
    );
}