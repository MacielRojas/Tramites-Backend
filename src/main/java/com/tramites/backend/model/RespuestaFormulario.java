package com.tramites.backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "respuestas_formulario")
public class RespuestaFormulario {

    @Id
    private String id;

    private String formularioPlantillaId;
    private String tramiteId;
    private String actividadId;

    private String respondioPorId;
    private String respondioPorNombre;

    // campoId → valor (String | List<String> | List<Map<String,Object>> para GRID)
    private Map<String, Object> valores;

    private LocalDateTime fechaRespuesta;
}
