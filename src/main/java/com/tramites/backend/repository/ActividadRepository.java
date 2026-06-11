package com.tramites.backend.repository;

import com.tramites.backend.model.Actividad;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActividadRepository extends MongoRepository<Actividad, String> {

    List<Actividad> findByTramiteId(String tramiteId);

    List<Actividad> findByResponsableId(String responsableId);

    List<Actividad> findByResponsableIdOrderByOrdenAsc(String responsableId);

    List<Actividad> findByEstado(Actividad.EstadoActividad estado);

    List<Actividad> findByTramiteIdOrderByOrdenAsc(String tramiteId);

    List<Actividad> findByTramiteIdAndEstado(String tramiteId, Actividad.EstadoActividad estado);

    List<Actividad> findByDepartamentoIdAndEstado(String departamentoId, Actividad.EstadoActividad estado);

    List<Actividad> findByDepartamentoId(String departamentoId);
}
