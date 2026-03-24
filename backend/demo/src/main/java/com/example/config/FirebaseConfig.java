package com.example.config;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account}")
    private String serviceAccountPath;

    @Value("${firebase.project-id}")
    private String projectId;

    @Bean
    public Firestore firestore() throws Exception {
        if (FirebaseApp.getApps().isEmpty()) {
            // Prefer JSON content from env var to avoid committing credentials to the repo
            String envJson = System.getenv("FIREBASE_SERVICE_ACCOUNT");
            if (envJson != null && !envJson.isBlank()) {
                try (InputStream serviceAccount = new ByteArrayInputStream(envJson.getBytes(StandardCharsets.UTF_8))) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .setProjectId(projectId)
                            .build();
                    FirebaseApp.initializeApp(options);
                }
            } else {
                ClassPathResource resource = new ClassPathResource(serviceAccountPath);
                try (InputStream serviceAccount = resource.getInputStream()) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .setProjectId(projectId)
                            .build();
                    FirebaseApp.initializeApp(options);
                }
            }
        }
        return FirestoreClient.getFirestore();
    }
}
