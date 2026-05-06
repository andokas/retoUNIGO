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
    private RadioGroup radioGroup;
    private MaterialButton btnGuardar, btnCerrarSesion;
    private UsuarioRepository usuarioRepository;
    private String uidActual;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        tvNombre = view.findViewById(R.id.tvNombreUsuario);
        tvEmail = view.findViewById(R.id.tvEmailUsuario);
        radioGroup = view.findViewById(R.id.radioGroupTransporte);
        btnGuardar = view.findViewById(R.id.btnGuardarTransporte);
        btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);

        usuarioRepository = new UsuarioRepository();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            uidActual = user.getUid();
            tvNombre.setText(user.getDisplayName() != null ? user.getDisplayName() : "Usuario");
            tvEmail.setText(user.getEmail());
            cargarTransporteGuardado();
        }

        btnGuardar.setOnClickListener(v -> guardarTransporte());
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());

        return view;
    }

    private void cargarTransporteGuardado() {
        usuarioRepository.obtenerUsuario(uidActual)
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String transporte = doc.getString("transportePreferido");
                        if (transporte == null) return;
                        switch (transporte) {
                            case "pie":     radioGroup.check(R.id.radioPie);    break;
                            case "bici":    radioGroup.check(R.id.radioBici);   break;
                            case "bus":     radioGroup.check(R.id.radioBus);    break;
                            case "tranvia": radioGroup.check(R.id.radioTranvia);break;
                        }
                    }
                });
    }

    private void guardarTransporte() {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(getContext(), "Selecciona un modo de transporte", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selected = radioGroup.findViewById(selectedId);
        String transporte = selected.getTag().toString();

        usuarioRepository.actualizarTransporte(uidActual, transporte)
                .addOnSuccessListener(unused ->
                        Toast.makeText(getContext(), "Preferencia guardada ✓", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show());
    }

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getActivity(), LoginActivity.class));
        requireActivity().finish();
    }
}