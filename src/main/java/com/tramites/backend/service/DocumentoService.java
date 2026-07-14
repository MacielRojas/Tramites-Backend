package com.tramites.backend.service;

import com.tramites.backend.model.Actividad;
import com.tramites.backend.model.Documento;
import com.tramites.backend.model.Tramite;
import com.tramites.backend.repository.ActividadRepository;
import com.tramites.backend.repository.DocumentoRepository;
import com.tramites.backend.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final S3Service s3Service;
    private final DocumentoRepository documentoRepository;
    private final TramiteRepository tramiteRepository;
    private final ActividadRepository actividadRepository;

    public Documento subirDocumento(MultipartFile file, String subidoPor,
                                    String politicaId, String tramiteId, String actividadId) {
        String carpeta = resolverCarpeta(politicaId, tramiteId, actividadId);
        S3Service.UploadResult result = s3Service.uploadFile(file, carpeta);

        Documento doc = new Documento();
        doc.setNombre(file.getOriginalFilename());
        doc.setTipo(file.getContentType());
        doc.setUrl(result.url());
        doc.setS3Key(result.s3Key());
        doc.setSubidoPor(subidoPor);
        doc.setPoliticaId(politicaId);
        doc.setTramiteId(tramiteId);
        doc.setActividadId(actividadId);
        doc.setSize(file.getSize());
        doc.setFechaSubida(LocalDateTime.now());
        doc.setVersionActual(1);

        Documento.DocumentoVersion v1 = new Documento.DocumentoVersion(
                UUID.randomUUID().toString(),
                1,
                result.url(),
                result.s3Key(),
                file.getSize(),
                subidoPor,
                LocalDateTime.now(),
                "Carga inicial"
        );
        doc.getVersiones().add(v1);

        return documentoRepository.save(doc);
    }

    public List<Documento> listarTodos() {
        return documentoRepository.findAll();
    }

    public List<Documento> listarPorPolitica(String politicaId) {
        return documentoRepository.findByPoliticaId(politicaId);
    }

    public List<Documento> listarPorTramite(String tramiteId) {
        return documentoRepository.findByTramiteId(tramiteId);
    }

    public List<Documento> listarPorActividad(String actividadId) {
        return documentoRepository.findByActividadId(actividadId);
    }

    public void eliminar(String id) {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado: " + id));
        
        // Eliminar todas las versiones de S3
        if (doc.getVersiones() != null) {
            for (Documento.DocumentoVersion v : doc.getVersiones()) {
                if (v.getS3Key() != null) {
                    s3Service.deleteFile(v.getS3Key());
                }
            }
        }
        
        // En caso de que la clave raíz no esté en la lista por algún motivo
        if (doc.getS3Key() != null) {
            s3Service.deleteFile(doc.getS3Key());
        }

        documentoRepository.deleteById(id);
    }

    public Documento buscarPorId(String id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado: " + id));
    }

    public byte[] descargarContenido(String documentId) {
        Documento doc = buscarPorId(documentId);
        return s3Service.downloadFile(doc.getS3Key());
    }

    public byte[] descargarContenidoVersion(String documentId, String versionId) {
        Documento doc = buscarPorId(documentId);
        Documento.DocumentoVersion version = doc.getVersiones().stream()
                .filter(v -> v.getId().equals(versionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Versión no encontrada: " + versionId));
        return s3Service.downloadFile(version.getS3Key());
    }

    public Documento subirNuevaVersionArchivo(String documentId, MultipartFile file, String editadoPor, String comentario) {
        Documento doc = buscarPorId(documentId);
        if (doc.getVersiones() == null) {
            doc.setVersiones(new ArrayList<>());
        }
        String carpeta = resolverCarpeta(doc.getPoliticaId(), doc.getTramiteId(), doc.getActividadId());
        
        int nuevaVersionNum = doc.getVersionActual() + 1;
        S3Service.UploadResult result = s3Service.uploadFile(file, carpeta + "/v" + nuevaVersionNum);

        Documento.DocumentoVersion nuevaVersion = new Documento.DocumentoVersion(
                UUID.randomUUID().toString(),
                nuevaVersionNum,
                result.url(),
                result.s3Key(),
                file.getSize(),
                editadoPor,
                LocalDateTime.now(),
                comentario != null && !comentario.isBlank() ? comentario : "Subida de archivo v" + nuevaVersionNum
        );

        doc.getVersiones().add(nuevaVersion);
        doc.setVersionActual(nuevaVersionNum);
        doc.setUrl(result.url());
        doc.setS3Key(result.s3Key());
        doc.setSize(file.getSize());
        doc.setFechaSubida(LocalDateTime.now());

        return documentoRepository.save(doc);
    }

    public Documento subirNuevaVersionBytes(String documentId, byte[] content, String editadoPor, String comentario) {
        Documento doc = buscarPorId(documentId);
        if (doc.getVersiones() == null) {
            doc.setVersiones(new ArrayList<>());
        }
        String carpeta = resolverCarpeta(doc.getPoliticaId(), doc.getTramiteId(), doc.getActividadId());
        
        int nuevaVersionNum = doc.getVersionActual() + 1;
        String cleanName = doc.getNombre();
        String ext = "";
        if (cleanName != null && cleanName.contains(".")) {
            ext = cleanName.substring(cleanName.lastIndexOf('.'));
            cleanName = cleanName.substring(0, cleanName.lastIndexOf('.'));
        }
        String customName = cleanName + "_v" + nuevaVersionNum + ext;

        S3Service.UploadResult result = s3Service.uploadBytes(
                content,
                doc.getTipo() != null ? doc.getTipo() : "application/octet-stream",
                customName,
                carpeta + "/v" + nuevaVersionNum
        );

        Documento.DocumentoVersion nuevaVersion = new Documento.DocumentoVersion(
                UUID.randomUUID().toString(),
                nuevaVersionNum,
                result.url(),
                result.s3Key(),
                (long) content.length,
                editadoPor,
                LocalDateTime.now(),
                comentario != null && !comentario.isBlank() ? comentario : "Edición en línea v" + nuevaVersionNum
        );

        doc.getVersiones().add(nuevaVersion);
        doc.setVersionActual(nuevaVersionNum);
        doc.setUrl(result.url());
        doc.setS3Key(result.s3Key());
        doc.setSize((long) content.length);
        doc.setFechaSubida(LocalDateTime.now());

        return documentoRepository.save(doc);
    }

    public Documento restaurarVersion(String documentId, String versionId, String editadoPor) {
        Documento doc = buscarPorId(documentId);
        if (doc.getVersiones() == null) {
            doc.setVersiones(new ArrayList<>());
        }
        Documento.DocumentoVersion target = doc.getVersiones().stream()
                .filter(v -> v.getId().equals(versionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Versión no encontrada: " + versionId));

        int nuevaVersionNum = doc.getVersionActual() + 1;
        Documento.DocumentoVersion nuevaVersion = new Documento.DocumentoVersion(
                UUID.randomUUID().toString(),
                nuevaVersionNum,
                target.getUrl(),
                target.getS3Key(),
                target.getSize(),
                editadoPor,
                LocalDateTime.now(),
                "Restaurado a la versión " + target.getVersion()
        );

        doc.getVersiones().add(nuevaVersion);
        doc.setVersionActual(nuevaVersionNum);
        doc.setUrl(target.getUrl());
        doc.setS3Key(target.getS3Key());
        doc.setSize(target.getSize());
        doc.setFechaSubida(LocalDateTime.now());

        return documentoRepository.save(doc);
    }

    public String obtenerPermisoEditor(String documentId, String username, List<String> userRoles) {
        Documento doc = buscarPorId(documentId);
        
        if (doc.getTramiteId() == null || doc.getTramiteId().isBlank()) {
            if (userRoles != null && userRoles.contains("ROLE_ADMIN")) {
                return "edit";
            }
            return "view";
        }

        try {
            Tramite tramite = tramiteRepository.findById(doc.getTramiteId()).orElse(null);
            if (tramite == null) {
                return "view";
            }
            
            if (tramite.getEstado() == Tramite.EstadoTramite.COMPLETADO || 
                tramite.getEstado() == Tramite.EstadoTramite.RECHAZADO) {
                return "view";
            }

            List<Actividad> actividades = actividadRepository.findByTramiteId(tramite.getId());
            Actividad activa = actividades.stream()
                    .filter(a -> a.getEstado() == Actividad.EstadoActividad.PENDIENTE || 
                                 a.getEstado() == Actividad.EstadoActividad.EN_PROCESO)
                    .findFirst()
                    .orElse(null);

            if (activa != null) {
                String rolRequerido = activa.getRolRequerido();
                if (rolRequerido == null || rolRequerido.isBlank()) {
                    return "edit";
                }
                
                if (userRoles != null && (userRoles.contains(rolRequerido) || userRoles.contains("ROLE_ADMIN"))) {
                    return "edit";
                }
            }
        } catch (Exception e) {
            log.error("Error resolviendo permisos de edición: {}", e.getMessage());
        }

        return "view";
    }

    private String resolverCarpeta(String politicaId, String tramiteId, String actividadId) {
        if (tramiteId != null) return "tramites/" + tramiteId;
        if (actividadId != null) return "actividades/" + actividadId;
        if (politicaId != null) return "politicas/" + politicaId;
        return "general";
    }
}
