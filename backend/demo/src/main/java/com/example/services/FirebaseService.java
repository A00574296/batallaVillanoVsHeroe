package com.example.services;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;

@Service
public class FirebaseService {

    private final Firestore db;

    public FirebaseService(Firestore db) {
        this.db = db;
    }

    public String guardarDato() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("mensaje", "Conexion exitosa con Firestore");
            data.put("estado", "ok");

            ApiFuture<WriteResult> future = db.collection("test")
                    .document("doc1")
                    .set(data);

            WriteResult result = future.get();
            return "Datos guardados correctamente at: " + result.getUpdateTime();

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
            return "Error al guardar datos: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error al guardar datos";
        }
    }
}
