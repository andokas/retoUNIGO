package com.example.bismart.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.bismart.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import com.example.bismart.models.CentroUniversitario;
import com.example.bismart.repositories.CentroRepository;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

public class MapaFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    // Gestor moderno de permisos de Android
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    obtenerUbicacionActual();
                } else {
                    Toast.makeText(getContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                    // Centrar en Bilbao por defecto si no da permisos
                    centrarEnBilbao();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapa, container, false);

        // Inicializamos el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Inicializamos el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Comprobar si tenemos permisos
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionActual();
        } else {
            // Pedir permisos
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        cargarCentrosEnMapa();
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacionActual() {
        mMap.setMyLocationEnabled(true); // Muestra el puntito azul
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Obtenemos la última ubicación conocida
        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                LatLng miUbicacion = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(miUbicacion, 15f));
            } else {
                centrarEnBilbao();
            }
        });
    }

    private void centrarEnBilbao() {
        if (mMap != null) {
            LatLng bilbao = new LatLng(43.2630, -2.9350);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(bilbao, 13f));
        }
    }

    private void cargarCentrosEnMapa() {
        CentroRepository repo = new CentroRepository();

        // Llamamos al método que creó tu compañero
        repo.obtenerTodos().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot document : queryDocumentSnapshots) {
                // Traducimos el JSON de Firebase a nuestra clase Java
                CentroUniversitario centro = document.toObject(CentroUniversitario.class);

                if (centro != null && mMap != null) {
                    LatLng posicion = new LatLng(centro.latitud, centro.longitud);

                    // Le damos un color distinto a cada universidad para sumar puntos en Diseño
                    float colorPin = BitmapDescriptorFactory.HUE_RED; // EHU (Rojo por defecto)
                    if ("Deusto".equals(centro.entidad)) {
                        colorPin = BitmapDescriptorFactory.HUE_AZURE; // Azul
                    } else if ("MU".equals(centro.entidad)) {
                        colorPin = BitmapDescriptorFactory.HUE_GREEN; // Verde
                    }

                    // Creamos el "Pincito" y lo añadimos al mapa
                    mMap.addMarker(new MarkerOptions()
                            .position(posicion)
                            .title(centro.nombre)
                            .snippet(centro.entidad + " - " + centro.ubicacion) // El subtítulo al tocar el pin
                            .icon(BitmapDescriptorFactory.defaultMarker(colorPin)));
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Error cargando los centros del mapa", Toast.LENGTH_SHORT).show();
        });
    }
}