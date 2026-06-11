package com.tramites.backend.service;

import com.tramites.backend.model.Documento;
import com.tramites.backend.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final GCSService gcsService;
    private final DocumentoRepository documentoRepository;

    public Documento subirDocumento(MultipartFile file, String subidoPor,
                                    String politicaId, String tramiteId, String actividadId) throws IOException {
        String carpeta = resolverCarpeta(politicaId, tramiteId, actividadId);
        GCSService.UploadResult result = gcsService.uploadFile(file, carpeta);

        Documento doc = new Documento();
        doc.setNombre(file.getOriginalFilename());
        doc.setTipo(file.getContentType());
        doc.setUrl(result.url());
        doc.setGcsPath(result.gcsPath());
        doc.setSubidoPor(subidoPor);
        doc.setPoliticaId(politicaId);
        doc.setTramiteId(tramiteId);
        doc.setActividadId(actividadId);
        doc.setSize(file.getSize());
        doc.setFechaSubida(LocalDateTime.now());

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
        gcsService.deleteFile(doc.getGcsPath());
        documentoRepository.deleteById(id);
    }

    public Documento buscarPorId(String id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado: " + id));
    }

    private String resolverCarpeta(String politicaId, String tramiteId, String actividadId) {
        if (tramiteId != null) return "tramites/" + tramiteId;
        if (actividadId != null) return "actividades/" + actividadId;
        if (politicaId != null) return "politicas/" + politicaId;
        return "general";
    }
}
