package com.example.bismart.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREF_IDIOMA = "idioma_app";

    public static void aplicarIdioma(Context context, String idioma) {
        // Guardar en SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("bismart_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_IDIOMA, idioma).apply();

        // Aplicar el idioma
        setLocale(context, idioma);
    }

    public static void aplicarIdiomaGuardado(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("bismart_prefs", Context.MODE_PRIVATE);
        String idioma = prefs.getString(PREF_IDIOMA, "es"); // español por defecto
        setLocale(context, idioma);
    }

    public static String getIdiomaGuardado(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("bismart_prefs", Context.MODE_PRIVATE);
        return prefs.getString(PREF_IDIOMA, "es");
    }

    private static void setLocale(Context context, String idioma) {
        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        Context newContext = context.createConfigurationContext(configuration);
        context.getResources().updateConfiguration(
                configuration,
                newContext.getResources().getDisplayMetrics()
        );
    }
}