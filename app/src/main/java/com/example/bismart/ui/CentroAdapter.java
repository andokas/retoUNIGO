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

    private List<CentroUniversitario> listaOriginal;
    private List<CentroUniversitario> listaFiltrada;
    private Location ubicacionUsuario;

    public CentroAdapter(List<CentroUniversitario> listaOriginal, Location ubicacionUsuario) {
        this.listaOriginal = listaOriginal;
        this.listaFiltrada = new ArrayList<>(listaOriginal);
        this.ubicacionUsuario = ubicacionUsuario;
    }

    @NonNull
    @Override
    public CentroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_centro, parent, false);
        return new CentroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CentroViewHolder holder, int position) {
        CentroUniversitario centro = listaFiltrada.get(position);
        holder.tvNombre.setText(centro.nombre);
        holder.tvEntidad.setText(centro.entidad + " - " + centro.ubicacion);

        if (ubicacionUsuario != null) {
            // Calcular distancia en línea recta
            Location locCentro = new Location("");
            locCentro.setLatitude(centro.latitud);
            locCentro.setLongitude(centro.longitud);
            float distanciaMetros = ubicacionUsuario.distanceTo(locCentro);

            // Estimación de tiempo (Asumiendo 15 km/h en ciudad -> ~250m/min)
            int minEstimados = (int) (distanciaMetros / 250);

            holder.tvDistancia.setText(String.format("%.1f km (Aprox %d min)", distanciaMetros / 1000, minEstimados));
        } else {
            holder.tvDistancia.setText("Calculando distancia...");
        }

        // Aquí programaremos el click para abrir el Detalle luego
        holder.itemView.setOnClickListener(v -> {
            // TODO: Abrir pantalla de detalle
        });
    }

    @Override
    public int getItemCount() {
        return listaFiltrada.size();
    }

    // Método para el buscador
    public void filtrar(String texto) {
        listaFiltrada.clear();
        if (texto.isEmpty()) {
            listaFiltrada.addAll(listaOriginal);
        } else {
            texto = texto.toLowerCase();
            for (CentroUniversitario c : listaOriginal) {
                if (c.nombre.toLowerCase().contains(texto) || c.entidad.toLowerCase().contains(texto)) {
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