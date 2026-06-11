package com.tramites.backend.controller;

import com.tramites.backend.model.Tramite;
import com.tramites.backend.model.Usuario;
import com.tramites.backend.service.TramiteService;
import com.tramites.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tramites")
@RequiredArgsConstructor
public class TramiteController {

    private final TramiteService tramiteService;
    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('FUNCIONARIO')")
    public ResponseEntity<?> listarTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        String username = authentication.getName();
        var usuario = usuarioService.buscarPorUsername(username).orElseThrow();
        boolean isAdmin = usuario.getRoles().contains("ROLE_ADMIN");

        List<Tramite> tramites = isAdmin
                ? tramiteService.listarTodos()
                : tramiteService.listarPorAsignado(usuario.getId());

        return ResponseEntity.ok(Map.of(
                "content", tramites,
                "totalElements", tramites.size()
        ));
    }

    @GetMapping("/mis-tramites")
    public ResponseEntity<?> misTramites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Authentication authentication) {
        String username = authentication.getName();
        Usuario usuario = usuarioService.buscarPorUsername(username).orElseThrow();
        List<Tramite> tramites = tramiteService.listarPorUsuario(usuario.getId());
        return ResponseEntity.ok(Map.of("content", tramites, "totalElements", tramites.size()));
    }

    @GetMapping("/asignados")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<?> tramitesAsignados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Authentication authentication) {
        String username = authentication.getName();
        Usuario usuario = usuarioService.buscarPorUsername(username).orElseThrow();
        List<Tramite> tramites = tramiteService.listarPorAsignado(usuario.getId());
        return ResponseEntity.ok(Map.of("content", tramites, "totalElements", tramites.size()));
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> contarTramites(@RequestParam(required = false) String estado) {
        if (estado != null) {
            try {
                return ResponseEntity.ok(tramiteService.contarPorEstado(Tramite.EstadoTramite.valueOf(estado)));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.ok(0L);
            }
        }
        return ResponseEntity.ok(tramiteService.contarTodos());
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Tramite>> listarPorUsuario(@PathVariable String usuarioId) {
        return ResponseEntity.ok(tramiteService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listarPorEstado(@PathVariable String estado) {
        try {
            return ResponseEntity.ok(tramiteService.listarPorEstado(Tramite.EstadoTramite.valueOf(estado)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Estado inválido: " + estado);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tramite> buscarPorId(@PathVariable String id) {
        return tramiteService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> iniciar(@RequestBody IniciarTramiteRequest req,
                                      Authentication authentication) {
        try {
            String username = authentication.getName();
            Usuario usuario = usuarioService.buscarPorUsername(username).orElseThrow();
            return ResponseEntity.ok(tramiteService.iniciar(
                    req.titulo(), req.descripcion(), req.politicaId(),
                    usuario.getId(), req.datos()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/asignar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> asignar(@PathVariable String id,
                                      @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(tramiteService.asignar(id, body.get("usuarioAsignadoId")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FUNCIONARIO')")
    public ResponseEntity<?> cambiarEstado(@PathVariable String id,
                                            @RequestBody CambiarEstadoRequest req) {
        try {
            return ResponseEntity.ok(tramiteService.cambiarEstado(
                    id, Tramite.EstadoTramite.valueOf(req.estado()), req.comentario()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        try {
            tramiteService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    public record IniciarTramiteRequest(
            String titulo, String descripcion, String politicaId,
            String usuarioSolicitanteId, Map<String, Object> datos) {}

    public record CambiarEstadoRequest(String estado, String comentario) {}
}
