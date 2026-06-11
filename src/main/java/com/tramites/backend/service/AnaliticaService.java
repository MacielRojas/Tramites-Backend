package com.tramites.backend.service;

import com.tramites.backend.dto.AnaliticaResumenDTO;
import com.tramites.backend.model.Actividad;
import com.tramites.backend.model.Tramite;
import com.tramites.backend.repository.ActividadRepository;
import com.tramites.backend.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnaliticaService {

    private final TramiteRepository tramiteRepository;
    private final ActividadRepository actividadRepository;

    public AnaliticaResumenDTO obtenerResumen() {
        List<Tramite> tramites = tramiteRepository.findAll();
        List<Actividad> actividades = actividadRepository.findAll();

        long totalTramites = tramites.size();

        Map<String, Long> tramitesPorEstado = tramites.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getEstado().name(),
                        Collectors.counting()
                ));

        long totalActividades = actividades.size();

        Map<String, Long> actividadesPorEstado = actividades.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getEstado().name(),
                        Collectors.counting()
                ));

        Map<String, Long> actividadesPorDepartamento = actividades.stream()
                .filter(a -> a.getNombreDepartamento() != null
                          && !a.getNombreDepartamento().isBlank())
                .collect(Collectors.groupingBy(
                        Actividad::getNombreDepartamento,
                        Collectors.counting()
                ));

        double duracionPromedioHoras = actividades.stream()
                .filter(a -> a.getFechaInicio() != null && a.getFechaFin() != null)
                .mapToLong(a -> ChronoUnit.HOURS.between(a.getFechaInicio(), a.getFechaFin()))
                .average()
                .stream()
                .map(avg -> Math.round(avg * 10.0) / 10.0)
                .findFirst()
                .orElse(0.0);

        LocalDateTime hace7Dias = LocalDateTime.now().minusDays(7);
        long tramitesUltimos7Dias = tramites.stream()
                .filter(t -> t.getFechaInicio() != null
                          && !t.getFechaInicio().isBefore(hace7Dias))
                .count();

        return new AnaliticaResumenDTO(
                totalTramites,
                tramitesPorEstado,
                totalActividades,
                actividadesPorEstado,
                actividadesPorDepartamento,
                duracionPromedioHoras,
                tramitesUltimos7Dias
        );
    }
}
