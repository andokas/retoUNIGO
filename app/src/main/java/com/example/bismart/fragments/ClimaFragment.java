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
import com.example.bismart.network.OpenWeatherApi;
import com.example.bismart.network.OpenWeatherResponse;
import com.example.bismart.network.OpenWeatherRetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClimaFragment extends Fragment {

    private static final String API_KEY = "7dec46e36174f031adcef4826c340199";

    // Coordenadas de Bilbao por defecto
    private static final double LAT_BILBAO = 43.2630;
    private static final double LON_BILBAO = -2.9350;

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
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                obtenerDatosClima(location.getLatitude(), location.getLongitude());
            } else {
                // Sin GPS usamos Bilbao
                obtenerDatosClima(LAT_BILBAO, LON_BILBAO);
            }
        });
    }

    private void obtenerDatosClima(double lat, double lon) {
        OpenWeatherApi api = OpenWeatherRetrofitClient.getClient().create(OpenWeatherApi.class);
        api.getWeather(lat, lon, API_KEY, "metric", "es")
                .enqueue(new Callback<OpenWeatherResponse>() {
                    @Override
                    public void onResponse(Call<OpenWeatherResponse> call,
                                           Response<OpenWeatherResponse> response) {
                        Log.d("CLIMA_OW", "Código: " + response.code());
                        if (response.isSuccessful() && response.body() != null) {
                            Log.d("CLIMA_OW", "Ciudad: " + response.body().cityName);
                            Log.d("CLIMA_OW", "Temp: " + response.body().main.temp);
                            mostrarDatos(response.body());
                        } else {
                            try {
                                Log.e("CLIMA_OW", "Error body: " + response.errorBody().string());
                            } catch (Exception e) {
                                Log.e("CLIMA_OW", "Error: " + response.code());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<OpenWeatherResponse> call, Throwable t) {
                        Log.e("CLIMA_OW", "Fallo: " + t.getMessage());
                    }
                });
    }

    private void mostrarDatos(OpenWeatherResponse data) {
        // Temperatura
        tvTemperatura.setText(String.format("%.1f°C", data.main.temp));

        // Humedad
        tvHumedad.setText(String.format("%.0f%%", data.main.humidity));

        // Viento (m/s → km/h)
        double vientoKmh = data.wind.speed * 3.6;
        tvViento.setText(String.format("%.1f km/h", vientoKmh));

        // Lluvia (puede ser null si no llueve)
        double lluvia = data.rain != null ? data.rain.oneHour : 0;
        tvLluvia.setText(String.format("%.1f mm", lluvia));

        // Nombre de la ciudad
        String descripcion = data.weather != null && !data.weather.isEmpty()
                ? data.weather.get(0).description : "";
        tvEstacion.setText("📍 " + data.cityName + " · " + descripcion);

        // Aviso si llueve o viento fuerte
        if (lluvia > 0 || vientoKmh > 40) {
            String aviso = lluvia > 0
                    ? "🌧 Está lloviendo. Considera usar el autobús o tranvía."
                    : "💨 Viento fuerte. No recomendable ir en bici.";
            tvAviso.setText(aviso);
            cardAviso.setVisibility(View.VISIBLE);
        } else {
            cardAviso.setVisibility(View.GONE);
        }
    }
}