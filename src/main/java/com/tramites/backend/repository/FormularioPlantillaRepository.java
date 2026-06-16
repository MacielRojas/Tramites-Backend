package com.tramites.backend.repository;

import com.tramites.backend.model.FormularioPlantilla;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FormularioPlantillaRepository extends MongoRepository<FormularioPlantilla, String> {
    Optional<FormularioPlantilla> findByPoliticaId(String politicaId);
    List<FormularioPlantilla> findAllByOrderByFechaCreacionDesc();
    boolean existsByPoliticaId(String politicaId);
}
