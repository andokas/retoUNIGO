package com.example.bismart.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.bismart.R;
import com.example.bismart.fragments.MapaFragment;
import com.example.bismart.fragments.CentrosFragment;
import com.example.bismart.fragments.PerfilFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
            } else if (id == R.id.nav_perfil) {
                fragment = new PerfilFragment();
            } else {
                return false;
            }
            cargarFragment(fragment);
            return true;
        });
    }

    private void cargarFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}