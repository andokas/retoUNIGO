package com.example.bismart.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.bismart.R;
import com.example.bismart.models.CentroUniversitario;
import com.example.bismart.repositories.CentroRepository;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapaFragment extends Fragment {

    private MapView mapView;
    private MyLocationNewOverlay locationOverlay; // El puntito azul

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        return inflater.inflate(R.layout.fragment_mapa, container, false);
        //hola
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // 1. Configurar Bilbao como centro inicial
        GeoPoint bilbao = new GeoPoint(43.2630, -2.9350);
        mapView.getController().setZoom(13.0);
        mapView.getController().setCenter(bilbao);

        // 2. Activar el puntito azul (Ubicación real)
        activarUbicacionReal();

        // 3. Cargar marcadores con colores
        cargarMarcadores();
    }

    private void activarUbicacionReal() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            this.locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
            this.locationOverlay.enableMyLocation();
            this.locationOverlay.enableFollowLocation(); // El mapa sigue al usuario al inicio
            mapView.getOverlays().add(this.locationOverlay);
        } else {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void cargarMarcadores() {
        new CentroRepository().obtenerTodos().addOnSuccessListener(querySnapshot -> {
            for (var doc : querySnapshot.getDocuments()) {
                CentroUniversitario centro = doc.toObject(CentroUniversitario.class);
                if (centro != null) {
                    Marker marker = new Marker(mapView);
                    marker.setPosition(new GeoPoint(centro.latitud, centro.longitud));
                    marker.setTitle(centro.nombre);
                    marker.setSnippet(centro.entidad);
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                    Drawable icon = getResources().getDrawable(R.drawable.icono_marcador_universidad, null);

                    if (icon != null) {
                        // Extraer el Bitmap de la imagen
                        Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();

                        // REDIMENSIONAR: Aquí ajustas el tamaño. 80x80 suele quedar perfecto.
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 40, 40, false);

                        // Volver a convertirlo en Drawable para el marcador
                        Drawable scaledIcon = new BitmapDrawable(getResources(), scaledBitmap).mutate();


                        // Ponerle el icono al marcador
                        marker.setIcon(scaledIcon);
                    }

                    mapView.getOverlays().add(marker);
                }
            }
            mapView.invalidate(); // Refresca el mapa para mostrar los cambios
        });
    }

    @Override public void onResume() { super.onResume(); mapView.onResume(); }
    @Override public void onPause() { super.onPause(); mapView.onPause(); }
}