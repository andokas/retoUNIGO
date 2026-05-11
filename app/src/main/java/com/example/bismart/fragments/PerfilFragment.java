package com.example.bismart.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.bismart.R;
import com.example.bismart.activities.LoginActivity;
import com.example.bismart.repositories.UsuarioRepository;
import com.example.bismart.utils.LocaleHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class PerfilFragment extends Fragment {

    private TextView tvNombre, tvEmail;
    private RadioGroup radioGroupTransporte, radioGroupIdioma;
    private MaterialButton btnGuardarTransporte, btnGuardarIdioma, btnCerrarSesion, btnEditarPerfil, btnGuardarEdicionPerfil, btnCancelarEdicionPerfil;
    private CardView layoutEdicionPerfil;
    private EditText editNombre, editEmail, editPassActual, editPassNueva;
    private UsuarioRepository usuarioRepository;
    private String uidActual;
    private FirebaseUser user;

    @SuppressLint("WrongViewCast")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        // Info usuario
        tvNombre = view.findViewById(R.id.tvNombreUsuario);
        tvEmail = view.findViewById(R.id.tvEmailUsuario);

        // Campos edición (ocultos al iniciar)
        layoutEdicionPerfil = view.findViewById(R.id.layoutEdicionPerfil);
        editNombre = view.findViewById(R.id.editNombre);
        editEmail = view.findViewById(R.id.editEmail);
        editPassActual = view.findViewById(R.id.editPassActual);
        editPassNueva = view.findViewById(R.id.editPassNueva);
        btnGuardarEdicionPerfil = view.findViewById(R.id.btnGuardarEdicionPerfil);
        btnCancelarEdicionPerfil = view.findViewById(R.id.btnCancelarEdicionPerfil);

        // Preferencias de transporte e idioma
        radioGroupTransporte = view.findViewById(R.id.radioGroupTransporte);
        radioGroupIdioma = view.findViewById(R.id.radioGroupIdioma);
        btnGuardarTransporte = view.findViewById(R.id.btnGuardarTransporte);
        btnGuardarIdioma = view.findViewById(R.id.btnGuardarIdioma);
        btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);
        btnEditarPerfil = view.findViewById(R.id.btnEditarPerfil);

        usuarioRepository = new UsuarioRepository();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            uidActual = user.getUid();
            actualizarTextosPantalla();
            cargarPreferenciasGuardadas();
        }

        btnGuardarTransporte.setOnClickListener(v -> guardarTransporte());
        btnGuardarIdioma.setOnClickListener(v -> guardarIdioma());
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());

        // Al pulsar "Editar perfil" se muestra la tarjeta de edición
        btnEditarPerfil.setOnClickListener(v -> {
            layoutEdicionPerfil.setVisibility(View.VISIBLE);
            if (user != null) {
                editNombre.setText(user.getDisplayName());
                editEmail.setText(user.getEmail());
                editPassActual.setText("");
                editPassNueva.setText("");
            }
        });

        // Al cancelar, se oculta
        btnCancelarEdicionPerfil.setOnClickListener(v -> {
            layoutEdicionPerfil.setVisibility(View.GONE);
        });

        // Al guardar edición
        btnGuardarEdicionPerfil.setOnClickListener(v -> {
            String nuevoNombre = editNombre.getText().toString().trim();
            String nuevoEmail = editEmail.getText().toString().trim();
            String passActual = editPassActual.getText().toString().trim();
            String passNueva = editPassNueva.getText().toString().trim();

            if (nuevoNombre.isEmpty() || nuevoEmail.isEmpty() || passActual.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.perfil_error_campos_obligatorios), Toast.LENGTH_SHORT).show();
                return;
            }

            actualizarPerfilCompleto(nuevoNombre, nuevoEmail, passActual, passNueva);
        });

        return view;
    }

    private void actualizarTextosPantalla() {
        if (user != null) {
            tvNombre.setText(user.getDisplayName() != null && !user.getDisplayName().isEmpty() ? user.getDisplayName() : getString(R.string.perfil_usuario));
            tvEmail.setText(user.getEmail());
        }
    }

    private void actualizarPerfilCompleto(String nombre, String email, String passActual, String passNueva) {
        if (user == null) return;

        // 1. RE-AUTENTICACIÓN
        com.google.firebase.auth.AuthCredential credential =
                EmailAuthProvider.getCredential(user.getEmail(), passActual);

        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Nombre en Auth
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(nombre).build();
                user.updateProfile(profileUpdates);

                // Email en Auth
                user.updateEmail(email).addOnCompleteListener(taskEmail -> {
                    if (taskEmail.isSuccessful()) {
                        // Cambia contraseña si aplica
                        if (!passNueva.isEmpty()) {
                            user.updatePassword(passNueva);
                        }
                        // Firestore (si aplica)
                        usuarioRepository.actualizarDatosPerfil(uidActual, nombre, email)
                                .addOnSuccessListener(aVoid -> {
                                    actualizarTextosPantalla();
                                    Toast.makeText(getContext(), getString(R.string.perfil_actualizado_exito), Toast.LENGTH_SHORT).show();
                                    editPassActual.setText("");
                                    editPassNueva.setText("");
                                    layoutEdicionPerfil.setVisibility(View.GONE); // Oculta tras guardar
                                });
                    } else {
                        Toast.makeText(getContext(), getString(R.string.perfil_error_email) + ": " + taskEmail.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

            } else {
                Toast.makeText(getContext(), getString(R.string.perfil_error_contrasena_incorrecta), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarPreferenciasGuardadas() {
        usuarioRepository.obtenerUsuario(uidActual)
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String transporte = doc.getString("transportePreferido");
                    if (transporte != null) {
                        switch (transporte) {
                            case "pie":     radioGroupTransporte.check(R.id.radioPie);    break;
                            case "bici":    radioGroupTransporte.check(R.id.radioBici);   break;
                            case "bus":     radioGroupTransporte.check(R.id.radioBus);    break;
                            case "metro":   radioGroupTransporte.check(R.id.radioMetro);  break;
                            case "tren":    radioGroupTransporte.check(R.id.radioTren);   break;
                            case "tranvia": radioGroupTransporte.check(R.id.radioTranvia);break;
                        }
                    }
                    String idioma = doc.getString("idioma");
                    if (idioma != null) {
                        switch (idioma) {
                            case "es": radioGroupIdioma.check(R.id.radioEspanol); break;
                            case "eu": radioGroupIdioma.check(R.id.radioEuskera); break;
                            case "en": radioGroupIdioma.check(R.id.radioIngles);  break;
                        }
                    } else {
                        radioGroupIdioma.check(R.id.radioEspanol);
                    }
                });
    }

    private void guardarTransporte() {
        int selectedId = radioGroupTransporte.getCheckedRadioButtonId();
        if (selectedId == -1) return;
        String transporte = ((RadioButton) radioGroupTransporte.findViewById(selectedId)).getTag().toString();
        usuarioRepository.actualizarTransporte(uidActual, transporte)
                .addOnSuccessListener(unused -> Toast.makeText(getContext(), getString(R.string.perfil_transporte_guardado), Toast.LENGTH_SHORT).show());
    }

    private void guardarIdioma() {
        int selectedId = radioGroupIdioma.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(getContext(), getString(R.string.perfil_selecciona_idioma), Toast.LENGTH_SHORT).show();
            return;
        }
        String idioma = ((RadioButton) radioGroupIdioma.findViewById(selectedId)).getTag().toString();

        // Guardar en Firestore
        usuarioRepository.actualizarIdioma(uidActual, idioma)
                .addOnSuccessListener(unused -> {
                    LocaleHelper.aplicarIdioma(requireContext(), idioma);
                    requireActivity().finish();
                    requireActivity().startActivity(requireActivity().getIntent());
                });
    }

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getActivity(), LoginActivity.class));
        requireActivity().finish();
    }
}