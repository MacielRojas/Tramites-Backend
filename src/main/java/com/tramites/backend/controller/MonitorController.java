package com.tramites.backend.controller;

import com.tramites.backend.model.Actividad;
import com.tramites.backend.model.Tramite;
import com.tramites.backend.repository.ActividadRepository;
import com.tramites.backend.repository.TramiteRepository;
import com.tramites.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final ActividadRepository actividadRepository;
    private final TramiteRepository tramiteRepository;
    private final UsuarioService usuarioService;

    @GetMapping("/actividades")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listarActividades(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size) {
        List<Actividad> actividades = actividadRepository.findAll(
            PageRequest.of(page, size)).getContent();
        return ResponseEntity.ok(Map.of(
            "content", actividades,
            "totalElements", actividadRepository.count()
        ));
    }

    @GetMapping("/mis-actividades")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<?> misActividades(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size,
            Authentication authentication) {
        String username = authentication.getName();
        var usuario = usuarioService.buscarPorUsername(username).orElseThrow();
        List<Actividad> actividades;
        if (usuario.getDepartamentoId() != null) {
            String depId = usuario.getDepartamentoId();
            actividades = actividadRepository.findByDepartamentoId(depId)
                .stream()
                .filter(a -> a.getEstado() == Actividad.EstadoActividad.PENDIENTE
                          || a.getEstado() == Actividad.EstadoActividad.EN_PROCESO)
                .sorted(Comparator.comparing(Actividad::getOrden))
                .collect(java.util.stream.Collectors.toList());
        } else {
            actividades = actividadRepository.findByResponsableId(usuario.getId())
                .stream()
                .filter(a -> a.getEstado() != Actividad.EstadoActividad.BLOQUEADO)
                .collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(Map.of("content", actividades, "totalElements", actividades.size()));
    }

    @GetMapping("/tramites")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listarTramites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<Tramite> tramites = tramiteRepository.findAll(
            PageRequest.of(page, size)).getContent();
        return ResponseEntity.ok(Map.of(
            "content", tramites,
            "totalElements", tramiteRepository.count()
        ));
    }

    @GetMapping("/eventos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listarEventos(
            @RequestParam(defaultValue = "50") int limit) {

        List<Map<String, Object>> eventos = new ArrayList<>();

        // Eventos de trámites (creación y cambios de estado)
        tramiteRepository.findAll().forEach(t -> {
            if (t.getFechaInicio() != null) {
                eventos.add(buildEvento("create", t.getUsuarioSolicitanteId(),
                        "Creó trámite: " + t.getTitulo(), "Trámites", t.getFechaInicio()));
            }
            if (t.getFechaFin() != null) {
                String accion = t.getEstado() == Tramite.EstadoTramite.COMPLETADO
                        ? "Trámite completado: " + t.getTitulo()
                        : "Trámite " + t.getEstado().name().toLowerCase() + ": " + t.getTitulo();
                String tipo = t.getEstado() == Tramite.EstadoTramite.RECHAZADO ? "error" : "update";
                eventos.add(buildEvento(tipo, t.getUsuarioSolicitanteId(), accion, "Trámites", t.getFechaFin()));
            }
        });

        // Eventos del historial de actividades
        actividadRepository.findAll().forEach(a -> {
            if (a.getHistorial() == null) return;
            a.getHistorial().forEach(h -> {
                String tipo = switch (h.getTipo()) {
                    case "INICIADA"   -> "update";
                    case "COMPLETADA" -> "create";
                    default           -> "update";
                };
                String autor = h.getAutorNombre() != null ? h.getAutorNombre() : h.getAutorId();
                String accion = h.getDescripcion() + " — " + a.getNombre();
                eventos.add(buildEvento(tipo, autor, accion, "Actividades", h.getFecha()));
            });

            // Comentarios como eventos
            if (a.getComentarios() != null) {
                a.getComentarios().forEach(c -> eventos.add(
                        buildEvento("update", c.getAutorId(),
                                "Comentó en actividad: " + a.getNombre(), "Actividades", c.getFecha())));
            }
        });

        // Ordenar por fecha desc y limitar
        List<Map<String, Object>> resultado = eventos.stream()
                .filter(e -> e.get("fecha") != null)
                .sorted((a, b) -> {
                    LocalDateTime da = (LocalDateTime) a.get("fecha");
                    LocalDateTime db = (LocalDateTime) b.get("fecha");
                    return db.compareTo(da);
                })
                .limit(limit)
                .collect(Collectors.toList());

        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> stats() {
        long totalTramites   = tramiteRepository.count();
        long abiertos = tramiteRepository.findAll().stream()
                .filter(t -> t.getEstado() == Tramite.EstadoTramite.PENDIENTE
                          || t.getEstado() == Tramite.EstadoTramite.EN_PROCESO)
                .count();
        long completados = tramiteRepository.findAll().stream()
                .filter(t -> t.getEstado() == Tramite.EstadoTramite.COMPLETADO).count();
        long actividadesPendientes = actividadRepository.findAll().stream()
                .filter(a -> a.getEstado() == Actividad.EstadoActividad.PENDIENTE
                          || a.getEstado() == Actividad.EstadoActividad.EN_PROCESO)
                .count();

        return ResponseEntity.ok(Map.of(
                "totalTramites",         totalTramites,
                "tramitesAbiertos",      abiertos,
                "tramitesCompletados",   completados,
                "actividadesPendientes", actividadesPendientes
        ));
    }

    private Map<String, Object> buildEvento(String tipo, String usuario, String accion,
                                             String modulo, LocalDateTime fecha) {
        Map<String, Object> ev = new HashMap<>();
        ev.put("tipo",    tipo);
        ev.put("usuario", usuario != null ? usuario : "sistema");
        ev.put("accion",  accion);
        ev.put("modulo",  modulo);
        ev.put("fecha",   fecha);
        return ev;
    }
}
