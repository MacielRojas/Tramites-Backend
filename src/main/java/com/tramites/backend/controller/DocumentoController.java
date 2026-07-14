package com.tramites.backend.controller;

import com.tramites.backend.model.Documento;
import com.tramites.backend.service.DocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;

    @GetMapping
    public ResponseEntity<List<Documento>> listarTodos() {
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
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al subir el archivo: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Documento> buscarPorId(@PathVariable String id) {
        try {
            return ResponseEntity.ok(documentoService.buscarPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
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

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> descargarDocumento(@PathVariable String id) {
        try {
            Documento doc = documentoService.buscarPorId(id);
            byte[] bytes = documentoService.descargarContenido(id);
            
            String contentType = doc.getTipo() != null ? doc.getTipo() : "application/octet-stream";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getNombre() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(bytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/versions/{versionId}/download")
    public ResponseEntity<byte[]> descargarDocumentoVersion(@PathVariable String id, @PathVariable String versionId) {
        try {
            Documento doc = documentoService.buscarPorId(id);
            byte[] bytes = documentoService.descargarContenidoVersion(id, versionId);
            
            String contentType = doc.getTipo() != null ? doc.getTipo() : "application/octet-stream";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"v_" + versionId + "_" + doc.getNombre() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(bytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/upload-version")
    public ResponseEntity<?> subirNuevaVersion(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "comentario", required = false) String comentario,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Documento doc = documentoService.subirNuevaVersionArchivo(id, file, username, comentario);
            return ResponseEntity.ok(doc);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al subir la nueva versión: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/restore/{versionId}")
    public ResponseEntity<?> restaurarVersion(
            @PathVariable String id,
            @PathVariable String versionId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Documento doc = documentoService.restaurarVersion(id, versionId, username);
            return ResponseEntity.ok(doc);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al restaurar versión: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/permiso")
    public ResponseEntity<?> obtenerPermiso(
            @PathVariable String id,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            List<String> roles = authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .toList();
            String permiso = documentoService.obtenerPermisoEditor(id, username, roles);
            return ResponseEntity.ok(Map.of("permiso", permiso));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al evaluar permisos: " + e.getMessage());
        }
    }
}
