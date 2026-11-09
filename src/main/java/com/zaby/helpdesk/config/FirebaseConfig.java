package com.zaby.helpdesk.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${notifications.fcm.service-account-path:}")
    private String serviceAccountPath;

    @Value("${notifications.fcm.service-account-json:}")
    private String serviceAccountJson;

    @Bean
    @ConditionalOnProperty(name = "notifications.fcm.enabled", havingValue = "true")
    public FirebaseMessaging firebaseMessaging() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials = resolveCredentials();
                if (credentials == null) {
                    log.warn("FCM is enabled, but no valid service account credentials were provided via FCM_SERVICE_ACCOUNT_PATH or FCM_SERVICE_ACCOUNT_JSON. Starting without FCM.");
                    return null;
                }
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully.");
            }
            return FirebaseMessaging.getInstance();
        } catch (Exception ex) {
            log.warn("Failed to initialize Firebase Admin SDK: {}. Application will continue without mobile push notifications.", ex.getMessage());
            return null;
        }
    }

    private GoogleCredentials resolveCredentials() throws Exception {
        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            try (InputStream is = new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))) {
                return GoogleCredentials.fromStream(is);
            }
        }
        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            try (InputStream is = new FileInputStream(serviceAccountPath.trim())) {
                return GoogleCredentials.fromStream(is);
            }
        }
        return null;
    }
}
