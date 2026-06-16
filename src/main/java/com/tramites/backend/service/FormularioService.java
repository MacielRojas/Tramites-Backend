package com.tramites.backend.service;

import com.tramites.backend.model.Actividad;
import com.tramites.backend.model.FormularioPlantilla;
import com.tramites.backend.model.RespuestaFormulario;
import com.tramites.backend.repository.ActividadRepository;
import com.tramites.backend.repository.FormularioPlantillaRepository;
import com.tramites.backend.repository.PoliticaNegocioRepository;
import com.tramites.backend.repository.RespuestaFormularioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FormularioService {

    private final FormularioPlantillaRepository plantillaRepo;
    private final RespuestaFormularioRepository respuestaRepo;
    private final ActividadRepository actividadRepo;
    private final PoliticaNegocioRepository politicaRepo;

    // ── Plantilla CRUD ──────────────────────────────────────────────────────────

    public FormularioPlantilla crear(FormularioPlantilla plantilla) {
        plantilla.setFechaCreacion(LocalDateTime.now());
        plantilla.setFechaActualizacion(LocalDateTime.now());
        if (plantilla.getCampos() == null) plantilla.setCampos(new ArrayList<>());
        FormularioPlantilla saved = plantillaRepo.save(plantilla);
        // Sincronizar la política con el id de este formulario
        vincularPolitica(saved.getPoliticaId(), saved.getId());
        return saved;
    }

    public FormularioPlantilla actualizar(String id, FormularioPlantilla datos) {
        FormularioPlantilla existing = plantillaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado: " + id));

        // Si cambió la política, limpiar la anterior y vincular la nueva
        String politicaAnterior = existing.getPoliticaId();
        String politicaNueva    = datos.getPoliticaId();

        existing.setNombre(datos.getNombre());
        existing.setDescripcion(datos.getDescripcion());
        existing.setPoliticaId(politicaNueva);
        existing.setNombrePolitica(datos.getNombrePolitica());
        existing.setCampos(datos.getCampos() != null ? datos.getCampos() : new ArrayList<>());
        existing.setFechaActualizacion(LocalDateTime.now());
        FormularioPlantilla saved = plantillaRepo.save(existing);

        if (politicaAnterior != null && !politicaAnterior.equals(politicaNueva)) {
            desvincularPolitica(politicaAnterior);
        }
        vincularPolitica(politicaNueva, id);
        return saved;
    }

    public List<FormularioPlantilla> listarTodas() {
        return plantillaRepo.findAllByOrderByFechaCreacionDesc();
    }

    public Optional<FormularioPlantilla> buscarPorId(String id) {
        return plantillaRepo.findById(id);
    }

    public Optional<FormularioPlantilla> buscarPorPoliticaId(String politicaId) {
        return plantillaRepo.findByPoliticaId(politicaId);
    }

    public void eliminar(String id) {
        FormularioPlantilla existing = plantillaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado: " + id));
        desvincularPolitica(existing.getPoliticaId());
        plantillaRepo.deleteById(id);
    }

    // ── Respuestas ──────────────────────────────────────────────────────────────

    public RespuestaFormulario guardarRespuesta(String actividadId,
                                                 String usuarioId,
                                                 String usuarioNombre,
                                                 Map<String, Object> valores) {
        Actividad actividad = actividadRepo.findById(actividadId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada: " + actividadId));

        RespuestaFormulario respuesta = RespuestaFormulario.builder()
                .formularioPlantillaId(actividad.getFormularioPlantillaId())
                .tramiteId(actividad.getTramiteId())
                .actividadId(actividadId)
                .respondioPorId(usuarioId)
                .respondioPorNombre(usuarioNombre)
                .valores(valores)
                .fechaRespuesta(LocalDateTime.now())
                .build();

        respuesta = respuestaRepo.save(respuesta);

        actividad.setDatosFormulario(valores);
        actividad.setRespuestaFormularioId(respuesta.getId());
        actividadRepo.save(actividad);

        return respuesta;
    }

    public Optional<RespuestaFormulario> buscarRespuestaPorActividad(String actividadId) {
        return respuestaRepo.findByActividadId(actividadId);
    }

    public List<RespuestaFormulario> listarRespuestasPorTramite(String tramiteId) {
        return respuestaRepo.findByTramiteId(tramiteId);
    }

    // ── Helpers de sincronización ───────────────────────────────────────────────

    private void vincularPolitica(String politicaId, String formularioId) {
        if (politicaId == null || politicaId.isBlank()) return;
        politicaRepo.findById(politicaId).ifPresent(p -> {
            p.setFormularioPlantillaId(formularioId);
            politicaRepo.save(p);
        });
    }

    private void desvincularPolitica(String politicaId) {
        if (politicaId == null || politicaId.isBlank()) return;
        politicaRepo.findById(politicaId).ifPresent(p -> {
            p.setFormularioPlantillaId(null);
            politicaRepo.save(p);
        });
    }
}
