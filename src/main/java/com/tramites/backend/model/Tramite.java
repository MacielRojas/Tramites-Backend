package com.tramites.backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "tramites")
public class Tramite {

    @Id
    private String id;

    private String titulo;

    private String descripcion;

    private String politicaId;

    private String usuarioSolicitanteId;

    private String usuarioAsignadoId;

    private EstadoTramite estado;

    private List<String> actividadesIds;

    private Map<String, Object> datos;

    private String comentarios;

    private LocalDateTime fechaInicio;

    private LocalDateTime fechaFin;

    private LocalDateTime fechaActualizacion;

    public enum EstadoTramite {
        PENDIENTE, EN_PROCESO, COMPLETADO, RECHAZADO, CANCELADO
    }
}
