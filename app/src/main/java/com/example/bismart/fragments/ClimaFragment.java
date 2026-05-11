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
import com.example.bismart.utils.LocaleHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClimaFragment extends Fragment {

    private static final String API_KEY = "7dec46e36174f031adcef4826c340199";
    private static final double LAT_BILBAO = 43.2630;
    private static final double LON_BILBAO = -2.9350;

    private TextView tvTemperatura, tvLluvia, tvViento, tvHumedad, tvAviso,
            tvIndiceCalidad, tvDescripcionCalidad, tvConsejoAire;
    private TextView tvIconoEstado, tvDescripcionEstado;
    private CardView cardAviso, cardIndiceCalidad;

    private FusedLocationProviderClient fusedLocationClient;

    private static final Set<String> NOMBRES_ESTACIONES_CON_DATOS = new HashSet<>(Arrays.asList(
            "ABANTO", "AMURRIO", "ARRIGORRIAGA", "ATOCHA", "AZPEITIA", "BARAKALDO", "BASURTO",
            "BERMEO", "DURANGO", "EIBAR", "ERANDIO", "ERRENTERIA", "GETXO", "GALDAKAO",
            "GALDAKAO B", "IRUN", "IZARRA", "KAREAGA", "LAVAGA", "LEGAZPI", "MAIRAGA",
            "MIRAVALLES", "MUNGIA", "ORTUELLA", "PORTUGALETE", "SAKANA", "SALBURUA",
            "SANTURTZI", "UNBE", "UNIDAD MOVIL 1", "UNIDAD MOVIL 3", "VALLE DE TRAPAGA",
            "VDA ARETXABALETA", "VDA ANASTRA", "VDA BASAURI", "VDA BASURTO", "VDA EIBAR",
            "VDA GASTEIZ", "VDA IRUN", "VDA LODOSA", "VDA PORTU", "VDA SANTURTZI",
            "VDA TOLOSA", "ZALLA", "ZARAUTZ"
    ));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clima, container, false);

        tvIconoEstado = view.findViewById(R.id.ivIconoEstado);
        tvDescripcionEstado = view.findViewById(R.id.tvDescripcionEstado);
        tvTemperatura = view.findViewById(R.id.tvTemperatura);
        tvLluvia = view.findViewById(R.id.tvLluvia);
        tvViento = view.findViewById(R.id.tvViento);
        tvHumedad = view.findViewById(R.id.tvHumedad);
        tvAviso = view.findViewById(R.id.tvAviso);
        cardAviso = view.findViewById(R.id.cardAviso);
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
                obtenerDatosClima(LAT_BILBAO, LON_BILBAO);
            }
            cargarCalidadAire(location);
        });
    }

    private void obtenerDatosClima(double lat, double lon) {
        // Usa el idioma guardado en la app
        String idioma = LocaleHelper.getIdiomaGuardado(requireContext());

        OpenWeatherApi api = OpenWeatherRetrofitClient.getClient().create(OpenWeatherApi.class);
        api.getWeather(lat, lon, API_KEY, "metric", idioma)
                .enqueue(new Callback<OpenWeatherResponse>() {
                    @Override
                    public void onResponse(Call<OpenWeatherResponse> call,
                                           Response<OpenWeatherResponse> response) {
                        Log.d("CLIMA_OW", "Código: " + response.code());
                        if (response.isSuccessful() && response.body() != null) {
                            mostrarDatos(response.body());
                        } else {
                            try {
                                Log.e("CLIMA_OW", "Error: " + response.errorBody().string());
                            } catch (Exception e) {
                                Log.e("CLIMA_OW", "Error código: " + response.code());
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
        if (data.weather != null && !data.weather.isEmpty()) {
            setIconoYDescripcionCielo(data.weather.get(0).main, data.weather.get(0).description);
        } else {
            tvIconoEstado.setText("🌡");
            tvDescripcionEstado.setText("-");
        }

        tvTemperatura.setText(String.format("%.1f°C", data.main.temp));
        tvHumedad.setText(String.format("%.0f%%", data.main.humidity));
        double vientoKmh = data.wind.speed * 3.6;
        tvViento.setText(String.format("%.1f km/h", vientoKmh));
        double lluvia = data.rain != null ? data.rain.oneHour : 0;
        tvLluvia.setText(String.format("%.1f mm", lluvia));

        if (lluvia > 0 || vientoKmh > 40) {
            String aviso = lluvia > 0
                    ? getString(R.string.aviso_lluvia)
                    : getString(R.string.aviso_viento);
            tvAviso.setText(aviso);
            cardAviso.setVisibility(View.VISIBLE);
        } else {
            cardAviso.setVisibility(View.GONE);
        }
    }

    private void setIconoYDescripcionCielo(String estado, String descripcionApi) {
        if (estado == null) estado = "";
        switch (estado) {
            case "Clear":
                tvIconoEstado.setText("☀️");
                tvDescripcionEstado.setText(getString(R.string.cielo_soleado));
                break;
            case "Clouds":
                tvIconoEstado.setText("☁️");
                tvDescripcionEstado.setText(getString(R.string.cielo_nuboso));
                break;
            case "Rain":
                tvIconoEstado.setText("🌧");
                tvDescripcionEstado.setText(getString(R.string.cielo_lluvia));
                break;
            case "Thunderstorm":
                tvIconoEstado.setText("⛈");
                tvDescripcionEstado.setText(getString(R.string.cielo_tormenta));
                break;
            case "Snow":
                tvIconoEstado.setText("❄️");
                tvDescripcionEstado.setText(getString(R.string.cielo_nieve));
                break;
            case "Drizzle":
                tvIconoEstado.setText("🌦️");
                tvDescripcionEstado.setText(getString(R.string.cielo_chubascos));
                break;
            case "Mist":
            case "Fog":
                tvIconoEstado.setText("🌫");
                tvDescripcionEstado.setText(getString(R.string.cielo_niebla));
                break;
            default:
                tvIconoEstado.setText("🌡");
                tvDescripcionEstado.setText(capitalizeFirst(descripcionApi));
                break;
        }
    }

    private String capitalizeFirst(String texto) {
        if (texto == null || texto.isEmpty()) return "";
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
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
                    List<AireEstacionResponse> estacionesConDatos = filtrarEstacionesConDatos(response.body());
                    Log.d("AIRE_DEBUG", "Estaciones con datos: " + estacionesConDatos.size());
                    if (estacionesConDatos.isEmpty()) {
                        Log.e("AIRE_DEBUG", "No hay estaciones con datos.");
                        return;
                    }
                    AireEstacionResponse cercana = encontrarEstacionMasCercana(estacionesConDatos, location);
                    if (cercana != null) {
                        Log.d("AIRE_DEBUG", "4. Estación más cercana: " + cercana.nombre);
                        obtenerMedicionAire(cercana);
                    }
                } else {
                    try {
                        Log.e("AIRE_DEBUG", "Error: " + response.errorBody().string());
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

    private List<AireEstacionResponse> filtrarEstacionesConDatos(List<AireEstacionResponse> estaciones) {
        List<AireEstacionResponse> resultado = new ArrayList<>();
        for (AireEstacionResponse est : estaciones) {
            String nombreLimpio = quitarTildes(est.nombre);
            if (NOMBRES_ESTACIONES_CON_DATOS.contains(nombreLimpio)) {
                resultado.add(est);
            }
        }
        return resultado;
    }

    private AireEstacionResponse encontrarEstacionMasCercana(List<AireEstacionResponse> estaciones,
                                                             Location miUbicacion) {
        if (estaciones == null || estaciones.isEmpty()) return null;
        AireEstacionResponse cercana = estaciones.get(0);
        double minDist = Double.MAX_VALUE;
        double lat = miUbicacion != null ? miUbicacion.getLatitude() : LAT_BILBAO;
        double lon = miUbicacion != null ? miUbicacion.getLongitude() : LON_BILBAO;
        for (AireEstacionResponse e : estaciones) {
            double dist = Math.sqrt(Math.pow(e.getLatitud() - lat, 2) + Math.pow(e.getLongitud() - lon, 2));
            if (dist < minDist) {
                minDist = dist;
                cercana = e;
            }
        }
        return cercana;
    }

    private void obtenerMedicionAire(AireEstacionResponse estacion) {
        String nombreArchivo = quitarTildes(estacion.nombre);
        Log.d("AIRE_DEBUG", "5. Pidiendo medición para: '" + nombreArchivo + "'");

        AireApi api = AireRetrofitClient.getClient().create(AireApi.class);
        api.getMedicion(nombreArchivo).enqueue(new Callback<List<AireMedicionResponse>>() {
            @Override
            public void onResponse(Call<List<AireMedicionResponse>> call,
                                   Response<List<AireMedicionResponse>> response) {
                Log.d("AIRE_DEBUG", "6. Código medición: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("AIRE_DEBUG", "7. Lecturas: " + response.body().size());
                    for (AireMedicionResponse lectura : response.body()) {
                        if (lectura.icaEstacion != null && !lectura.icaEstacion.isEmpty()) {
                            mostrarIndiceCalidad(lectura.icaEstacion);
                            return;
                        }
                    }
                    Log.e("AIRE_DEBUG", "Ninguna lectura tiene ICAEstacion");
                } else {
                    try {
                        Log.e("AIRE_DEBUG", "Error: " + response.errorBody().string());
                    } catch (Exception e) {
                        Log.e("AIRE_DEBUG", "Error código: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<List<AireMedicionResponse>> call, Throwable t) {
                Log.e("AIRE_DEBUG", "Error medición: " + t.getMessage());
            }
        });
    }

    private void mostrarIndiceCalidad(String ica) {
        String[] partes = ica.split("/");
        String descripcion = partes[0].trim();

        int descr = R.string.clima_cargando;
        int color;
        int consejoRes;
        String icaLower = descripcion.toLowerCase();

        if (icaLower.contains("muy buena") || icaLower.contains("oso ona")) {
            tvIndiceCalidad.setText("1");
            descr = R.string.aire_1;
            color = 0xFF4CAF50;
            consejoRes = R.string.aire_consejo_1;
        } else if (icaLower.contains("buena") || icaLower.contains("ona")) {
            tvIndiceCalidad.setText("2");
            descr = R.string.aire_2;
            color = 0xFF8BC34A;
            consejoRes = R.string.aire_consejo_2;
        } else if (icaLower.contains("admisible") || icaLower.contains("onartekoa")) {
            descr = R.string.aire_3;
            tvIndiceCalidad.setText("3");
            color = 0xFFFFC107;
            consejoRes = R.string.aire_consejo_3;
        } else if (icaLower.contains("regular") || icaLower.contains("arrunta")) {
            descr = R.string.aire_4;
            tvIndiceCalidad.setText("4");
            color = 0xFFFF9800;
            consejoRes = R.string.aire_consejo_4;
        } else if (icaLower.contains("muy mala") || icaLower.contains("oso txarra")) {
            descr = R.string.aire_5;
            tvIndiceCalidad.setText("5");
            color = 0xFFF44336;
            consejoRes = R.string.aire_consejo_5;
        } else {
            descr = R.string.aire_6;
            tvIndiceCalidad.setText("6");
            color = 0xFF9C27B0;
            consejoRes = R.string.aire_consejo_6;
        }

        tvDescripcionCalidad.setText(getString(descr));
        cardIndiceCalidad.setCardBackgroundColor(color);
        tvConsejoAire.setText(getString(consejoRes));
    }

    public static String quitarTildes(String input) {
        if (input == null) return null;
        return input
                .replace("Á", "A").replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U")
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u")
                .replace("Ü", "U").replace("ü", "u")
                .replace("Ñ", "N").replace("ñ", "n");
    }
}