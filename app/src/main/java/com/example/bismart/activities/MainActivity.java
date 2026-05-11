package com.example.bismart.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.bismart.R;
import com.example.bismart.fragments.ClimaFragment;
import com.example.bismart.fragments.MapaFragment;
import com.example.bismart.fragments.CentrosFragment;
import com.example.bismart.fragments.PerfilFragment;
import com.example.bismart.utils.LocaleHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.content.IntentSender;
import android.util.Log;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {

    private android.content.BroadcastReceiver locationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.aplicarIdiomaGuardado(this);
        super.onCreate(savedInstanceState);

        // Comprobamos si hay sesión en Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // No hay sesión: Al Login y cerramos esta pantalla
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        // ---------------------------------------------------------

        setContentView(R.layout.activity_main);

        // Poblar centros solo la primera vez
        android.content.SharedPreferences prefs = getSharedPreferences("bismart_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("centros_poblados", false)) {
            new com.example.bismart.repositories.CentroRepository().poblarCentros();
            prefs.edit().putBoolean("centros_poblados", true).apply();
        }

        // Fragment inicial
        cargarFragment(new MapaFragment());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();
            if (id == R.id.nav_mapa) {
                fragment = new MapaFragment();
            } else if (id == R.id.nav_centros) {
                fragment = new CentrosFragment();
            } else if (id == R.id.nav_clima) {
                fragment = new ClimaFragment();
            } else if (id == R.id.nav_perfil) {
                fragment = new PerfilFragment();
            } else {
                return false;
            }
            cargarFragment(fragment);
            return true;
        });

        pedirActivarUbicacion();
        registrarReceptorUbicacion();
    }

    private void cargarFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void pedirActivarUbicacion() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true)
                .build();

        LocationServices.getSettingsClient(this)
                .checkLocationSettings(settingsRequest)
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ((ResolvableApiException) e).startResolutionForResult(this, 1001);
                        } catch (IntentSender.SendIntentException ex) {
                            Log.e("UBICACION", "Error: " + ex.getMessage());
                        }
                    }
                });
    }

    private void registrarReceptorUbicacion() {
        locationReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                if (intent.getAction().equals(android.location.LocationManager.PROVIDERS_CHANGED_ACTION)) {
                    android.location.LocationManager lm =
                            (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
                    boolean gpsActivo = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
                    if (!gpsActivo) {
                        // Ubicación desactivada → pedir que la encienda
                        pedirActivarUbicacion();
                    }
                }
            }
        };

        android.content.IntentFilter filter = new android.content.IntentFilter(
                android.location.LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(locationReceiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode != RESULT_OK) {
            // Si rechaza, volvemos a pedir al cabo de 3 segundos
            new android.os.Handler().postDelayed(this::pedirActivarUbicacion, 3000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationReceiver != null) unregisterReceiver(locationReceiver);
    }
}