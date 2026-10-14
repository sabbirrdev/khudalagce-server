package com.company.efood.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initializeFirebase() {
        try {
            // 1. Look for the file inside the src/main/resources folder
            ClassPathResource resource = new ClassPathResource("msrcommerce-firebase-serviceAccountKey.json");

            if (!resource.exists()) {
                log.error("❌ [Firebase] Configuration file 'msrcommerce-firebase-serviceAccountKey.json' is missing from src/main/resources/");
                return;
            }

            try (InputStream serviceAccount = resource.getInputStream()) {
                // 2. Build configuration choices using the Service Account JSON stream
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                // 3. Prevent duplicate initialization errors during hot reloads
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                    log.info("🚀 [Firebase] Admin SDK has been successfully initialized for MSRCommerce!");
                } else {
                    log.info("ℹ️ [Firebase] App already initialized, skipping configuration.");
                }
            }
        } catch (IOException e) {
            log.error("❌ [Firebase] Exception encountered during initialization: {}", e.getMessage(), e);
        }
    }
}

