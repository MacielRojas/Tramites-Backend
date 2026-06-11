package com.tramites.backend.controller;

import com.tramites.backend.model.Usuario;
import com.tramites.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Usuario> buscarPorId(@PathVariable String id) {
        return usuarioService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> actualizar(@PathVariable String id,
                                         @RequestBody UpdateUsuarioRequest req) {
        try {
            return ResponseEntity.ok(usuarioService.actualizar(id, req.email(), req.roles(), req.activo(), req.departamentoId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cambiarPassword(@PathVariable String id,
                                                 @RequestBody CambiarPasswordRequest req) {
        try {
            usuarioService.cambiarPassword(id, req.nuevaPassword());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        try {
            usuarioService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> contarUsuarios() {
        return ResponseEntity.ok((long) usuarioService.listarTodos().size());
    }

    @GetMapping("/me")
    public ResponseEntity<Usuario> miPerfil(Authentication authentication) {
        String username = authentication.getName();
        return usuarioService.buscarPorUsername(username)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<?> actualizarMiPerfil(
            @RequestBody ActualizarPerfilRequest req,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Usuario usuario = usuarioService.buscarPorUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            return ResponseEntity.ok(
                usuarioService.actualizar(
                    usuario.getId(),
                    req.email(),
                    usuario.getRoles(),
                    usuario.isActivo(),
                    usuario.getDepartamentoId()
                )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/me/fcm-token")
    @PatchMapping("/me/fcm-token")
    public ResponseEntity<?> actualizarFcmToken(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String token = body.getOrDefault("token", body.get("fcmToken"));
        usuarioService.actualizarFcmToken(authentication.getName(), token);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/me/password")
    public ResponseEntity<?> cambiarMiPassword(
            @RequestBody CambiarPasswordRequest req,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Usuario usuario = usuarioService.buscarPorUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            usuarioService.cambiarPassword(usuario.getId(), req.nuevaPassword());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleEstado(@PathVariable String id){
        return ResponseEntity.ok(usuarioService.toggleEstado(id));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?>toggleEstadoActivate(@PathVariable String id){
        return ResponseEntity.ok(usuarioService.toggleEstado(id));
    }


    public record UpdateUsuarioRequest(String email, Set<String> roles, boolean activo, String departamentoId) {}

    public record CambiarPasswordRequest(String nuevaPassword) {}

    public record ActualizarPerfilRequest(String email) {}
}
