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
import com.example.bismart.network.DirectionsApi;
import com.example.bismart.network.DirectionsResponse;
import com.example.bismart.network.GoogleMapsRetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapaFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    private LatLng centroSeleccionado;
    private String centroNombre;
    private String centroEntidad;
    private String centroUbicacion;

    private ChipGroup chipGroup;
    private String transporteActual = "pie";
    private Polyline rutaActual;
    private Marker markerCentro;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    obtenerUbicacionActual();
                } else {
                    Toast.makeText(getContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                    centrarEnBilbao();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapa, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Leer centro seleccionado si viene desde la lista
        Bundle args = getArguments();
        if (args != null) {
            double lat = args.getDouble("lat", Double.NaN);
            double lng = args.getDouble("lng", Double.NaN);
            if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
                centroSeleccionado = new LatLng(lat, lng);
                centroNombre = args.getString("nombre", "");
                centroEntidad = args.getString("entidad", "");
                centroUbicacion = args.getString("ubicacion", "");
            }
        }

        chipGroup = view.findViewById(R.id.chipGroupTransporte);
        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);
                if (id == R.id.chipPie) transporteActual = "pie";
                else if (id == R.id.chipBici) transporteActual = "bici";
                else if (id == R.id.chipBus) transporteActual = "bus";
                else if (id == R.id.chipTranvia) transporteActual = "tranvia";

                recalcularRuta();
            });
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionActual();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacionActual() {
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                LatLng miUbicacion = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(miUbicacion, 14f));

                if (centroSeleccionado != null) {
                    colocarMarkerCentro();
                    recalcularRuta();
                }
            } else {
                centrarEnBilbao();
            }
        });
    }

    private void colocarMarkerCentro() {
        if (mMap == null || centroSeleccionado == null) return;

        if (markerCentro != null) markerCentro.remove();
        markerCentro = mMap.addMarker(new MarkerOptions()
                .position(centroSeleccionado)
                .title(centroNombre)
                .snippet(centroEntidad + " - " + centroUbicacion));
    }

    private void recalcularRuta() {
        if (centroSeleccionado == null) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                pedirRutaYTiempo(location);
            }
        });
    }

    private void pedirRutaYTiempo(Location location) {
        String origin = location.getLatitude() + "," + location.getLongitude();
        String destination = centroSeleccionado.latitude + "," + centroSeleccionado.longitude;

        String mode;
        String transitMode = null;

        switch (transporteActual) {
            case "bus":
                mode = "transit";
                transitMode = "bus";
                break;
            case "tranvia":
                mode = "transit";
                transitMode = "tram";
                break;
            case "bici":
                mode = "bicycling";
                break;
            default:
                mode = "walking";
        }

        DirectionsApi api = GoogleMapsRetrofitClient.getClient().create(DirectionsApi.class);
        api.getDirections(origin, destination, mode, transitMode, "TU_API_KEY")
                .enqueue(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().routes != null && !response.body().routes.isEmpty()) {

                            DirectionsResponse.Route route = response.body().routes.get(0);
                            if (route.legs == null || route.legs.isEmpty()) return;

                            String tiempo = route.legs.get(0).duration.text;

                            if (markerCentro != null) {
                                markerCentro.setSnippet(centroEntidad + " - " + centroUbicacion + "\nTiempo: " + tiempo);
                                markerCentro.showInfoWindow();
                            }

                            if (rutaActual != null) rutaActual.remove();
                            if (route.overview_polyline != null && route.overview_polyline.points != null) {
                                List<LatLng> puntos = decodePolyline(route.overview_polyline.points);
                                rutaActual = mMap.addPolyline(new PolylineOptions().addAll(puntos));
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) { }
                });
    }

    private void centrarEnBilbao() {
        if (mMap != null) {
            LatLng bilbao = new LatLng(43.2630, -2.9350);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(bilbao, 13f));
        }
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
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

            LatLng p = new LatLng(lat / 1E5, lng / 1E5);
            poly.add(p);
        }

        return poly;
    }
}