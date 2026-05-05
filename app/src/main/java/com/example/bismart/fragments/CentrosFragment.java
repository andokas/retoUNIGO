package com.example.bismart.fragments;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bismart.R;
import com.example.bismart.ui.CentroAdapter;
import com.example.bismart.models.CentroUniversitario;
import com.example.bismart.repositories.CentroRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CentrosFragment extends Fragment {

    private RecyclerView recyclerView;
    private CentroAdapter adapter;
    private List<CentroUniversitario> listaCentros = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private Location miUbicacion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_centros, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewCentros);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext())); // Mostrar en vertical

        SearchView searchView = view.findViewById(R.id.searchView);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // 1. Obtener ubicación del usuario
        obtenerUbicacionYCentros();

        // 2. Configurar buscador
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) adapter.filtrar(newText);
                return true;
            }
        });

        return view;
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacionYCentros() {
        // Asumimos que los permisos ya se pidieron en el MapaFragment
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            miUbicacion = location; // Puede ser null si el GPS está apagado
            descargarCentrosDeFirebase();
        });
    }

    private void descargarCentrosDeFirebase() {
        new CentroRepository().obtenerTodos().addOnSuccessListener(queryDocumentSnapshots -> {
            listaCentros.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                CentroUniversitario centro = doc.toObject(CentroUniversitario.class);
                if (centro != null) listaCentros.add(centro);
            }

            // Montar la lista
            adapter = new CentroAdapter(listaCentros, miUbicacion);
            recyclerView.setAdapter(adapter);
        });
    }
}