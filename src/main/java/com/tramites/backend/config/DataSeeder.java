package com.tramites.backend.config;

import com.tramites.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UsuarioService usuarioService;

    @Override
    public void run(String... args) {
        seed("admin1",       "admin1@tramiteflow.com",       "Admin1234",       Set.of("ROLE_ADMIN", "ROLE_FUNCIONARIO"));
        seed("Funcionario1", "funcionario1@tramiteflow.com", "Funcionario1234", Set.of("ROLE_FUNCIONARIO"));
        seed("Cliente1",     "cliente1@tramiteflow.com",     "Cliente1234",     Set.of("ROLE_CLIENTE"));
    }

    private void seed(String username, String email, String password, Set<String> roles) {
        try {
            usuarioService.crear(username, email, password, roles);
            log.info("Usuario creado: {} ({})", username, roles);
        } catch (IllegalArgumentException e) {
            // ya existe — no hacer nada
        }
    }
}
