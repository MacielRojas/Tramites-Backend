package com.tramites.backend.controller;

import com.tramites.backend.model.Departamento;
import com.tramites.backend.repository.DepartamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/departamentos")
@RequiredArgsConstructor
public class DepartamentoController {

    private final DepartamentoRepository departamentoRepository;

    @GetMapping
    public ResponseEntity<List<Departamento>> listar() {
        return ResponseEntity.ok(departamentoRepository.findByActivoTrue());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crear(@RequestBody Departamento departamento) {
        if (departamentoRepository.existsByNombre(departamento.getNombre())) {
            return ResponseEntity.badRequest().body("Ya existe un departamento con ese nombre");
        }
        departamento.setActivo(true);
        return ResponseEntity.ok(departamentoRepository.save(departamento));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody Departamento departamento) {
        return departamentoRepository.findById(id).map(dep -> {
            dep.setNombre(departamento.getNombre());
            dep.setDescripcion(departamento.getDescripcion());
            return ResponseEntity.ok(departamentoRepository.save(dep));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        departamentoRepository.findById(id).ifPresent(dep -> {
            dep.setActivo(false);
            departamentoRepository.save(dep);
        });
        return ResponseEntity.noContent().build();
    }
}
