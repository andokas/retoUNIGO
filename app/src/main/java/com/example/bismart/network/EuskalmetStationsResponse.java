package com.example.bismart.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class EuskalmetStationsResponse {

    @SerializedName("stations")
    public List<Station> stations;

    public static class Station {
        @SerializedName("id")
        public String id;

        @SerializedName("name")
        public String name;

        @SerializedName("location")
        public Location location;
    }

    public static class Location {
        @SerializedName("latitude")
        public double latitude;

        @SerializedName("longitude")
        public double longitude;
    }
}