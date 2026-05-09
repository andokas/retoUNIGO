package com.example.bismart.ui;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bismart.R;
import com.example.bismart.network.DirectionsResponse;

import java.util.List;

public class IndicacionesAdapter extends RecyclerView.Adapter<IndicacionesAdapter.ViewHolder> {

    private final List<DirectionsResponse.Step> steps;

    public IndicacionesAdapter(List<DirectionsResponse.Step> steps) {
        this.steps = steps;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_indicacion, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DirectionsResponse.Step step = steps.get(position);

        holder.tvNumero.setText(String.valueOf(position + 1));

        // html_instructions viene con tags HTML (<b>, <div>), los limpiamos
        String instruccion = Html.fromHtml(step.htmlInstructions, Html.FROM_HTML_MODE_LEGACY).toString();
        holder.tvInstruccion.setText(instruccion);

        if (step.distance != null) {
            holder.tvDistancia.setText(step.distance.text + " · " + step.duration.text);
        }
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumero, tvInstruccion, tvDistancia;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumero = itemView.findViewById(R.id.tvNumero);
            tvInstruccion = itemView.findViewById(R.id.tvInstruccion);
            tvDistancia = itemView.findViewById(R.id.tvDistanciaPaso);
        }
    }
}