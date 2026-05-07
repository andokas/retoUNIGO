package com.example.bismart.ui;

import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bismart.R;
import com.example.bismart.models.CentroUniversitario;

import java.util.ArrayList;
import java.util.List;

public class CentroAdapter extends RecyclerView.Adapter<CentroAdapter.CentroViewHolder> {

    public interface OnCentroClickListener {
        void onCentroClick(CentroUniversitario centro);
    }

    private List<CentroUniversitario> listaOriginal;
    private List<CentroUniversitario> listaFiltrada;
    private Location ubicacionUsuario;
    private OnCentroClickListener listener;

    // NUEVO: Variable para saber qué transporte está seleccionado
    private String modoTransporte = "A pie";

    public CentroAdapter(List<CentroUniversitario> listaOriginal, Location ubicacionUsuario, OnCentroClickListener listener) {
        this.listaOriginal = listaOriginal;
        this.listaFiltrada = new ArrayList<>(listaOriginal);
        this.ubicacionUsuario = ubicacionUsuario;
        this.listener = listener;
    }

    // NUEVO: Método para cambiar el transporte desde el Fragment
    public void setModoTransporte(String modo) {
        this.modoTransporte = modo;
        notifyDataSetChanged(); // Recarga la lista con los nuevos tiempos
    }

    @NonNull
    @Override
    public CentroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_centro, parent, false);
        return new CentroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CentroViewHolder holder, int position) {
        CentroUniversitario centro = listaFiltrada.get(position);
        holder.tvNombre.setText(centro.nombre);
        holder.tvEntidad.setText(centro.entidad + " - " + centro.ubicacion);

        if (ubicacionUsuario != null) {
            Location locCentro = new Location("");
            locCentro.setLatitude(centro.latitud);
            locCentro.setLongitude(centro.longitud);
            float distanciaMetros = ubicacionUsuario.distanceTo(locCentro);

            // NUEVO: Ajustamos la distancia real (las calles no son en línea recta)
            float factorRuta = 1.3f;
            float distanciaRealMetros = distanciaMetros * factorRuta;

            String distTexto = distanciaRealMetros < 1000
                    ? (int) distanciaRealMetros + " m"
                    : String.format("%.1f km", distanciaRealMetros / 1000);

            holder.tvDistancia.setText(distTexto);

            // NUEVO: Calcular tiempo estimado según el transporte
            int velocidadKmh;
            switch (modoTransporte) {
                case "A pie": velocidadKmh = 5; break;
                case "Bici": velocidadKmh = 15; break;
                case "Metro": velocidadKmh = 35; break;
                case "Tren": velocidadKmh = 45; break;
                default: velocidadKmh = 20; // Bus y Tranvía
            }

            // Tiempo = (Distancia / Velocidad) * 60 minutos + 2 min extra por semáforos/esperas
            float distKm = distanciaRealMetros / 1000;
            int tiempoMinutos = (int) ((distKm / velocidadKmh) * 60) + 2;

            // Si has añadido el tvTiempo en el XML, descomenta esta línea:
            // holder.tvTiempo.setText(tiempoMinutos + " min");

        } else {
            holder.tvDistancia.setText("Calculando distancia...");
            // if(holder.tvTiempo != null) holder.tvTiempo.setText("--");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCentroClick(centro);
        });
    }

    @Override
    public int getItemCount() {
        return listaFiltrada.size();
    }

    public void filtrar(String texto) {
        listaFiltrada.clear();
        if (texto.isEmpty()) {
            listaFiltrada.addAll(listaOriginal);
        } else {
            texto = texto.toLowerCase();
            for (CentroUniversitario c : listaOriginal) {
                if (c.nombre.toLowerCase().contains(texto) ||
                        c.entidad.toLowerCase().contains(texto)) {
                    listaFiltrada.add(c);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class CentroViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvEntidad, tvDistancia;
        // TextView tvTiempo; // Descomenta esto cuando lo añadas al XML

        public CentroViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreCentro);
            tvEntidad = itemView.findViewById(R.id.tvEntidadCentro);
            tvDistancia = itemView.findViewById(R.id.tvDistancia);
            // tvTiempo = itemView.findViewById(R.id.tvTiempo); // Descomenta esto también
        }
    }
}