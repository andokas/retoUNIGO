package com.example.bismart.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OpenWeatherResponse {

    @SerializedName("name")
    public String cityName;

    @SerializedName("main")
    public Main main;

    @SerializedName("wind")
    public Wind wind;

    @SerializedName("rain")
    public Rain rain;

    @SerializedName("weather")
    public List<Weather> weather;

    public static class Main {
        @SerializedName("temp")
        public double temp;

        @SerializedName("humidity")
        public double humidity;
    }

    public static class Wind {
        @SerializedName("speed")
        public double speed; // m/s
    }

    public static class Rain {
        @SerializedName("1h")
        public double oneHour; // mm última hora
    }

    public static class Weather {
        @SerializedName("description")
        public String description;

        @SerializedName("icon")
        public String icon;
    }
}