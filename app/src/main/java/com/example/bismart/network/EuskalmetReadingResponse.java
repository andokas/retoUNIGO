package com.example.bismart.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class EuskalmetReadingResponse {

    @SerializedName("readings")
    public List<Reading> readings;

    public static class Reading {
        @SerializedName("parameterId")
        public String parameterId; // "TA" = temp, "HR" = humedad, "VV" = viento, "PR" = lluvia

        @SerializedName("value")
        public double value;

        @SerializedName("unit")
        public String unit;
    }
}