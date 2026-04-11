package com.notio.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${notio.firebase.service-account-path:}")
    private String serviceAccountPath;

    @Value("${notio.firebase.enabled:false}")
    private boolean firebaseEnabled;

    @PostConstruct
    public void initialize() {
        if (!firebaseEnabled) {
            logger.info("Firebase is disabled. Push notifications will not be sent.");
            return;
        }

        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            logger.warn("Firebase service account path is not configured. Push notifications will not be sent.");
            return;
        }

        try {
            final FileInputStream serviceAccount = new FileInputStream(serviceAccountPath);
            final FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("Firebase Admin SDK initialized successfully");
            }
        } catch (IOException exception) {
            logger.error("Failed to initialize Firebase Admin SDK", exception);
        }
    }
}
