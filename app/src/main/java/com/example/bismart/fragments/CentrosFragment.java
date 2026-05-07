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
import com.example.bismart.models.CentroUniversitario;
import com.example.bismart.repositories.CentroRepository;
import com.example.bismart.ui.CentroAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                if (adapter != null) adapter.filtrar(newText);
                return true;
            }
        });

        // NUEVO: Escuchar los botones de transporte para recalcular distancias y tiempos
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupTransporteLista);
        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (!checkedIds.isEmpty() && adapter != null) {
                    Chip chip = view.findViewById(checkedIds.get(0));
                    adapter.setModoTransporte(chip.getText().toString());
                }
            });
        }

        obtenerUbicacionYCentros();

        return view;
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacionYCentros() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            miUbicacion = location;
            descargarCentrosDeFirebase();
        });
    }

    private void descargarCentrosDeFirebase() {
        new CentroRepository().obtenerTodos().addOnSuccessListener(queryDocumentSnapshots -> {
            listaCentros.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                CentroUniversitario centro = doc.toObject(CentroUniversitario.class);
                if (centro != null) listaCentros.add(centro);
            }

            adapter = new CentroAdapter(listaCentros, miUbicacion, centro -> {
                // Abrir MapaFragment con el centro seleccionado
                Bundle args = new Bundle();
                args.putDouble("lat", centro.latitud);
                args.putDouble("lng", centro.longitud);
                args.putString("nombre", centro.nombre);
                args.putString("entidad", centro.entidad);
                args.putString("ubicacion", centro.ubicacion);

                MapaFragment mapaFragment = new MapaFragment();
                mapaFragment.setArguments(args);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, mapaFragment)
                        .commit();
            });

            recyclerView.setAdapter(adapter);
        });
    }
}