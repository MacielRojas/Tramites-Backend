package com.tramites.backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "formularios_plantilla")
public class FormularioPlantilla {

    @Id
    private String id;

    private String nombre;
    private String descripcion;

    private String politicaId;
    private String nombrePolitica;

    private List<CampoFormulario> campos;

    private String creadoPor;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CampoFormulario {
        private String id;
        // TEXT | TEXTAREA | NUMBER | DATE | CHECKLIST | SELECTOR | RADIO | GRID
        private String tipo;
        private String etiqueta;
        private String placeholder;
        private boolean requerido;
        private int orden;
        private List<String> opciones;
        private List<ColumnaDef> columnas;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnaDef {
        private String key;
        private String label;
        // TEXT | NUMBER | DATE | SELECTOR
        private String tipo;
        private List<String> opciones;
    }
}
