package com.example.bismart.repositories;

import com.example.bismart.models.Usuario;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UsuarioRepository {

    private static final String COLECCION = "usuarios";
    private final FirebaseFirestore db;

    public UsuarioRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // Crear usuario al registrarse
    public Task<Void> crearUsuario(String uid, String email) {
        Usuario usuario = new Usuario(uid, email, "pie"); // transporte por defecto
        return db.collection(COLECCION)
                .document(uid)
                .set(usuario);
    }

    // Obtener usuario por uid
    public Task<DocumentSnapshot> obtenerUsuario(String uid) {
        return db.collection(COLECCION)
                .document(uid)
                .get();
    }

    // Actualizar transporte preferido
    public Task<Void> actualizarTransporte(String uid, String transporte) {
        return db.collection(COLECCION)
                .document(uid)
                .update("transportePreferido", transporte);
    }

    // Actualizar idioma
    public Task<Void> actualizarIdioma(String uid, String idioma) {
        return db.collection(COLECCION)
                .document(uid)
                .update("idioma", idioma);
    }
}