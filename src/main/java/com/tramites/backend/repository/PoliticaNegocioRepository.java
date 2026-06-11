package com.tramites.backend.repository;

import com.tramites.backend.model.PoliticaNegocio;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PoliticaNegocioRepository extends MongoRepository<PoliticaNegocio, String> {

    List<PoliticaNegocio> findByActivaTrue();

    List<PoliticaNegocio> findByCreadoPor(String creadoPor);

    boolean existsByNombre(String nombre);

    Optional<PoliticaNegocio> findByLinkCompartido(String linkCompartido);
}
