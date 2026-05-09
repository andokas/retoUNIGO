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

        @SerializedName("steps")
        public List<Step> steps; // ← NUEVO
    }

    public static class Step {
        @SerializedName("html_instructions")
        public String htmlInstructions; // ← Texto con la indicación

        @SerializedName("distance")
        public Distance distance;

        @SerializedName("duration")
        public Duration duration;

        @SerializedName("travel_mode")
        public String travelMode;
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