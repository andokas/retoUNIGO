package com.example.bismart.repositories;

import com.example.bismart.models.CentroUniversitario;
import com.example.bismart.models.Usuario;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
// import com.google.firebase.firestore.SetOptions;

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

    // Obtener favoritos del usuario
    public Task<QuerySnapshot> obtenerFavoritos(String uid) {
        return db.collection(COLECCION)
                .document(uid)
                .collection("favoritos")
                .get();
    }

    // Añadir favorito
    public Task<Void> añadirFavorito(String uid, CentroUniversitario centro) {
        return db.collection(COLECCION)
                .document(uid)
                .collection("favoritos")
                .document(centro.nombre)
                .set(centro);
    }

    // Eliminar favorito
    public Task<Void> eliminarFavorito(String uid, String nombreCentro) {
        return db.collection(COLECCION)
                .document(uid)
                .collection("favoritos")
                .document(nombreCentro)
                .delete();
    }


    public com.google.android.gms.tasks.Task<Void> actualizarDatosPerfil(String documentId, String nuevoNombre, String nuevoEmail) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("nombre", nuevoNombre);
        updates.put("email", nuevoEmail);



        return db.collection(COLECCION)
                .document(documentId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge());
    }
}