package com.example.bismart.models;

public class Usuario {
    public String uid;
    public String email;
    public String transportePreferido;
    public String idioma;

    public Usuario() {}

    public Usuario(String uid, String email, String transportePreferido) {
        this.uid = uid;
        this.email = email;
        this.transportePreferido = transportePreferido;
    }
}