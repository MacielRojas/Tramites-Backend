package com.tramites.backend.service;

import com.tramites.backend.model.Actividad;
import com.tramites.backend.model.PoliticaNegocio;
import com.tramites.backend.model.Tramite;
import com.tramites.backend.repository.ActividadRepository;
import com.tramites.backend.repository.PoliticaNegocioRepository;
import com.tramites.backend.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TramiteService {

    private final TramiteRepository tramiteRepository;
    private final ActividadRepository actividadRepository;
    private final PoliticaNegocioRepository politicaNegocioRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public Tramite iniciar(String titulo, String descripcion, String politicaId,
                           String usuarioSolicitanteId, Map<String, Object> datos) {


        PoliticaNegocio politica = politicaNegocioRepository.findById(politicaId)
                .orElseThrow(() -> new IllegalArgumentException("Política no encontrada: " + politicaId));



        if (!politica.isActiva()) {
            throw new IllegalStateException("La política no está activa: " + politicaId);
        }

        Tramite tramite = Tramite.builder()
                .titulo(titulo)
                .descripcion(descripcion)
                .politicaId(politicaId)
                .usuarioSolicitanteId(usuarioSolicitanteId)
                .estado(Tramite.EstadoTramite.PENDIENTE)
                .datos(datos)
                .actividadesIds(new ArrayList<>())
                .fechaInicio(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        tramite = tramiteRepository.save(tramite);

        List<String> actividadesIds = new ArrayList<>();
        if (politica.getPasos() != null) {
            List<PoliticaNegocio.PasoWorkflow> pasosOrdenados = politica.getPasos()
                .stream()
                .sorted(java.util.Comparator.comparingInt(PoliticaNegocio.PasoWorkflow::getOrden))
                .collect(java.util.stream.Collectors.toList());
            for (PoliticaNegocio.PasoWorkflow paso : pasosOrdenados) {
                Actividad.EstadoActividad estadoInicial = paso.getOrden() <= 2
                        ? Actividad.EstadoActividad.PENDIENTE
                        : Actividad.EstadoActividad.BLOQUEADO;
                Actividad actividad = Actividad.builder()
                        .tramiteId(tramite.getId())
                        .nombre(paso.getNombre())
                        .descripcion(paso.getDescripcion())
                        .estado(estadoInicial)
                        .orden(paso.getOrden())
                        .rolRequerido(paso.getRolRequerido())
                        .departamentoId(paso.getDepartamentoId())
                        .nombreDepartamento(paso.getNombreDepartamento())
                        .formularioDefinicion(paso.getFormulario())
                        .comentarios(new ArrayList<>())
                        .build();

                actividad = actividadRepository.save(actividad);
                actividadesIds.add(actividad.getId());
            }
        }


        if (!actividadesIds.isEmpty()) {

            actividadRepository.findByTramiteIdOrderByOrdenAsc(tramite.getId())
                .stream()
                .filter(a -> a.getOrden() == 1)
                .findFirst()
                .ifPresent(primera -> {
                    primera.setDatosFormulario(datos != null ? datos : new java.util.HashMap<>());
                    primera.setEstado(Actividad.EstadoActividad.COMPLETADO);
                    primera.setFechaInicio(LocalDateTime.now());
                    primera.setFechaFin(LocalDateTime.now());
                    primera.setCompletadoPorId(usuarioSolicitanteId);
                    actividadRepository.save(primera);
                });
        }

        tramite.setActividadesIds(actividadesIds);
        tramite = tramiteRepository.save(tramite);

        notificar("/topic/tramites", "TRAMITE_INICIADO", tramite.getId());
        return tramite;
    }

    public List<Tramite> listarTodos() {
        return tramiteRepository.findAll();
    }

    public List<Tramite> listarPorUsuario(String usuarioId) {
        return tramiteRepository.findByUsuarioSolicitanteId(usuarioId);
    }

    public List<Tramite> listarPorEstado(Tramite.EstadoTramite estado) {
        return tramiteRepository.findByEstado(estado);
    }

    public Optional<Tramite> buscarPorId(String id) {
        return tramiteRepository.findById(id);
    }

    public Tramite asignar(String tramiteId, String usuarioAsignadoId) {
        Tramite tramite = tramiteRepository.findById(tramiteId)
                .orElseThrow(() -> new IllegalArgumentException("Trámite no encontrado: " + tramiteId));

        tramite.setUsuarioAsignadoId(usuarioAsignadoId);
        tramite.setEstado(Tramite.EstadoTramite.EN_PROCESO);
        tramite.setFechaActualizacion(LocalDateTime.now());

        tramite = tramiteRepository.save(tramite);
        notificar("/topic/tramites/" + tramiteId, "TRAMITE_ASIGNADO", tramiteId);
        return tramite;
    }

    public Tramite cambiarEstado(String tramiteId, Tramite.EstadoTramite nuevoEstado, String comentario) {
        Tramite tramite = tramiteRepository.findById(tramiteId)
                .orElseThrow(() -> new IllegalArgumentException("Trámite no encontrado: " + tramiteId));

        tramite.setEstado(nuevoEstado);
        tramite.setComentarios(comentario);
        tramite.setFechaActualizacion(LocalDateTime.now());

        boolean estadoFinal = nuevoEstado == Tramite.EstadoTramite.COMPLETADO
                || nuevoEstado == Tramite.EstadoTramite.RECHAZADO
                || nuevoEstado == Tramite.EstadoTramite.CANCELADO;

        if (estadoFinal) {
            tramite.setFechaFin(LocalDateTime.now());
        }

        tramite = tramiteRepository.save(tramite);
        notificar("/topic/tramites/" + tramiteId, "ESTADO_CAMBIADO", nuevoEstado.name());
        return tramite;
    }

    public void eliminar(String id) {
        Tramite tramite = tramiteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trámite no encontrado: " + id));
        actividadRepository.deleteAllById(tramite.getActividadesIds());
        tramiteRepository.deleteById(id);
    }

    public List<Tramite> listarPorAsignado(String usuarioId) {
        return tramiteRepository.findByUsuarioAsignadoId(usuarioId);
    }

    public long contarTodos() {
        return tramiteRepository.count();
    }

    public long contarPorEstado(Tramite.EstadoTramite estado) {
        return tramiteRepository.countByEstado(estado);
    }

    private void notificar(String destino, String tipo, String payload) {
        Map<String, String> mensaje = new HashMap<>();
        mensaje.put("tipo", tipo);
        mensaje.put("payload", payload);
        messagingTemplate.convertAndSend(destino, mensaje);
    }
}
