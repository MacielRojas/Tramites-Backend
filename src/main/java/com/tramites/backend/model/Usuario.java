package com.tramites.backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String password;

    private Set<String> roles;

    private String departamentoId;
    private String nombreDepartamento;

    @Builder.Default
    private boolean activo = true;

    private LocalDateTime fechaCreacion;

    private LocalDateTime ultimoAcceso;

    private String fcmToken;
}
