package com.tramites.backend.repository;

import com.tramites.backend.model.Departamento;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DepartamentoRepository extends MongoRepository<Departamento, String> {
    List<Departamento> findByActivoTrue();
    boolean existsByNombre(String nombre);
}
