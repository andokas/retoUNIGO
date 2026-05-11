package com.example.bismart.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.google.firebase.auth.UserProfileChangeRequest;

public class PerfilFragment extends Fragment {

    private TextView tvNombre, tvEmail;
    private RadioGroup radioGroupTransporte, radioGroupIdioma;
    private MaterialButton btnGuardarTransporte, btnGuardarIdioma, btnCerrarSesion, btnEditarPerfil;
    private UsuarioRepository usuarioRepository;
    private String uidActual;
    private FirebaseUser user;

    @SuppressLint("WrongViewCast")
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

        //  Al hacer clic, abrimos la ventana emergente
        btnEditarPerfil.setOnClickListener(v -> mostrarDialogoEdicion());

        return view;
    }

    private void actualizarTextosPantalla() {
        if (user != null) {
            tvNombre.setText(user.getDisplayName() != null && !user.getDisplayName().isEmpty() ? user.getDisplayName() : "Usuario");
            tvEmail.setText(user.getEmail());
        }
    }


    private void mostrarDialogoEdicion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Editar Perfil");

        // Crear el contenedor
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // 1. Campo Nombre
        final EditText etNombre = new EditText(requireContext());
        etNombre.setHint("Nuevo Nombre de Usuario");
        etNombre.setText(user.getDisplayName());
        layout.addView(etNombre);

        // 2. Campo Correo
        final EditText etEmail = new EditText(requireContext());
        etEmail.setHint("Nuevo Correo Electrónico");
        etEmail.setText(user.getEmail());
        layout.addView(etEmail);

        // 3. Campo Contraseña ACTUAL (Obligatorio para validar cambios)
        final EditText etPassActual = new EditText(requireContext());
        etPassActual.setHint("Contraseña Actual");
        etPassActual.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etPassActual);

        // 4. Campo Nueva Contraseña (Opcional)
        final EditText etPassNueva = new EditText(requireContext());
        etPassNueva.setHint("Nueva Contraseña");
        etPassNueva.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etPassNueva);

        builder.setView(layout);

        builder.setPositiveButton("Guardar Cambios", (dialog, which) -> {
            String nuevoNombre = etNombre.getText().toString().trim();
            String nuevoEmail = etEmail.getText().toString().trim();
            String passActual = etPassActual.getText().toString().trim();
            String passNueva = etPassNueva.getText().toString().trim();

            if (nuevoNombre.isEmpty() || nuevoEmail.isEmpty() || passActual.isEmpty()) {
                Toast.makeText(getContext(), "Rellena los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            // Llamamos a la lógica de actualización con re-autenticación
            actualizarPerfilCompleto(nuevoNombre, nuevoEmail, passActual, passNueva);
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void actualizarPerfilCompleto(String nombre, String email, String passActual, String passNueva) {
        if (user == null) return;

        // 1. RE-AUTENTICACIÓN: Validamos la contraseña actual
        com.google.firebase.auth.AuthCredential credential =
                com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(), passActual);

        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Si la clave actual es correcta, empezamos las actualizaciones:

                // A. Cambiar Nombre en el Perfil de Auth
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(nombre).build();
                user.updateProfile(profileUpdates);

                // B. Cambiar Email en Auth
                user.updateEmail(email).addOnCompleteListener(taskEmail -> {
                    if (taskEmail.isSuccessful()) {

                        // C. Cambiar Contraseña en Auth (si ha escrito una nueva)
                        if (!passNueva.isEmpty()) {
                            user.updatePassword(passNueva);
                        }

                        // D. Guardar en Firestore (para que los datos coincidan)
                        usuarioRepository.actualizarDatosPerfil(uidActual, nombre, email)
                                .addOnSuccessListener(aVoid -> {
                                    actualizarTextosPantalla(); // Refrescar los TextViews
                                    Toast.makeText(getContext(), "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(getContext(), "Error al cambiar email: " + taskEmail.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

            } else {
                Toast.makeText(getContext(), "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show();
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
                            case "metro":     radioGroupTransporte.check(R.id.radioMetro);    break;
                            case "tren":     radioGroupTransporte.check(R.id.radioTren);    break;
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
        usuarioRepository.actualizarTransporte(uidActual, transporte).addOnSuccessListener(unused -> Toast.makeText(getContext(), "Transporte guardado ✓", Toast.LENGTH_SHORT).show());
    }

    private void guardarIdioma() {
        int selectedId = radioGroupIdioma.getCheckedRadioButtonId();
        if (selectedId == -1) return;
        String idioma = ((RadioButton) radioGroupIdioma.findViewById(selectedId)).getTag().toString();
        usuarioRepository.actualizarIdioma(uidActual, idioma).addOnSuccessListener(unused -> Toast.makeText(getContext(), "Idioma guardado ✓", Toast.LENGTH_SHORT).show());
    }

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getActivity(), LoginActivity.class));
        requireActivity().finish();
    }
}