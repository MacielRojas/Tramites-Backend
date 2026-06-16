package com.tramites.backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class GCSService {

    @Value("${gcs.bucket-name:tramites-documentos-proyecto-2026}")
    private String bucketName;

    private Storage storage;

    @PostConstruct
    public void init() {
        try {
            FileInputStream serviceAccount = new FileInputStream(
                "src/main/resources/firebase-service-account.json"
            );
            GoogleCredentials credentials = GoogleCredentials
                .fromStream(serviceAccount)
                .createScoped("https://www.googleapis.com/auth/devstorage.read_write");
            this.storage = StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .build()
                    .getService();
            log.info("GCS inicializado correctamente");
        } catch (Exception e) {
            log.warn("GCS no configurado, subida de archivos no disponible: {}", e.getMessage());
            this.storage = null;
        }
    }

    public UploadResult uploadFile(MultipartFile file, String carpeta) {
        if (storage != null) {
            try {
                String original = file.getOriginalFilename();
                String ext = (original != null && original.contains("."))
                        ? original.substring(original.lastIndexOf('.'))
                        : "";
                String gcsPath = carpeta + "/" + UUID.randomUUID() + ext;
                BlobId   blobId   = BlobId.of(bucketName, gcsPath);
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                        .setContentType(file.getContentType())
                        .build();
                storage.create(blobInfo, file.getBytes());
                String url = "https://storage.googleapis.com/" + bucketName + "/" + gcsPath;
                log.info("Archivo subido a GCS: {}", url);
                return new UploadResult(url, gcsPath);
            } catch (Exception e) {
                log.warn("Error subiendo a GCS, usando URL local: {}", e.getMessage());
            }
        }
        return localFallback(carpeta, file.getOriginalFilename());
    }

    private UploadResult localFallback(String carpeta, String fileName) {
        String name = fileName != null ? fileName : UUID.randomUUID().toString();
        String id   = UUID.randomUUID().toString();
        return new UploadResult(
            "http://localhost:8080/documentos/" + carpeta + "/" + id + "/" + name,
            carpeta + "/" + id
        );
    }

    public void deleteFile(String gcsPath) {
        if (storage == null) {
            log.warn("GCS no disponible, no se puede eliminar: {}", gcsPath);
            return;
        }
        storage.delete(BlobId.of(bucketName, gcsPath));
    }

    public record UploadResult(String url, String gcsPath) {}
}