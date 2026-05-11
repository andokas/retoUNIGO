package com.example.bismart.network;

import com.google.gson.annotations.SerializedName;

public class AireEstacionResponse {

    @SerializedName("Name")
    public String nombre;

    @SerializedName("Province")
    public String provincia;

    @SerializedName("Town")
    public String municipio;

    @SerializedName("Latitude")
    public String latitudStr;

    @SerializedName("Longitude")
    public String longitudStr;

    // Convierte la cadena con coma a double
    public double getLatitud() {
        try { return Double.parseDouble(latitudStr.replace(",", ".")); }
        catch (Exception e) { return 0; }
    }

    public double getLongitud() {
        try { return Double.parseDouble(longitudStr.replace(",", ".")); }
        catch (Exception e) { return 0; }
    }
}