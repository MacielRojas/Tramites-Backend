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

import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
}
