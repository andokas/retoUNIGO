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

    public CentroAdapter(List<CentroUniversitario> listaOriginal, Location ubicacionUsuario, OnCentroClickListener listener) {
        this.listaOriginal = listaOriginal;
        this.listaFiltrada = new ArrayList<>(listaOriginal);
        this.ubicacionUsuario = ubicacionUsuario;
        this.listener = listener;
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

            String distTexto = distanciaMetros < 1000
                    ? (int) distanciaMetros + " m"
                    : String.format("%.1f km", distanciaMetros / 1000);

            holder.tvDistancia.setText(distTexto);
        } else {
            holder.tvDistancia.setText("Calculando distancia...");
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

        public CentroViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreCentro);
            tvEntidad = itemView.findViewById(R.id.tvEntidadCentro);
            tvDistancia = itemView.findViewById(R.id.tvDistancia);
        }
    }
}