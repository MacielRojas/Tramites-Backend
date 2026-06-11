package com.tramites.backend.repository;

import com.tramites.backend.model.Tramite;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TramiteRepository extends MongoRepository<Tramite, String> {

    List<Tramite> findByUsuarioSolicitanteId(String usuarioSolicitanteId);

    List<Tramite> findByUsuarioAsignadoId(String usuarioAsignadoId);

    List<Tramite> findByEstado(Tramite.EstadoTramite estado);

    List<Tramite> findByPoliticaId(String politicaId);

    List<Tramite> findByUsuarioSolicitanteIdAndEstado(String usuarioSolicitanteId, Tramite.EstadoTramite estado);

    long countByEstado(Tramite.EstadoTramite estado);
}
