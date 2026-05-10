package com.example.bismart.network;

import com.google.gson.annotations.SerializedName;

public class AireMedicionResponse {

    @SerializedName("Date")
    public String fecha;

    @SerializedName("HourGMT")
    public String hora;

    @SerializedName("ICAEstacion")
    public String icaEstacion; // "Muy buena / Oso ona", "Buena / Ona", etc.

    @SerializedName("TC")
    public String temperatura;

    @SerializedName("H")
    public String humedad;

    @SerializedName("Vvienms")
    public String viento;

    @SerializedName("Precipitacionlm2")
    public String precipitacion;
}