package com.tramites.backend.controller;

import com.tramites.backend.model.PoliticaNegocio;
import com.tramites.backend.service.PoliticaNegocioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/politicas")
@RequiredArgsConstructor
public class PoliticaNegocioController {

    private final PoliticaNegocioService politicaNegocioService;

    @GetMapping
    public ResponseEntity<List<PoliticaNegocio>> listar(
            @RequestParam(required = false) Boolean soloActivas) {
        if (Boolean.TRUE.equals(soloActivas)) {
            return ResponseEntity.ok(politicaNegocioService.listarActivas());
        }
        return ResponseEntity.ok(politicaNegocioService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PoliticaNegocio> buscarPorId(@PathVariable String id) {
        return politicaNegocioService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('GESTOR')")
    public ResponseEntity<?> crear(@RequestBody PoliticaNegocio politica) {
        try {
            return ResponseEntity.ok(politicaNegocioService.crear(politica));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('GESTOR')")
    public ResponseEntity<?> actualizar(@PathVariable String id,
                                         @RequestBody PoliticaNegocio politica) {
        try {
            return ResponseEntity.ok(politicaNegocioService.actualizar(id, politica));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activar(@PathVariable String id) {
        try {
            return ResponseEntity.ok(politicaNegocioService.activar(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> desactivar(@PathVariable String id) {
        try {
            return ResponseEntity.ok(politicaNegocioService.desactivar(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/compartir")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> compartir(@PathVariable String id,
                                        @RequestBody Map<String, String> body) {
        String modo = body.getOrDefault("modo", "READONLY");
        return politicaNegocioService.buscarPorId(id)
            .map(p -> {
                String token = java.util.UUID.randomUUID().toString();
                p.setLinkCompartido(token);
                p.setModoCompartido(modo.toUpperCase());
                politicaNegocioService.actualizar(id, p);
                return ResponseEntity.ok(java.util.Map.of(
                    "token", token,
                    "modo", modo.toUpperCase(),
                    "url", "/politicas/ver/" + token
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/compartido/{token}")
    public ResponseEntity<?> verCompartido(@PathVariable String token) {
        return politicaNegocioService.buscarPorToken(token)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/debug/compartido")
    public ResponseEntity<?> debugCompartido() {
        return ResponseEntity.ok(
            politicaNegocioService.listarTodas().stream()
                .map(p -> java.util.Map.of(
                    "id", p.getId(),
                    "nombre", p.getNombre(),
                    "linkCompartido", p.getLinkCompartido() != null ? p.getLinkCompartido() : "NULL",
                    "modoCompartido", p.getModoCompartido() != null ? p.getModoCompartido() : "NULL"
                ))
                .collect(java.util.stream.Collectors.toList())
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        try {
            politicaNegocioService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
