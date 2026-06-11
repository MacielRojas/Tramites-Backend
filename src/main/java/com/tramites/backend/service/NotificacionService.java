package com.tramites.backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;

@Slf4j
@Service
public class NotificacionService {

    private FirebaseMessaging messaging;

    @PostConstruct
    public void inicializar() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials;
                String credentialsJson = System.getenv("FIREBASE_CREDENTIALS_JSON");
                if (credentialsJson != null && !credentialsJson.isEmpty()) {
                    log.info("Inicializando Firebase desde variable FIREBASE_CREDENTIALS_JSON");
                    credentials = GoogleCredentials.fromStream(
                        new java.io.ByteArrayInputStream(credentialsJson.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    );
                } else {
                    log.info("Inicializando Firebase desde archivo local");
                    credentials = GoogleCredentials.fromStream(
                        new FileInputStream("src/main/resources/firebase-service-account.json")
                    );
                }
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp inicializado correctamente");
            }
            this.messaging = FirebaseMessaging.getInstance();
            log.info("FirebaseMessaging listo");
        } catch (Exception e) {
            log.error("Firebase no configurado: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    public void enviarNotificacion(String fcmToken, String titulo, String cuerpo) {
        log.info("FCM - token: {}, titulo: {}", fcmToken, titulo);
        if (messaging == null) {
            log.error("Firebase messaging es NULL - no inicializado");
            return;
        }
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("FCM token vacío para usuario");
            return;
        }
        try {
            Message mensaje = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle(titulo)
                    .setBody(cuerpo)
                    .build())
                .build();
            messaging.send(mensaje);
        } catch (Exception e) {
            log.error("Error enviando notificacion FCM: {}", e.getMessage());
        }
    }
}
