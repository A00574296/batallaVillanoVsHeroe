package com.example.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

@RestController
@RequestMapping("/battles")
public class BattlesController {

    private final Firestore db;

    @Value("${firebase.default-health1:100}")
    private int defaultHealth1;

    @Value("${firebase.default-health2:100}")
    private int defaultHealth2;

    public BattlesController(Firestore db) {
        this.db = db;
    }

    // Create a new battle document named battleN with fields pokemonHealth1 and pokemonHealth2
    @PostMapping("/new")
    public ResponseEntity<?> newBattle(@RequestBody(required = false) Map<String, Integer> body) {
        int h1 = defaultHealth1;
        int h2 = defaultHealth2;
        if (body != null) {
            if (body.get("health1") != null) h1 = body.get("health1");
            if (body.get("health2") != null) h2 = body.get("health2");
        }

        try {
            ApiFuture<QuerySnapshot> future = db.collection("battles").get();
            QuerySnapshot snapshot = future.get();

            int max = 0;
            List<QueryDocumentSnapshot> docs = snapshot.getDocuments();
            for (QueryDocumentSnapshot d : docs) {
                String id = d.getId();
                if (id.startsWith("battle")) {
                    try {
                        int n = Integer.parseInt(id.substring(6));
                        if (n > max) max = n;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            int next = max + 1;
            String docId = "battle" + next;

            Map<String, Object> data = new HashMap<>();
            data.put("pokemonHealth1", h1);
            data.put("pokemonHealth2", h2);

            db.collection("battles").document(docId).set(data).get();

            Map<String, Object> resp = new HashMap<>();
            resp.put("id", docId);
            resp.put("pokemonHealth1", h1);
            resp.put("pokemonHealth2", h2);
            return ResponseEntity.ok(resp);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body("Interrupted");
        } catch (ExecutionException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // Apply damage to a pokemon inside a battle document.
    // Request body: { "target": 1 or 2, "amount": 10 }
    @PostMapping("/{battleId}/damage")
    public ResponseEntity<?> damagePokemon(@PathVariable String battleId, @RequestBody Map<String, Integer> body) {
        Integer target = body == null ? null : body.get("target");
        Integer amount = body == null ? null : body.get("amount");
        if (target == null || amount == null) {
            return ResponseEntity.badRequest().body("Missing 'target' or 'amount' in request body");
        }
        if (target != 1 && target != 2) {
            return ResponseEntity.badRequest().body("'target' must be 1 or 2");
        }

        String field = (target == 1) ? "pokemonHealth1" : "pokemonHealth2";

        try {
            // Run transaction to update atomically
            db.runTransaction(tx -> {
                DocumentReference ref = db.collection("battles").document(battleId);
                DocumentSnapshot snap;
                try {
                    snap = tx.get(ref).get();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                if (!snap.exists()) {
                    throw new IllegalArgumentException("Battle not found: " + battleId);
                }

                Long curLong = snap.getLong(field);
                long cur = curLong == null ? 0L : curLong.longValue();
                long updated = cur - amount;
                if (updated < 0) updated = 0;

                Map<String, Object> updates = new HashMap<>();
                updates.put(field, updated);
                tx.update(ref, updates);
                return updated;
            }).get();

            DocumentReference ref = db.collection("battles").document(battleId);
            DocumentSnapshot finalSnap = ref.get().get();

            Map<String, Object> resp = new HashMap<>();
            resp.put("id", battleId);
            resp.put("pokemonHealth1", finalSnap.getLong("pokemonHealth1"));
            resp.put("pokemonHealth2", finalSnap.getLong("pokemonHealth2"));
            return ResponseEntity.ok(resp);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body("Interrupted");
        } catch (ExecutionException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
