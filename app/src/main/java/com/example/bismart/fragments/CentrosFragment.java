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
import com.example.bismart.repositories.UsuarioRepository;
import com.example.bismart.ui.CentroAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CentrosFragment extends Fragment {

    private RecyclerView recyclerView;
    private CentroAdapter adapter;
    private List<CentroUniversitario> listaCentros = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private Location miUbicacion;
    private Set<String> favoritosSet = new HashSet<>();
    private UsuarioRepository usuarioRepository;
    private String uidActual;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_centros, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewCentros);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        usuarioRepository = new UsuarioRepository();
        uidActual = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Buscador
        SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                if (adapter != null) adapter.filtrar(newText);
                return true;
            }
        });

        // Chips de universidad
        ChipGroup chipUniversidad = view.findViewById(R.id.chipGroupUniversidad);
        chipUniversidad.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty() || adapter == null) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipTodas)      adapter.filtrarPorEntidad("Todas");
            else if (id == R.id.chipEHU)   adapter.filtrarPorEntidad("EHU");
            else if (id == R.id.chipDeusto) adapter.filtrarPorEntidad("Deusto");
            else if (id == R.id.chipMU)    adapter.filtrarPorEntidad("MU");
            else if (id == R.id.chipFavoritos) adapter.mostrarSoloFavoritos(favoritosSet);
        });

        obtenerUbicacionYCentros();
        return view;
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacionYCentros() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            miUbicacion = location;
            cargarFavoritosYCentros();
        });
    }

    private void cargarFavoritosYCentros() {
        // Primero cargamos los favoritos, luego los centros
        usuarioRepository.obtenerFavoritos(uidActual)
                .addOnSuccessListener(querySnapshot -> {
                    favoritosSet.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        favoritosSet.add(doc.getId());
                    }
                    descargarCentrosDeFirebase();
                })
                .addOnFailureListener(e -> descargarCentrosDeFirebase());
    }

    private void descargarCentrosDeFirebase() {
        new CentroRepository().obtenerTodos().addOnSuccessListener(queryDocumentSnapshots -> {
            listaCentros.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                CentroUniversitario centro = doc.toObject(CentroUniversitario.class);
                if (centro != null) listaCentros.add(centro);
            }

            adapter = new CentroAdapter(listaCentros, miUbicacion, centro -> {
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

            // Pasar favoritos al adapter
            adapter.setFavoritos(favoritosSet);

            // Listener para guardar/borrar favoritos en Firestore
            adapter.setFavoritoListener((centro, esFavorito) -> {
                if (esFavorito) {
                    favoritosSet.add(centro.nombre);
                    usuarioRepository.añadirFavorito(uidActual, centro);
                } else {
                    favoritosSet.remove(centro.nombre);
                    usuarioRepository.eliminarFavorito(uidActual, centro.nombre);
                }
            });

            recyclerView.setAdapter(adapter);
        });
    }
}