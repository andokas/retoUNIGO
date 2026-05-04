package com.example.bismart.repositories;

import com.example.bismart.models.CentroUniversitario;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class CentroRepository {

    private static final String COLECCION = "centros";
    private final FirebaseFirestore db;

    public CentroRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // Obtener todos los centros
    public Task<QuerySnapshot> obtenerTodos() {
        return db.collection(COLECCION).get();
    }

    // Obtener centros filtrados por entidad (EHU, Deusto, MU)
    public Task<QuerySnapshot> obtenerPorEntidad(String entidad) {
        return db.collection(COLECCION)
                .whereEqualTo("entidad", entidad)
                .get();
    }

    // Poblar Firestore con todos los centros (llamar solo una vez)
    public void poblarCentros() {
        List<CentroUniversitario> centros = getCentrosIniciales();
        for (CentroUniversitario centro : centros) {
            db.collection(COLECCION).add(centro);
        }
    }

    private List<CentroUniversitario> getCentrosIniciales() {
        List<CentroUniversitario> list = new ArrayList<>();

        // EHU - Bilbao
        list.add(new CentroUniversitario("Escuela de Ingeniería de Bilbao", "Bilbao", "EHU", 43.2633891739913, -2.950245932245175));
        list.add(new CentroUniversitario("Facultad de Economía y Empresa (Elkano)", "Bilbao", "EHU", 43.260232396681644, -2.9331349610808672));
        list.add(new CentroUniversitario("Unidad Docente Medicina Basurto", "Bilbao", "EHU", 43.26109329480351, -2.951423391764935));
        list.add(new CentroUniversitario("Aulas de la Experiencia Bilbao", "Bilbao", "EHU", 43.25799262368508, -2.9223109542396326));
        list.add(new CentroUniversitario("Facultad de Economía y Empresa (Sarriko)", "Bilbao", "EHU", 43.27361499886447, -2.9582398880670513));


        // EHU - Leioa
        list.add(new CentroUniversitario("Facultad de Bellas Artes", "Leioa", "EHU", 43.33126514161276, -2.972694592314255));
        list.add(new CentroUniversitario("Facultad de Ciencia y Tecnología", "Leioa", "EHU", 43.330859325072495, -2.969765620218001));
        list.add(new CentroUniversitario("Facultad de Ciencias Sociales y Comunicación", "Leioa", "EHU", 43.33158132447186, -2.9669614346432085));
        list.add(new CentroUniversitario("Facultad de Derecho", "Leioa", "EHU", 43.33111307681475, -2.9652555497959407));
        list.add(new CentroUniversitario("Facultad de Educación de Bilbao", "Leioa", "EHU", 43.33319274131006, -2.971876854235524));
        list.add(new CentroUniversitario("Facultad de Medicina y Enfermería", "Leioa", "EHU", 43.329712106387554, -2.965385908476955));

        // EHU - Otros
        list.add(new CentroUniversitario("Unidad Docente Medicina Galdakao", "Galdakao", "EHU", 43.22360323049686, -2.81762629601731));
        list.add(new CentroUniversitario("Unidad Docente Medicina Cruces", "Cruces", "EHU", 43.282689847677744, -2.9844918205993176));
        list.add(new CentroUniversitario("Escuela de Ingeniería de Bilbao (Náutica)", "Portugalete", "EHU", 43.32713651163049, -3.0220451579331056));

        // Mondragon Unibertsitatea
        list.add(new CentroUniversitario("Bilbao Berrikuntza Faktoria (BBF): Facultad de Empresariales", "Bilbao", "MU", 43.26450942372405, -2.9267358076599423));
        list.add(new CentroUniversitario("Bilbao Berrikuntza Faktoria (BBF): LEINN (Liderazgo Emprendedor e Innovación)", "Bilbao", "MU", 43.26450942372405, -2.9267358076599423));
        list.add(new CentroUniversitario("As Fabrik - Escuela Politécnica Superior", "Zorrotzaurre", "MU", 43.27201450040566, -2.9639309524848643));
        list.add(new CentroUniversitario("As Fabrik - Facultad de Humanidades", "Zorrotzaurre", "MU", 43.27201450040566, -2.9639309524848643));

        // Deusto
        list.add(new CentroUniversitario("Deusto Business School", "Deusto", "Deusto", 43.27133612505671, -2.938963913758562));
        list.add(new CentroUniversitario("Facultad de Derecho", "Deusto", "Deusto", 43.2706061262747, -2.9374298573831097));
        list.add(new CentroUniversitario("Facultad de Ciencias Sociales y Humanas", "Deusto", "Deusto", 43.27060572627537, -2.937000503962367));
        list.add(new CentroUniversitario("Facultad de Ingeniería", "Deusto", "Deusto", 43.27188686903164, -2.938792219493774));
        list.add(new CentroUniversitario("Facultad de Educación y Deporte", "Deusto", "Deusto", 43.27132707850854, -2.9394760313129957));
        list.add(new CentroUniversitario("Facultad de Ciencias de la Salud", "Deusto", "Deusto", 43.27132707850854, -2.9394760313129957));
        list.add(new CentroUniversitario("Facultad de Ciencias Sociales y Comunicación", "Deusto", "Deusto", 43.27132707850854, -2.9394760313129957));

        return list;
    }
}