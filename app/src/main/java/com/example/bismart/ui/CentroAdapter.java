package com.example.bismart.ui;

import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bismart.R;
import com.example.bismart.models.CentroUniversitario;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CentroAdapter extends RecyclerView.Adapter<CentroAdapter.CentroViewHolder> {

    public interface OnCentroClickListener {
        void onCentroClick(CentroUniversitario centro);
    }

    public interface OnFavoritoClickListener {
        void onFavoritoClick(CentroUniversitario centro, boolean esFavorito);
    }

    private List<CentroUniversitario> listaOriginal;
    private List<CentroUniversitario> listaFiltrada;
    private Location ubicacionUsuario;
    private OnCentroClickListener listener;
    private OnFavoritoClickListener favListener;
    private Set<String> favoritos = new HashSet<>();
    private String modoTransporte = "A pie";

    public CentroAdapter(List<CentroUniversitario> listaOriginal, Location ubicacionUsuario,
                         OnCentroClickListener listener) {
        this.listaOriginal = listaOriginal;
        this.listaFiltrada = new ArrayList<>(listaOriginal);
        this.ubicacionUsuario = ubicacionUsuario;
        this.listener = listener;
    }

    public void setFavoritoListener(OnFavoritoClickListener favListener) {
        this.favListener = favListener;
    }

    public void setFavoritos(Set<String> favoritos) {
        this.favoritos = favoritos;
        notifyDataSetChanged();
    }

    public void setModoTransporte(String modo) {
        this.modoTransporte = modo;
        notifyDataSetChanged();
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

        // Distancia y tiempo
        if (ubicacionUsuario != null) {
            Location locCentro = new Location("");
            locCentro.setLatitude(centro.latitud);
            locCentro.setLongitude(centro.longitud);
            float distanciaMetros = ubicacionUsuario.distanceTo(locCentro) * 1.3f;

            String distTexto = distanciaMetros < 1000
                    ? (int) distanciaMetros + " m"
                    : String.format("%.1f km", distanciaMetros / 1000);

            int velocidadKmh;
            switch (modoTransporte) {
                case "Bici":   velocidadKmh = 15; break;
                case "Metro":  velocidadKmh = 35; break;
                case "Tren":   velocidadKmh = 45; break;
                default:       velocidadKmh = 20; break; // Bus, Tranvía
                case "A pie":  velocidadKmh = 5;  break;
            }

            int tiempoMinutos = (int) ((distanciaMetros / 1000f / velocidadKmh) * 60) + 2;
            holder.tvDistancia.setText(distTexto + " · ~" + tiempoMinutos + " min");
        } else {
            holder.tvDistancia.setText("Calculando distancia...");
        }

        // Favorito
        boolean esFav = favoritos.contains(centro.nombre);
        holder.btnFavorito.setImageResource(esFav
                ? android.R.drawable.btn_star_big_on
                : android.R.drawable.btn_star_big_off);

        holder.btnFavorito.setOnClickListener(v -> {
            boolean nuevoEstado = !favoritos.contains(centro.nombre);
            if (nuevoEstado) favoritos.add(centro.nombre);
            else favoritos.remove(centro.nombre);
            notifyItemChanged(position);
            if (favListener != null) favListener.onFavoritoClick(centro, nuevoEstado);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCentroClick(centro);
        });
    }

    @Override
    public int getItemCount() { return listaFiltrada.size(); }

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

    public void filtrarPorEntidad(String entidad) {
        listaFiltrada.clear();
        if (entidad.equals("Todas")) {
            listaFiltrada.addAll(listaOriginal);
        } else {
            for (CentroUniversitario c : listaOriginal) {
                if (c.entidad.equalsIgnoreCase(entidad)) listaFiltrada.add(c);
            }
        }
        notifyDataSetChanged();
    }

    public void mostrarSoloFavoritos(Set<String> favs) {
        listaFiltrada.clear();
        for (CentroUniversitario c : listaOriginal) {
            if (favs.contains(c.nombre)) listaFiltrada.add(c);
        }
        notifyDataSetChanged();
    }

    static class CentroViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvEntidad, tvDistancia;
        ImageButton btnFavorito;

        public CentroViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreCentro);
            tvEntidad = itemView.findViewById(R.id.tvEntidadCentro);
            tvDistancia = itemView.findViewById(R.id.tvDistancia);
            btnFavorito = itemView.findViewById(R.id.btnFavorito);
        }
    }
}