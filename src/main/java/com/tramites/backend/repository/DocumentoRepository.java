package com.tramites.backend.repository;

import com.tramites.backend.model.Documento;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DocumentoRepository extends MongoRepository<Documento, String> {
    List<Documento> findByPoliticaId(String politicaId);
    List<Documento> findByTramiteId(String tramiteId);
    List<Documento> findByActividadId(String actividadId);
    List<Documento> findBySubidoPor(String subidoPor);
}
