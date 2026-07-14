package com.tramites.backend.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class S3Service {

    @Value("${aws.access-key-id}")
    private String accessKeyId;

    @Value("${aws.secret-access-key}")
    private String secretAccessKey;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.bucket-name}")
    private String bucketName;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        try {
            if (accessKeyId == null || accessKeyId.isEmpty() || secretAccessKey == null || secretAccessKey.isEmpty()) {
                log.warn("Credenciales de AWS vacías. S3 no estará disponible.");
                this.s3Client = null;
                return;
            }
            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                    ))
                    .build();
            log.info("S3 Client inicializado correctamente en la región {}", region);
        } catch (Exception e) {
            log.warn("S3 no configurado, subida de archivos no disponible: {}", e.getMessage());
            this.s3Client = null;
        }
    }

    public UploadResult uploadFile(MultipartFile file, String carpeta) {
        if (s3Client != null) {
            try {
                String original = file.getOriginalFilename();
                String ext = (original != null && original.contains("."))
                        ? original.substring(original.lastIndexOf('.'))
                        : "";
                String s3Key = carpeta + "/" + UUID.randomUUID() + ext;

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType(file.getContentType())
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

                String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
                log.info("Archivo subido a S3: {}", url);
                return new UploadResult(url, s3Key);
            } catch (Exception e) {
                log.warn("Error subiendo a S3, usando URL local: {}", e.getMessage());
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

    public byte[] downloadFile(String s3Key) {
        if (s3Client == null) {
            log.warn("S3 no disponible para descarga: {}", s3Key);
            return new byte[0];
        }
        try {
            software.amazon.awssdk.services.s3.model.GetObjectRequest getObjectRequest = 
                    software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .build();
            return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
        } catch (Exception e) {
            log.warn("Error descargando archivo de S3: {}", e.getMessage());
            return new byte[0];
        }
    }

    public UploadResult uploadBytes(byte[] content, String contentType, String filename, String carpeta) {
        if (s3Client != null) {
            try {
                String original = filename != null ? filename : "documento.docx";
                String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
                String s3Key = carpeta + "/" + UUID.randomUUID() + ext;

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType(contentType)
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));

                String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
                log.info("Archivo de bytes subido a S3: {}", url);
                return new UploadResult(url, s3Key);
            } catch (Exception e) {
                log.warn("Error subiendo bytes a S3: {}", e.getMessage());
            }
        }
        return localFallback(carpeta, filename);
    }

    public void deleteFile(String s3Key) {
        if (s3Client == null) {
            log.warn("S3 no disponible, no se puede eliminar: {}", s3Key);
            return;
        }
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
            log.info("Archivo eliminado de S3: {}", s3Key);
        } catch (Exception e) {
            log.warn("Error eliminando archivo de S3: {}", e.getMessage());
        }
    }

    public record UploadResult(String url, String s3Key) {}
}
