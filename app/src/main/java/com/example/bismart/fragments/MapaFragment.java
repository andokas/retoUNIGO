package com.example.bismart.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.bismart.R;
import com.example.bismart.models.CentroUniversitario;
import com.example.bismart.repositories.CentroRepository;
import com.example.bismart.network.DirectionsApi;
import com.example.bismart.network.DirectionsResponse;
import com.example.bismart.network.GoogleMapsRetrofitClient;
import com.example.bismart.repositories.UsuarioRepository;
import com.example.bismart.ui.IndicacionesAdapter;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapaFragment extends Fragment {

    private MapView mapView;
    private MyLocationNewOverlay locationOverlay; // El puntito azul
    private Polyline rutaDibujadaActual = null;
    private GeoPoint ubicacionUsuario = null;   //  GPS
    private GeoPoint destinoSeleccionado = null; // La universidad elegida
    private CardView cardIndicaciones;
    private TextView tvResumenRuta;
    private RecyclerView recyclerIndicaciones;
    private ImageView ivFlechaPanel;
    private boolean panelExpandido = false;
    private String idiomaActual = "es"; // español por defecto

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        return inflater.inflate(R.layout.fragment_mapa, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        cardIndicaciones = view.findViewById(R.id.cardIndicaciones);
        tvResumenRuta = view.findViewById(R.id.tvResumenRuta);
        recyclerIndicaciones = view.findViewById(R.id.recyclerIndicaciones);
        ivFlechaPanel = view.findViewById(R.id.ivFlechaPanel);

        // Expandir/colapsar al tocar la cabecera
        view.findViewById(R.id.headerIndicaciones).setOnClickListener(v -> {
            panelExpandido = !panelExpandido;
            recyclerIndicaciones.setVisibility(panelExpandido ? View.VISIBLE : View.GONE);
            ivFlechaPanel.setRotation(panelExpandido ? 180f : 0f);
        });

        // 1. Activar el puntito azul (Ubicación real)
        activarUbicacionReal();

        // 2. Cargar argumentos y destino
        Bundle args = getArguments();
        String transportePref = null;
        String nombre = null;

        if (args != null && args.containsKey("lat")) {
            double lat = args.getDouble("lat");
            double lng = args.getDouble("lng");
            nombre = args.getString("nombre");
            destinoSeleccionado = new GeoPoint(lat, lng);

            // Centramos el mapa en la facultad
            mapView.getController().setZoom(16.0);
            mapView.getController().setCenter(destinoSeleccionado);
        } else {
            // Abrir mapa general en Bilbao
            GeoPoint bilbao = new GeoPoint(43.2630, -2.9350);
            mapView.getController().setZoom(13.0);
            mapView.getController().setCenter(bilbao);
        }

        // 3. Cargar marcadores
        cargarMarcadores();

        // 4. Chips de transporte y selección automática
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupTransporte);
        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;

                int idSeleccionado = checkedIds.get(0);
                String medio = "";

                if (idSeleccionado == R.id.chipPie) medio = "pie";
                else if (idSeleccionado == R.id.chipBici) medio = "bici";
                else if (idSeleccionado == R.id.chipBus) medio = "bus";
                else if (idSeleccionado == R.id.chipMetro) medio = "metro";
                else if (idSeleccionado == R.id.chipTren) medio = "tren";
                else if (idSeleccionado == R.id.chipTranvia) medio = "tranvia";

                calcularRutaSegura(medio); // ¡Ahora calcula solo si hay ubicación GPS!
            });
        }

        // Marcar automáticamente el chip favorito si viene en el bundle:
        if (args != null && args.containsKey("transportePreferido")) {
            transportePref = args.getString("transportePreferido");
            int chipId = -1;
            if (chipGroup != null && transportePref != null && !transportePref.isEmpty()) {
                switch (transportePref) {
                    case "pie":      chipId = R.id.chipPie; break;
                    case "bici":     chipId = R.id.chipBici; break;
                    case "bus":      chipId = R.id.chipBus; break;
                    case "metro":    chipId = R.id.chipMetro; break;
                    case "tren":     chipId = R.id.chipTren; break;
                    case "tranvia":  chipId = R.id.chipTranvia; break;
                }
                if (chipId != -1) {
                    chipGroup.check(chipId);
                }
            }

            // Ahora, en vez de calcular directamente, esperamos a tener posición GPS
            if (transportePref != null && !transportePref.isEmpty()) {
                calcularRutaSegura(transportePref);
            }
        } else if (nombre != null) {
            Toast.makeText(getContext(), "Destino: " + nombre + "\nElige transporte arriba", Toast.LENGTH_LONG).show();
        }

        // 5. CARGAR IDIOMA del usuario
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        new UsuarioRepository().obtenerUsuario(uid)
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("idioma") != null) {
                        idiomaActual = doc.getString("idioma");
                    }
                });
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

    /**
     * Llama a calcularRuta solo cuando haya una posición GPS REAL.
     * Si ya la tiene, calcula ya.
     * Si no la tiene, espera (pero solo calcula una vez cuando el GPS la obtenga).
     */
    private void calcularRutaSegura(String medioTransporte) {
        if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
            calcularRuta(medioTransporte);
        } else if (locationOverlay != null) {
            // Espera UNA vez a la posición real, luego calcula.
            locationOverlay.runOnFirstFix(() ->
                    requireActivity().runOnUiThread(() -> calcularRuta(medioTransporte))
            );
        } else {
            Toast.makeText(getContext(), "GPS no disponible en este momento", Toast.LENGTH_SHORT).show();
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
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 40, 40, false);
                        Drawable scaledIcon = new BitmapDrawable(getResources(), scaledBitmap).mutate();
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

    /**
     * Esta función SOLO se llama desde calcularRutaSegura, así que SIEMPRE hay ubicación real lista.
     */
    private void calcularRuta(String medioTransporte) {
        if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
            ubicacionUsuario = locationOverlay.getMyLocation();
        } else {
            // Prácticamente nunca, salvo error, pero mantenemos el fallback:
            Log.e("RUTA_DEBUG", "GPS no disponible. Usando Casco Viejo por defecto.");
            ubicacionUsuario = new GeoPoint(43.2598, -2.9244);
        }

        if (destinoSeleccionado == null) {
            Toast.makeText(getContext(), "Error: Vuelve a la lista y selecciona una universidad", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Preparamos las coordenadas
        String origen = ubicacionUsuario.getLatitude() + "," + ubicacionUsuario.getLongitude();
        String destino = destinoSeleccionado.getLatitude() + "," + destinoSeleccionado.getLongitude();

        Log.d("RUTA_DEBUG", "Intentando calcular ruta. Origen: " + origen + " | Destino: " + destino);

        String modo = "transit";
        String modoTransito = "";

        switch (medioTransporte) {
            case "pie": modo = "walking"; break;
            case "bici": modo = "bicycling"; break;
            case "bus": modo = "transit"; modoTransito = "bus"; break;
            case "metro": modo = "transit"; modoTransito = "subway"; break;
            case "tren": modo = "transit"; modoTransito = "train"; break;
            case "tranvia": modo = "transit"; modoTransito = "tram"; break;
        }

        // Clave API de Google Cloud
        String apiKey = "AIzaSyCgQ4kwjnt9cxqZyIOMx_vwMuK3vRYq9_s";

        // 3. Hacemos la llamada con Retrofit
        DirectionsApi api = GoogleMapsRetrofitClient.getClient().create(DirectionsApi.class);
        Call<DirectionsResponse> call = api.getDirections(origen, destino, modo, modoTransito, idiomaActual, apiKey);

        call.enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().routes != null && !response.body().routes.isEmpty()) {

                    DirectionsResponse.Leg leg = response.body().routes.get(0).legs.get(0);
                    String polylineCodificada = response.body().routes.get(0).overview_polyline.points;

                    // Mostrar resumen en la cabecera
                    tvResumenRuta.setText("🕐 " + leg.duration.text + "  📍 " + leg.distance.text);
                    cardIndicaciones.setVisibility(View.VISIBLE);

                    // Cargar pasos en el RecyclerView
                    recyclerIndicaciones.setLayoutManager(
                            new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
                    recyclerIndicaciones.setAdapter(new IndicacionesAdapter(leg.steps));

                    // Dibujar ruta
                    dibujarRutaEnMapa(polylineCodificada);

                } else {
                    Toast.makeText(getContext(), "No se encontró ruta para este transporte",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Log.e("RUTA_DEBUG", "Fallo de conexión crítico: " + t.getMessage());
            }
        });
    }

    private void dibujarRutaEnMapa(String polylineCodificada) {
        List<GeoPoint> puntosDeRuta = decodificarPolyline(polylineCodificada);

        if (rutaDibujadaActual != null) {
            mapView.getOverlays().remove(rutaDibujadaActual);
        }

        rutaDibujadaActual = new Polyline();
        rutaDibujadaActual.setPoints(puntosDeRuta);
        rutaDibujadaActual.setColor(Color.parseColor("#1A73E8"));
        rutaDibujadaActual.setWidth(12.0f);

        mapView.getOverlays().add(rutaDibujadaActual);
        mapView.invalidate();
    }

    private List<GeoPoint> decodificarPolyline(String encoded) {
        List<GeoPoint> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            GeoPoint p = new GeoPoint((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }
}