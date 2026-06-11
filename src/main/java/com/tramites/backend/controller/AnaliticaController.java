package com.tramites.backend.controller;

import com.tramites.backend.dto.AnaliticaResumenDTO;
import com.tramites.backend.service.AnaliticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analitica")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AnaliticaController {

    private final AnaliticaService analiticaService;

    @GetMapping
    public ResponseEntity<AnaliticaResumenDTO> obtenerResumen() {
        return ResponseEntity.ok(analiticaService.obtenerResumen());
    }
}
