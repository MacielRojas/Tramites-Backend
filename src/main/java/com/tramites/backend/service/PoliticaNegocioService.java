package com.tramites.backend.service;

import com.tramites.backend.model.PoliticaNegocio;
import com.tramites.backend.repository.PoliticaNegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PoliticaNegocioService {

    private final PoliticaNegocioRepository politicaNegocioRepository;

    public PoliticaNegocio crear(PoliticaNegocio politica) {
        if (politicaNegocioRepository.existsByNombre(politica.getNombre())) {
            throw new IllegalArgumentException("Ya existe una política con el nombre: " + politica.getNombre());
        }
        politica.setFechaCreacion(LocalDateTime.now());
        politica.setFechaActualizacion(LocalDateTime.now());
        politica.setActiva(true);
        return politicaNegocioRepository.save(politica);
    }

    public List<PoliticaNegocio> listarTodas() {
        return politicaNegocioRepository.findAll();
    }

    public List<PoliticaNegocio> listarActivas() {
        return politicaNegocioRepository.findByActivaTrue();
    }

    public Optional<PoliticaNegocio> buscarPorId(String id) {
        return politicaNegocioRepository.findById(id);
    }

    public PoliticaNegocio actualizar(String id, PoliticaNegocio datosActualizados) {
        PoliticaNegocio existente = politicaNegocioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Política no encontrada: " + id));

        existente.setNombre(datosActualizados.getNombre());
        existente.setDescripcion(datosActualizados.getDescripcion());
        existente.setPasos(datosActualizados.getPasos());
        existente.setActiva(datosActualizados.isActiva());
        existente.setDiagramJson(datosActualizados.getDiagramJson());
        existente.setDiagramXml(datosActualizados.getDiagramXml());
        existente.setLinkCompartido(datosActualizados.getLinkCompartido());
        existente.setModoCompartido(datosActualizados.getModoCompartido());
        existente.setFechaActualizacion(LocalDateTime.now());

        return politicaNegocioRepository.save(existente);
    }

    public PoliticaNegocio activar(String id) {
        PoliticaNegocio politica = politicaNegocioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Política no encontrada: " + id));
        politica.setActiva(true);
        politica.setFechaActualizacion(LocalDateTime.now());
        return politicaNegocioRepository.save(politica);
    }

    public PoliticaNegocio desactivar(String id) {
        PoliticaNegocio politica = politicaNegocioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Política no encontrada: " + id));
        politica.setActiva(false);
        politica.setFechaActualizacion(LocalDateTime.now());
        return politicaNegocioRepository.save(politica);
    }

    public Optional<PoliticaNegocio> buscarPorToken(String token) {
        return politicaNegocioRepository.findByLinkCompartido(token);
    }

    public void eliminar(String id) {
        if (!politicaNegocioRepository.existsById(id)) {
            throw new IllegalArgumentException("Política no encontrada: " + id);
        }
        politicaNegocioRepository.deleteById(id);
    }
}
