package com.example.bismart.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DirectionsResponse {

    @SerializedName("status")
    public String status;

    @SerializedName("routes")
    public List<Route> routes;

    public static class Route {
        @SerializedName("legs")
        public List<Leg> legs;

        @SerializedName("overview_polyline")
        public OverviewPolyline overview_polyline;
    }

    public static class Leg {
        @SerializedName("duration")
        public Duration duration;

        @SerializedName("distance")
        public Distance distance;
    }

    public static class Duration {
        @SerializedName("text")
        public String text;

        @SerializedName("value")
        public int value;
    }

    public static class Distance {
        @SerializedName("text")
        public String text;

        @SerializedName("value")
        public int value;
    }

    public static class OverviewPolyline {
        @SerializedName("points")
        public String points;
    }
}