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
    private MyLocationNewOverlay locationOverlay; // Puntito azul
    private List<Polyline> polylines = new ArrayList<>();
    private GeoPoint ubicacionUsuario = null;
    private GeoPoint destinoSeleccionado = null;
    private CardView cardIndicaciones;
    private TextView tvResumenRuta;
    private RecyclerView recyclerIndicaciones;
    private ImageView ivFlechaPanel;
    private boolean panelExpandido = false;
    private String idiomaActual = "es";
    private boolean idiomaCargado = false;
    private Bundle argsGuardados = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        return inflater.inflate(R.layout.fragment_mapa, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Guardamos argumento para usar tras cargar idioma
        argsGuardados = getArguments();

        // Inicializa views básicos (no UI dependiente de idioma ni rutas todavía)
        mapView = view.findViewById(R.id.mapView);
        cardIndicaciones = view.findViewById(R.id.cardIndicaciones);
        tvResumenRuta = view.findViewById(R.id.tvResumenRuta);
        recyclerIndicaciones = view.findViewById(R.id.recyclerIndicaciones);
        ivFlechaPanel = view.findViewById(R.id.ivFlechaPanel);

        // Panel expandible
        view.findViewById(R.id.headerIndicaciones).setOnClickListener(v -> {
            panelExpandido = !panelExpandido;
            recyclerIndicaciones.setVisibility(panelExpandido ? View.VISIBLE : View.GONE);
            ivFlechaPanel.setRotation(panelExpandido ? 180f : 0f);
        });

        // Activa GPS (esto no depende de idioma)
        activarUbicacionReal();

        // Carga idioma y arranca inicialización UI "real" al terminar
        cargarIdiomaUsuarioYContinuar(view, argsGuardados);
    }

    private void cargarIdiomaUsuarioYContinuar(View view, Bundle args) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        new UsuarioRepository().obtenerUsuario(uid)
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("idioma") != null) {
                        idiomaActual = doc.getString("idioma");
                    }
                    idiomaCargado = true;
                    inicializarVistaMapa(view, args);
                })
                .addOnFailureListener(e -> {
                    idiomaCargado = true;
                    inicializarVistaMapa(view, args);
                });
    }

    /**
     * Solo se llama una vez se ha leído el idioma del usuario (o se queda en "es" por defecto).
     * Aquí sí puedes montar chips, panel, cálculo de ruta, etc.
     */
    private void inicializarVistaMapa(View view, Bundle args) {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        polylines.clear();

        String transportePref = null;
        String nombre = null;

        if (args != null && args.containsKey("lat")) {
            double lat = args.getDouble("lat");
            double lng = args.getDouble("lng");
            nombre = args.getString("nombre");
            destinoSeleccionado = new GeoPoint(lat, lng);

            mapView.getController().setZoom(16.0);
            mapView.getController().setCenter(destinoSeleccionado);
        } else {
            GeoPoint bilbao = new GeoPoint(43.2630, -2.9350);
            mapView.getController().setZoom(13.0);
            mapView.getController().setCenter(bilbao);
        }

        cargarMarcadores(); // Esto da igual idioma

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

                calcularRutaSegura(medio);
            });
        }

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
                if (chipId != -1) chipGroup.check(chipId);
            }
            if (transportePref != null && !transportePref.isEmpty()) {
                calcularRutaSegura(transportePref);
            }
        } else if (nombre != null) {
            Toast.makeText(getContext(), "Destino: " + nombre + "\nElige transporte arriba", Toast.LENGTH_LONG).show();
        }
    }

    private void activarUbicacionReal() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            this.locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
            this.locationOverlay.enableMyLocation();
            this.locationOverlay.enableFollowLocation();
            mapView.getOverlays().add(this.locationOverlay);
        } else {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void calcularRutaSegura(String medioTransporte) {
        if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
            calcularRuta(medioTransporte);
        } else if (locationOverlay != null) {
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
                        Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 40, 40, false);
                        Drawable scaledIcon = new BitmapDrawable(getResources(), scaledBitmap).mutate();
                        marker.setIcon(scaledIcon);
                    }
                    mapView.getOverlays().add(marker);
                }
            }
            mapView.invalidate();
        });
    }

    @Override public void onResume() { super.onResume(); mapView.onResume(); }
    @Override public void onPause() { super.onPause(); mapView.onPause(); }

    private void calcularRuta(String medioTransporte) {
        if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
            ubicacionUsuario = locationOverlay.getMyLocation();
        } else {
            Log.e("RUTA_DEBUG", "GPS no disponible. Usando Casco Viejo por defecto.");
            ubicacionUsuario = new GeoPoint(43.2598, -2.9244);
        }

        if (destinoSeleccionado == null) {
            Toast.makeText(getContext(), "Error: Vuelve a la lista y selecciona una universidad", Toast.LENGTH_SHORT).show();
            return;
        }

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

        String apiKey = "AIzaSyCgQ4kwjnt9cxqZyIOMx_vwMuK3vRYq9_s";

        DirectionsApi api = GoogleMapsRetrofitClient.getClient().create(DirectionsApi.class);
        Call<DirectionsResponse> call = api.getDirections(origen, destino, modo, modoTransito, idiomaActual, apiKey);

        call.enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().routes != null && !response.body().routes.isEmpty()) {

                    DirectionsResponse.Leg leg = response.body().routes.get(0).legs.get(0);

                    tvResumenRuta.setText("🕐 " + leg.duration.text + "  📍 " + leg.distance.text);
                    cardIndicaciones.setVisibility(View.VISIBLE);

                    recyclerIndicaciones.setLayoutManager(
                            new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
                    recyclerIndicaciones.setAdapter(new IndicacionesAdapter(leg.steps));

                    dibujarRutaMultiColor(leg.steps);

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

    /** Dibuja cada step con un color según travelMode: a pie, bici o transporte público */
    private void dibujarRutaMultiColor(List<DirectionsResponse.Step> steps) {
        for (Polyline p : polylines) {
            mapView.getOverlays().remove(p);
        }
        polylines.clear();

        for (DirectionsResponse.Step step : steps) {
            if (step.polyline == null || step.polyline.points == null) continue;
            List<GeoPoint> puntos = decodificarPolyline(step.polyline.points);

            Polyline line = new Polyline();
            line.setPoints(puntos);

            int color = Color.BLUE;
            if ("WALKING".equalsIgnoreCase(step.travelMode)) {
                color = Color.parseColor("#33B5E5"); // azul claro
            } else if ("BICYCLING".equalsIgnoreCase(step.travelMode)) {
                color = Color.parseColor("#4CAF50"); // verde bici
            } else if ("TRANSIT".equalsIgnoreCase(step.travelMode)) {
                if (step.transitDetails != null && step.transitDetails.line != null && step.transitDetails.line.vehicle != null) {
                    String tipo = step.transitDetails.line.vehicle.type;
                    if ("SUBWAY".equalsIgnoreCase(tipo))      color = Color.parseColor("#FF9800"); // naranja
                    else if ("BUS".equalsIgnoreCase(tipo))    color = Color.parseColor("#E91E63"); // rosa
                    else if ("TRAM".equalsIgnoreCase(tipo))   color = Color.parseColor("#009688"); // teal
                    else if ("TRAIN".equalsIgnoreCase(tipo))  color = Color.parseColor("#607D8B"); // gris
                    else color = Color.parseColor("#9C27B0"); // púrpura
                } else {
                    color = Color.parseColor("#9C27B0"); // púrpura general transporte
                }
            }

            line.setColor(color);
            line.setWidth(12.0f);
            polylines.add(line);
            mapView.getOverlays().add(line);
        }

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