package com.tramites.backend.controller;

import com.tramites.backend.model.Actividad;
import com.tramites.backend.repository.ActividadRepository;
import com.tramites.backend.service.ActividadService;
import com.tramites.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/actividades")
@RequiredArgsConstructor
public class ActividadController {

    private final ActividadService actividadService;
    private final UsuarioService usuarioService;
    private final ActividadRepository actividadRepository;

    @GetMapping("/tramite/{tramiteId}")
    public ResponseEntity<List<Actividad>> listarPorTramite(@PathVariable String tramiteId) {
        return ResponseEntity.ok(actividadService.listarPorTramite(tramiteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Actividad> buscarPorId(@PathVariable String id) {
        return actividadService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/iniciar")
    public ResponseEntity<?> iniciar(@PathVariable String id,
                                      @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(actividadService.iniciar(id, body.get("responsableId")));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/completar")
    public ResponseEntity<?> completar(@PathVariable String id,
                                        @RequestBody CompletarActividadRequest req) {
        try {
            return ResponseEntity.ok(actividadService.completar(id, req.comentario(), req.autorId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/omitir")
    public ResponseEntity<?> omitir(@PathVariable String id,
                                     @RequestBody OmitirActividadRequest req) {
        try {
            return ResponseEntity.ok(actividadService.omitir(id, req.motivo(), req.autorId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/comentarios")
    public ResponseEntity<?> agregarComentario(@PathVariable String id,
                                                @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(actividadService.agregarComentario(
                    id, body.get("autorId"), body.get("contenido")
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/formulario")
    public ResponseEntity<?> completarFormulario(@PathVariable String id,
                                                  @RequestBody Map<String, Object> datos,
                                                  Authentication authentication) {
        try {
            String username = authentication.getName();
            var usuario = usuarioService.buscarPorUsername(username).orElseThrow();

            Actividad actividad = actividadService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));

            actividad.setDatosFormulario(datos);
            actividad.setCompletadoPorId(usuario.getId());
            actividad.setCompletadoPorNombre(usuario.getUsername());

            // Agregar al historial
            List<com.tramites.backend.model.Actividad.EntradaHistorial> hist =
                actividad.getHistorial();
            if (hist == null) hist = new java.util.ArrayList<>();
            hist.add(com.tramites.backend.model.Actividad.EntradaHistorial.builder()
                .tipo("FORMULARIO_GUARDADO")
                .autorId(usuario.getId())
                .autorNombre(usuario.getUsername())
                .descripcion("Formulario completado con " + datos.size() + " campo(s)")
                .fecha(java.time.LocalDateTime.now())
                .build());
            actividad.setHistorial(hist);
            actividadRepository.save(actividad);

            // Use service to complete - this triggers verificarCompletitudTramite()
            return ResponseEntity.ok(actividadService.completar(id, null, usuario.getId()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<?> obtenerHistorial(@PathVariable String id) {
        return actividadService.buscarPorId(id)
            .map(a -> ResponseEntity.ok(a.getHistorial() != null ? a.getHistorial() : List.of()))
            .orElse(ResponseEntity.notFound().build());
    }

    public record CompletarActividadRequest(String comentario, String autorId) {}

    public record OmitirActividadRequest(String motivo, String autorId) {}
}
