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
@Document(collection = "politicas_negocio")
public class PoliticaNegocio {

    @Id
    private String id;

    private String nombre;

    private String descripcion;

    private List<PasoWorkflow> pasos;

    @Builder.Default
    private boolean activa = true;

    private String creadoPor;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    private String diagramXml;

    private String diagramJson;

    private String linkCompartido;
    private String modoCompartido;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PasoWorkflow {
        private int orden;
        private String nombre;
        private String descripcion;
        private String rolRequerido;
        private String departamentoId;
        private String nombreDepartamento;
        private boolean obligatorio;
        private List<CampoFormulario> formulario;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CampoFormulario {
        private String id;
        private String etiqueta;
        private String tipo; // TEXT, TEXTAREA, NUMBER, DATE, SELECT, CHECKBOX, FILE
        private boolean requerido;
        private List<String> opciones; // para SELECT
        private String valor; // valor por defecto
    }
}
