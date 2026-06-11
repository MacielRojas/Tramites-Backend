package com.tramites.backend.controller;

import com.tramites.backend.model.Documento;
import com.tramites.backend.service.DocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;

    @GetMapping
    public ResponseEntity<List<Documento>> listarTodos(Authentication authentication) {
        return ResponseEntity.ok(documentoService.listarTodos());
    }

    @PostMapping("/upload")
    public ResponseEntity<?> subirDocumento(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String politicaId,
            @RequestParam(required = false) String tramiteId,
            @RequestParam(required = false) String actividadId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Documento doc = documentoService.subirDocumento(file, username, politicaId, tramiteId, actividadId);
            return ResponseEntity.ok(doc);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error al subir el archivo: " + e.getMessage());
        }
    }

    @GetMapping("/politica/{politicaId}")
    public ResponseEntity<List<Documento>> listarPorPolitica(@PathVariable String politicaId) {
        return ResponseEntity.ok(documentoService.listarPorPolitica(politicaId));
    }

    @GetMapping("/tramite/{tramiteId}")
    public ResponseEntity<List<Documento>> listarPorTramite(@PathVariable String tramiteId) {
        return ResponseEntity.ok(documentoService.listarPorTramite(tramiteId));
    }

    @GetMapping("/actividad/{actividadId}")
    public ResponseEntity<List<Documento>> listarPorActividad(@PathVariable String actividadId) {
        return ResponseEntity.ok(documentoService.listarPorActividad(actividadId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable String id, Authentication authentication) {
        try {
            Documento doc = documentoService.buscarPorId(id);
            String username = authentication.getName();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin && !doc.getSubidoPor().equals(username)) {
                return ResponseEntity.status(403).body("No tienes permiso para eliminar este documento");
            }

            documentoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
