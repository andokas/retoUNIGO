package com.example.bismart.models;

public class CentroUniversitario {
    public String nombre;
    public String ubicacion;
    public String entidad; // EHU, Deusto, MU
    public double latitud;
    public double longitud;

    // Constructor vacío
    public CentroUniversitario() {}

    public CentroUniversitario(String nombre, String ubicacion, String entidad, double latitud, double longitud) {
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.entidad = entidad;
        this.latitud = latitud;
        this.longitud = longitud;
    }
}