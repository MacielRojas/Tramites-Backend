package com.tramites.backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.tramites.backend.model.PoliticaNegocio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "actividades")
public class Actividad {

    @Id
    private String id;

    private String tramiteId;

    private String nombre;

    private String descripcion;

    private String responsableId;

    private EstadoActividad estado;

    private int orden;

    private String rolRequerido;

    private String departamentoId;
    private String nombreDepartamento;
    private List<PoliticaNegocio.CampoFormulario> formularioDefinicion;
    private Map<String, Object> datosFormulario;
    private String completadoPorId;
    private String completadoPorNombre;

    private LocalDateTime fechaInicio;

    private LocalDateTime fechaFin;

    private List<Comentario> comentarios;
    private List<EntradaHistorial> historial;

    public enum EstadoActividad {
        BLOQUEADO, PENDIENTE, EN_PROCESO, COMPLETADO, OMITIDO
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Comentario {
        private String autorId;
        private String contenido;
        private LocalDateTime fecha;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EntradaHistorial {
        private String tipo;        // FORMULARIO_GUARDADO, BORRADOR, INICIADA, COMPLETADA, DOCUMENTO_SUBIDO
        private String autorId;
        private String autorNombre;
        private String descripcion;
        private LocalDateTime fecha;
    }
}
