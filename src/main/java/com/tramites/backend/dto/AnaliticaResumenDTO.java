package com.tramites.backend.dto;

import java.util.Map;

public record AnaliticaResumenDTO(
        long totalTramites,
        Map<String, Long> tramitesPorEstado,
        long totalActividades,
        Map<String, Long> actividadesPorEstado,
        Map<String, Long> actividadesPorDepartamento,
        double duracionPromedioHoras,
        long tramitesUltimos7Dias
) {}
