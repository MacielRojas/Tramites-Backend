package com.tramites.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "documentos")
public class Documento {
    @Id
    private String id;
    private String nombre;
    private String tipo;
    private String url;
    private String s3Key;
    private String subidoPor;
    private String politicaId;
    private String actividadId;
    private String tramiteId;
    private Long size;
    private LocalDateTime fechaSubida;
    private int versionActual = 1;
    private List<DocumentoVersion> versiones = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentoVersion {
        private String id;
        private int version;
        private String url;
        private String s3Key;
        private Long size;
        private String editadoPor;
        private LocalDateTime fechaEdicion;
        private String comentario;
    }
}
