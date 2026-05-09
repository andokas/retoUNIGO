package com.example.bismart.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bismart.R;
import com.example.bismart.activities.LoginActivity;
import com.example.bismart.repositories.UsuarioRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PerfilFragment extends Fragment {

    private TextView tvNombre, tvEmail;
    private RadioGroup radioGroupTransporte, radioGroupIdioma;
    private MaterialButton btnGuardarTransporte, btnGuardarIdioma, btnCerrarSesion;
    private UsuarioRepository usuarioRepository;
    private String uidActual;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        tvNombre = view.findViewById(R.id.tvNombreUsuario);
        tvEmail = view.findViewById(R.id.tvEmailUsuario);
        radioGroupTransporte = view.findViewById(R.id.radioGroupTransporte);
        radioGroupIdioma = view.findViewById(R.id.radioGroupIdioma);
        btnGuardarTransporte = view.findViewById(R.id.btnGuardarTransporte);
        btnGuardarIdioma = view.findViewById(R.id.btnGuardarIdioma);
        btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);

        usuarioRepository = new UsuarioRepository();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            uidActual = user.getUid();
            tvNombre.setText(user.getDisplayName() != null ? user.getDisplayName() : "Usuario");
            tvEmail.setText(user.getEmail());
            cargarPreferenciasGuardadas();
        }

        btnGuardarTransporte.setOnClickListener(v -> guardarTransporte());
        btnGuardarIdioma.setOnClickListener(v -> guardarIdioma());
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());

        return view;
    }

    private void cargarPreferenciasGuardadas() {
        usuarioRepository.obtenerUsuario(uidActual)
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    // Transporte
                    String transporte = doc.getString("transportePreferido");
                    if (transporte != null) {
                        switch (transporte) {
                            case "pie":     radioGroupTransporte.check(R.id.radioPie);    break;
                            case "bici":    radioGroupTransporte.check(R.id.radioBici);   break;
                            case "bus":     radioGroupTransporte.check(R.id.radioBus);    break;
                            case "tranvia": radioGroupTransporte.check(R.id.radioTranvia);break;
                        }
                    }

                    // Idioma
                    String idioma = doc.getString("idioma");
                    if (idioma != null) {
                        switch (idioma) {
                            case "es": radioGroupIdioma.check(R.id.radioEspanol); break;
                            case "eu": radioGroupIdioma.check(R.id.radioEuskera); break;
                            case "en": radioGroupIdioma.check(R.id.radioIngles);  break;
                        }
                    } else {
                        // Español por defecto
                        radioGroupIdioma.check(R.id.radioEspanol);
                    }
                });
    }

    private void guardarTransporte() {
        int selectedId = radioGroupTransporte.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(getContext(), "Selecciona un modo de transporte", Toast.LENGTH_SHORT).show();
            return;
        }
        String transporte = ((RadioButton) radioGroupTransporte.findViewById(selectedId)).getTag().toString();
        usuarioRepository.actualizarTransporte(uidActual, transporte)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(), "Transporte guardado ✓", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show());
    }

    private void guardarIdioma() {
        int selectedId = radioGroupIdioma.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(getContext(), "Selecciona un idioma", Toast.LENGTH_SHORT).show();
            return;
        }
        String idioma = ((RadioButton) radioGroupIdioma.findViewById(selectedId)).getTag().toString();
        usuarioRepository.actualizarIdioma(uidActual, idioma)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(), "Idioma guardado ✓", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show());
    }

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getActivity(), LoginActivity.class));
        requireActivity().finish();
    }
}