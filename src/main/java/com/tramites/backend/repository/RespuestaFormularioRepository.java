package com.tramites.backend.repository;

import com.tramites.backend.model.RespuestaFormulario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RespuestaFormularioRepository extends MongoRepository<RespuestaFormulario, String> {
    List<RespuestaFormulario> findByTramiteId(String tramiteId);
    Optional<RespuestaFormulario> findByActividadId(String actividadId);
    List<RespuestaFormulario> findByFormularioPlantillaId(String formularioPlantillaId);
}
