package com.tramites.backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "departamentos")
public class Departamento {
    @Id
    private String id;
    private String nombre;
    private String descripcion;
    @Builder.Default
    private Boolean activo = true;
}
