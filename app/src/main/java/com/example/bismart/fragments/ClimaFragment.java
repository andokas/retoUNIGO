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
import com.example.bismart.network.AireApi;
import com.example.bismart.network.AireEstacionResponse;
import com.example.bismart.network.AireMedicionResponse;
import com.example.bismart.network.AireRetrofitClient;
import com.example.bismart.network.OpenWeatherApi;
import com.example.bismart.network.OpenWeatherResponse;
import com.example.bismart.network.OpenWeatherRetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClimaFragment extends Fragment {

    private static final String API_KEY = "7dec46e36174f031adcef4826c340199";

    // Coordenadas de Bilbao por defecto
    private static final double LAT_BILBAO = 43.2630;
    private static final double LON_BILBAO = -2.9350;

    private TextView tvTemperatura, tvLluvia, tvViento, tvHumedad, tvEstacion, tvAviso, tvEstacionAire, tvIndiceCalidad, tvDescripcionCalidad, tvConsejoAire;
    private CardView cardAviso, cardIndiceCalidad;
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
        tvEstacionAire = view.findViewById(R.id.tvEstacionAire);
        tvIndiceCalidad = view.findViewById(R.id.tvIndiceCalidad);
        tvDescripcionCalidad = view.findViewById(R.id.tvDescripcionCalidad);
        tvConsejoAire = view.findViewById(R.id.tvConsejoAire);
        cardIndiceCalidad = view.findViewById(R.id.cardIndiceCalidad);

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
            cargarCalidadAire(location);
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

    private void cargarCalidadAire(Location location) {
        Log.d("AIRE_DEBUG", "1. Pidiendo estaciones...");
        AireApi api = AireRetrofitClient.getClient().create(AireApi.class);
        api.getEstaciones().enqueue(new Callback<List<AireEstacionResponse>>() {
            @Override
            public void onResponse(Call<List<AireEstacionResponse>> call,
                                   Response<List<AireEstacionResponse>> response) {
                Log.d("AIRE_DEBUG", "2. Código: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("AIRE_DEBUG", "3. Estaciones recibidas: " + response.body().size());
                    AireEstacionResponse cercana = encontrarEstacionMasCercana(response.body(), location);
                    if (cercana != null) {
                        Log.d("AIRE_DEBUG", "4. Estación más cercana: " + cercana.nombre);
                        obtenerMedicionAire(cercana);
                    }
                } else {
                    try {
                        Log.e("AIRE_DEBUG", "Error body: " + response.errorBody().string());
                    } catch (Exception e) {
                        Log.e("AIRE_DEBUG", "Error código: " + response.code());
                    }
                }
            }
            @Override
            public void onFailure(Call<List<AireEstacionResponse>> call, Throwable t) {
                Log.e("AIRE_DEBUG", "FALLO: " + t.getMessage());
            }
        });
    }

    private AireEstacionResponse encontrarEstacionMasCercana(List<AireEstacionResponse> estaciones,
                                                             Location miUbicacion) {
        if (estaciones == null || estaciones.isEmpty()) return null;
        AireEstacionResponse cercana = estaciones.get(0);
        double minDist = Double.MAX_VALUE;
        double lat = miUbicacion != null ? miUbicacion.getLatitude() : 43.2630;
        double lon = miUbicacion != null ? miUbicacion.getLongitude() : -2.9350;
        for (AireEstacionResponse e : estaciones) {
            double dist = Math.sqrt(Math.pow(e.getLatitud() - lat, 2) + Math.pow(e.getLongitud() - lon, 2));
            if (dist < minDist) { minDist = dist; cercana = e; }
        }
        return cercana;
    }

    private void obtenerMedicionAire(AireEstacionResponse estacion) {
        tvEstacionAire.setText("📍 " + estacion.nombre + " · " + estacion.municipio);

        AireApi api = AireRetrofitClient.getClient().create(AireApi.class);
        api.getMedicion(estacion.nombre).enqueue(new Callback<List<AireMedicionResponse>>() {
            @Override
            public void onResponse(Call<List<AireMedicionResponse>> call,
                                   Response<List<AireMedicionResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Buscar la lectura más reciente que tenga ICAEstacion
                    for (AireMedicionResponse lectura : response.body()) {
                        if (lectura.icaEstacion != null && !lectura.icaEstacion.isEmpty()) {
                            mostrarIndiceCalidad(lectura.icaEstacion);
                            return;
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<List<AireMedicionResponse>> call, Throwable t) {
                Log.e("AIRE", "Error medición: " + t.getMessage());
            }
        });
    }

    private void mostrarIndiceCalidad(String ica) {
        // El ICA viene como "Muy buena / Oso ona", "Buena / Ona", etc.
        // Cogemos solo la parte en español (antes del /)
        String[] partes = ica.split("/");
        String descripcion = partes[0].trim();

        tvDescripcionCalidad.setText(descripcion);

        int color;
        String consejo;
        String icaLower = descripcion.toLowerCase();

        if (icaLower.contains("muy buena")) {
            tvIndiceCalidad.setText("1");
            color = 0xFF4CAF50;
            consejo = "✅ Aire excelente. Perfecto para ir en bici o a pie.";
        } else if (icaLower.contains("buena")) {
            tvIndiceCalidad.setText("2");
            color = 0xFF8BC34A;
            consejo = "✅ Aire bueno. Puedes moverte sin problema.";
        } else if (icaLower.contains("admisible")) {
            tvIndiceCalidad.setText("3");
            color = 0xFFFFC107;
            consejo = "⚠️ Calidad admisible. Personas sensibles deben tener precaución.";
        } else if (icaLower.contains("regular") || icaLower.contains("mala")) {
            tvIndiceCalidad.setText("4");
            color = 0xFFFF9800;
            consejo = "⚠️ Calidad regular. Recomendable usar transporte público.";
        } else if (icaLower.contains("muy mala")) {
            tvIndiceCalidad.setText("5");
            color = 0xFFF44336;
            consejo = "🚫 Aire malo. Evita ejercicio físico al aire libre.";
        } else {
            tvIndiceCalidad.setText("6");
            color = 0xFF9C27B0;
            consejo = "🚫 Calidad extremadamente mala. Quédate en interior.";
        }

        cardIndiceCalidad.setCardBackgroundColor(color);
        tvConsejoAire.setText(consejo);
    }
}