package com.example.bismart.fragments;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.bismart.R;
import com.example.bismart.network.EuskalmetApi;
import com.example.bismart.network.EuskalmetReadingResponse;
import com.example.bismart.network.EuskalmetRetrofitClient;
import com.example.bismart.network.EuskalmetStationsResponse;
import com.example.bismart.network.EuskalmetTokenManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClimaFragment extends Fragment {

    // ID de la estación de Bilbao (Miribilla)
    private static final String ESTACION_BILBAO = "B024";

    private TextView tvTemperatura, tvLluvia, tvViento, tvHumedad, tvEstacion, tvAviso;
    private CardView cardAviso;
    private FusedLocationProviderClient fusedLocationClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clima, container, false);

        tvTemperatura = view.findViewById(R.id.tvTemperatura);
        tvLluvia = view.findViewById(R.id.tvLluvia);
        tvViento = view.findViewById(R.id.tvViento);
        tvHumedad = view.findViewById(R.id.tvHumedad);
        tvEstacion = view.findViewById(R.id.tvEstacion);
        tvAviso = view.findViewById(R.id.tvAviso);
        cardAviso = view.findViewById(R.id.cardAviso);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        cargarClima();
        return view;
    }

    @SuppressLint("MissingPermission")
    private void cargarClima() {
        String token = EuskalmetTokenManager.getToken(
                requireContext(),
                "andonicastellanos07@gmail.com"
        );

        if (token == null) {
            Log.e("CLIMA", "No se pudo generar el token");
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                buscarEstacionMasCercana(location, token);
            } else {
                obtenerDatosEstacion(ESTACION_BILBAO, "Bilbao - Miribilla", token);
            }
        });
    }

    private void buscarEstacionMasCercana(Location miUbicacion, String TOKEN) {
        EuskalmetApi api = EuskalmetRetrofitClient.getClient().create(EuskalmetApi.class);
        api.getStations(TOKEN).enqueue(new Callback<EuskalmetStationsResponse>() {
            @Override
            public void onResponse(Call<EuskalmetStationsResponse> call,
                                   Response<EuskalmetStationsResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().stations != null) {

                    List<EuskalmetStationsResponse.Station> stations = response.body().stations;
                    String idMasCercana = ESTACION_BILBAO;
                    String nombreMasCercana = "Bilbao";
                    double minDistancia = Double.MAX_VALUE;

                    for (EuskalmetStationsResponse.Station s : stations) {
                        if (s.location == null) continue;
                        double dist = calcularDistancia(
                                miUbicacion.getLatitude(), miUbicacion.getLongitude(),
                                s.location.latitude, s.location.longitude);
                        if (dist < minDistancia) {
                            minDistancia = dist;
                            idMasCercana = s.id;
                            nombreMasCercana = s.name;
                        }
                    }
                    obtenerDatosEstacion(idMasCercana, nombreMasCercana, TOKEN);
                } else {
                    obtenerDatosEstacion(ESTACION_BILBAO, "Bilbao - Miribilla", TOKEN);
                }
            }

            @Override
            public void onFailure(Call<EuskalmetStationsResponse> call, Throwable t) {
                Log.e("CLIMA", "Error: " + t.getMessage());
                obtenerDatosEstacion(ESTACION_BILBAO, "Bilbao - Miribilla", TOKEN);
            }
        });
    }

    private void obtenerDatosEstacion(String estacionId, String nombreEstacion, String TOKEN) {
        if (tvEstacion != null) {
            tvEstacion.setText("Estación: " + nombreEstacion);
        }

        EuskalmetApi api = EuskalmetRetrofitClient.getClient().create(EuskalmetApi.class);
        api.getLastReading(TOKEN, estacionId).enqueue(new Callback<EuskalmetReadingResponse>() {
            @Override
            public void onResponse(Call<EuskalmetReadingResponse> call,
                                   Response<EuskalmetReadingResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().readings != null) {
                    procesarLecturas(response.body().readings);
                }
            }

            @Override
            public void onFailure(Call<EuskalmetReadingResponse> call, Throwable t) {
                Log.e("CLIMA", "Error al obtener lecturas: " + t.getMessage());
            }
        });
    }

    private void procesarLecturas(List<EuskalmetReadingResponse.Reading> readings) {
        double temperatura = 0, lluvia = 0, viento = 0, humedad = 0;

        for (EuskalmetReadingResponse.Reading r : readings) {
            if (r.parameterId == null) continue;
            switch (r.parameterId) {
                case "TA":  temperatura = r.value; break; // Temperatura aire
                case "PR":  lluvia = r.value;      break; // Precipitación
                case "VV":  viento = r.value;      break; // Velocidad viento
                case "HR":  humedad = r.value;     break; // Humedad relativa
            }
        }

        final double tempFinal = temperatura;
        final double lluviaFinal = lluvia;
        final double vientoFinal = viento;
        final double humedadFinal = humedad;

        requireActivity().runOnUiThread(() -> {
            tvTemperatura.setText(String.format("%.1f°C", tempFinal));
            tvLluvia.setText(String.format("%.1f mm", lluviaFinal));
            tvViento.setText(String.format("%.1f km/h", vientoFinal));
            tvHumedad.setText(String.format("%.0f%%", humedadFinal));

            // Aviso si llueve o hace mucho viento
            if (lluviaFinal > 0 || vientoFinal > 40) {
                String aviso = lluviaFinal > 0
                        ? "🌧 Está lloviendo. Considera usar el autobús o tranvía."
                        : "💨 Viento fuerte. No recomendable ir en bici.";
                tvAviso.setText(aviso);
                cardAviso.setVisibility(View.VISIBLE);
            } else {
                cardAviso.setVisibility(View.GONE);
            }
        });
    }

    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}