package com.tramites.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

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

}
