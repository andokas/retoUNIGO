package com.example.bismart.network;

import java.util.List;

public class DirectionsResponse {
    public List<Route> routes;

    public static class Route {
        public List<Leg> legs;
        public OverviewPolyline overview_polyline;
    }

    public static class Leg {
        public Duration duration;
    }

    public static class Duration {
        public String text;
        public int value; // segundos
    }

    public static class OverviewPolyline {
        public String points;
    }
}