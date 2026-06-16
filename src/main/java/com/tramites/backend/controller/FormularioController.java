package com.tramites.backend.controller;

import com.tramites.backend.model.FormularioPlantilla;
import com.tramites.backend.model.RespuestaFormulario;
import com.tramites.backend.service.FormularioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/formularios")
@RequiredArgsConstructor
public class FormularioController {

    private final FormularioService formularioService;

    // ── Plantillas ──────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<FormularioPlantilla>> listar() {
        return ResponseEntity.ok(formularioService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormularioPlantilla> buscarPorId(@PathVariable String id) {
        return formularioService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/politica/{politicaId}")
    public ResponseEntity<FormularioPlantilla> buscarPorPolitica(@PathVariable String politicaId) {
        return formularioService.buscarPorPoliticaId(politicaId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('GESTOR')")
    public ResponseEntity<?> crear(@RequestBody FormularioPlantilla plantilla,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails != null) plantilla.setCreadoPor(userDetails.getUsername());
            return ResponseEntity.ok(formularioService.crear(plantilla));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('GESTOR')")
    public ResponseEntity<?> actualizar(@PathVariable String id,
                                        @RequestBody FormularioPlantilla plantilla) {
        try {
            return ResponseEntity.ok(formularioService.actualizar(id, plantilla));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        try {
            formularioService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Respuestas ──────────────────────────────────────────────────────────────

    @PostMapping("/respuestas/{actividadId}")
    public ResponseEntity<?> guardarRespuesta(@PathVariable String actividadId,
                                              @RequestBody Map<String, Object> valores,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = userDetails != null ? userDetails.getUsername() : "anonimo";
            String userName = userId;
            RespuestaFormulario respuesta = formularioService.guardarRespuesta(
                    actividadId, userId, userName, valores);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/respuestas/actividad/{actividadId}")
    public ResponseEntity<RespuestaFormulario> getRespuestaPorActividad(@PathVariable String actividadId) {
        return formularioService.buscarRespuestaPorActividad(actividadId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/respuestas/tramite/{tramiteId}")
    public ResponseEntity<List<RespuestaFormulario>> getRespuestasPorTramite(@PathVariable String tramiteId) {
        return ResponseEntity.ok(formularioService.listarRespuestasPorTramite(tramiteId));
    }
}
