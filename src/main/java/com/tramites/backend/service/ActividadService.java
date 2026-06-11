package com.tramites.backend.service;

import com.tramites.backend.model.Actividad;
import com.tramites.backend.model.Tramite;
import com.tramites.backend.repository.ActividadRepository;
import com.tramites.backend.repository.TramiteRepository;
import com.tramites.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActividadService {

    private final ActividadRepository actividadRepository;
    private final TramiteRepository tramiteRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificacionService notificacionService;

    public List<Actividad> listarPorTramite(String tramiteId) {
        return actividadRepository.findByTramiteIdOrderByOrdenAsc(tramiteId);
    }

    public Optional<Actividad> buscarPorId(String id) {
        return actividadRepository.findById(id);
    }

    public Actividad iniciar(String actividadId, String responsableId) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada: " + actividadId));

        if (actividad.getEstado() != Actividad.EstadoActividad.PENDIENTE) {
            throw new IllegalStateException("La actividad no está en estado PENDIENTE");
        }

        actividad.setResponsableId(responsableId);
        actividad.setEstado(Actividad.EstadoActividad.EN_PROCESO);
        actividad.setFechaInicio(LocalDateTime.now());

        agregarHistorial(actividad, "INICIADA", responsableId, responsableId, "Actividad iniciada");
        actividad = actividadRepository.save(actividad);


        tramiteRepository.findById(actividad.getTramiteId()).ifPresent(tramite -> {
            log.info("Iniciando actividad: {}, tramite estado: {}", actividadId, tramite.getEstado());
            if (tramite.getEstado() == Tramite.EstadoTramite.PENDIENTE) {
                tramite.setEstado(Tramite.EstadoTramite.EN_PROCESO);
                tramite.setFechaActualizacion(LocalDateTime.now());
                tramiteRepository.save(tramite);
                notificarTramite(tramite.getId(), "TRAMITE_EN_PROCESO", tramite.getId());
            }
            usuarioRepository.findById(tramite.getUsuarioSolicitanteId()).ifPresent(solicitante -> {
                log.info("FCM token del solicitante: {}", solicitante.getFcmToken());
                notificacionService.enviarNotificacion(
                    solicitante.getFcmToken(),
                    "Tu trámite está siendo atendido",
                    "Un funcionario comenzó a revisar tu trámite"
                );
            });
        });

        notificarTramite(actividad.getTramiteId(), "ACTIVIDAD_INICIADA", actividadId);
        return actividad;
    }

    public Actividad completar(String actividadId, String comentario, String autorId) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada: " + actividadId));

        actividad.setEstado(Actividad.EstadoActividad.COMPLETADO);
        actividad.setFechaFin(LocalDateTime.now());

        if (comentario != null && !comentario.isBlank()) {
            agregarComentarioInterno(actividad, autorId, comentario);
        }

        String autorNombre = autorId != null ? autorId : "sistema";
        agregarHistorial(actividad, "COMPLETADA", autorId, autorNombre, "Actividad completada");
        actividad = actividadRepository.save(actividad);

        if (tieneValorRechazo(actividad.getDatosFormulario())) {
            tramiteRepository.findById(actividad.getTramiteId()).ifPresent(tramite -> {
                tramite.setEstado(Tramite.EstadoTramite.RECHAZADO);
                tramite.setFechaFin(LocalDateTime.now());
                tramite.setFechaActualizacion(LocalDateTime.now());
                tramiteRepository.save(tramite);
                notificarTramite(tramite.getId(), "TRAMITE_RECHAZADO", tramite.getId());
                usuarioRepository.findById(tramite.getUsuarioSolicitanteId()).ifPresent(solicitante ->
                    notificacionService.enviarNotificacion(
                        solicitante.getFcmToken(),
                        "Trámite rechazado",
                        "Tu trámite  ha sido rechazado"
                    )
                );
            });
        } else {
            desbloquearSiguienteActividad(actividad);
            verificarCompletitudTramite(actividad.getTramiteId());
        }

        notificarTramite(actividad.getTramiteId(), "ACTIVIDAD_COMPLETADA", actividadId);
        return actividad;
    }

    private boolean tieneValorRechazo(Map<String, Object> datosFormulario) {
        if (datosFormulario == null || datosFormulario.isEmpty()) return false;
        Set<String> valoresRechazo = Set.of(
                "rechazado", "no", "cancelado"
        );
        return datosFormulario.values().stream()
                .filter(Objects::nonNull)
                .map(v -> v.toString().trim().toLowerCase())
                .anyMatch(valoresRechazo::contains);
    }

    private void desbloquearSiguienteActividad(Actividad completada) {
        int siguienteOrden = completada.getOrden() + 1;
        actividadRepository.findByTramiteIdOrderByOrdenAsc(completada.getTramiteId())
                .stream()
                .filter(a -> a.getOrden() == siguienteOrden
                          && a.getEstado() == Actividad.EstadoActividad.BLOQUEADO)
                .findFirst()
                .ifPresent(siguiente -> {
                    siguiente.setEstado(Actividad.EstadoActividad.PENDIENTE);
                    actividadRepository.save(siguiente);
                    notificarTramite(completada.getTramiteId(), "ACTIVIDAD_DESBLOQUEADA", siguiente.getId());

                    tramiteRepository.findById(completada.getTramiteId()).ifPresent(tramite ->
                        usuarioRepository.findById(tramite.getUsuarioSolicitanteId()).ifPresent(solicitante ->
                            notificacionService.enviarNotificacion(
                                solicitante.getFcmToken(),
                                "Tu trámite avanzó",
                                "Tu trámite avanzó al siguiente departamento: " + siguiente.getNombreDepartamento()
                            )
                        )
                    );

                    if (siguiente.getDepartamentoId() != null) {
                        usuarioRepository.findByDepartamentoId(siguiente.getDepartamentoId())
                            .forEach(funcionario -> notificacionService.enviarNotificacion(
                                funcionario.getFcmToken(),
                                "Nueva actividad pendiente",
                                "Tienes una nueva actividad pendiente en tu departamento"
                            ));
                    }
                });
    }

    public Actividad omitir(String actividadId, String motivo, String autorId) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada: " + actividadId));

        actividad.setEstado(Actividad.EstadoActividad.OMITIDO);
        actividad.setFechaFin(LocalDateTime.now());
        agregarComentarioInterno(actividad, autorId, "OMITIDO: " + motivo);

        actividad = actividadRepository.save(actividad);
        verificarCompletitudTramite(actividad.getTramiteId());
        return actividad;
    }

    public Actividad agregarComentario(String actividadId, String autorId, String contenido) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada: " + actividadId));

        agregarComentarioInterno(actividad, autorId, contenido);
        return actividadRepository.save(actividad);
    }

    private void agregarComentarioInterno(Actividad actividad, String autorId, String contenido) {
        List<Actividad.Comentario> comentarios = actividad.getComentarios();
        if (comentarios == null) {
            comentarios = new ArrayList<>();
        }
        comentarios.add(Actividad.Comentario.builder()
                .autorId(autorId)
                .contenido(contenido)
                .fecha(LocalDateTime.now())
                .build());
        actividad.setComentarios(comentarios);
    }

    private void verificarCompletitudTramite(String tramiteId) {
        List<Actividad> actividades = actividadRepository.findByTramiteIdOrderByOrdenAsc(tramiteId);
        if (actividades.isEmpty()) return;

        boolean todasFinalizadas = actividades.stream()
                .allMatch(a -> a.getEstado() == Actividad.EstadoActividad.COMPLETADO
                        || a.getEstado() == Actividad.EstadoActividad.OMITIDO)
                && actividades.stream()
                        .noneMatch(a -> a.getEstado() == Actividad.EstadoActividad.BLOQUEADO
                                     || a.getEstado() == Actividad.EstadoActividad.PENDIENTE
                                     || a.getEstado() == Actividad.EstadoActividad.EN_PROCESO);
        boolean hayEnProceso = actividades.stream()
                .anyMatch(a -> a.getEstado() == Actividad.EstadoActividad.EN_PROCESO
                        || a.getEstado() == Actividad.EstadoActividad.COMPLETADO);

        tramiteRepository.findById(tramiteId).ifPresent(tramite -> {
            if (todasFinalizadas) {
                tramite.setEstado(Tramite.EstadoTramite.COMPLETADO);
                tramite.setFechaFin(LocalDateTime.now());
                notificarTramite(tramiteId, "TRAMITE_COMPLETADO", tramiteId);
                usuarioRepository.findById(tramite.getUsuarioSolicitanteId()).ifPresent(solicitante ->
                    notificacionService.enviarNotificacion(
                        solicitante.getFcmToken(),
                        "Trámite completado",
                        "Tu trámite ha sido completado exitosamente"
                    )
                );
            } else if (hayEnProceso && tramite.getEstado() == Tramite.EstadoTramite.PENDIENTE) {
                tramite.setEstado(Tramite.EstadoTramite.EN_PROCESO);
                notificarTramite(tramiteId, "TRAMITE_EN_PROCESO", tramiteId);
            }
            tramite.setFechaActualizacion(LocalDateTime.now());
            tramiteRepository.save(tramite);
        });
    }

    private void notificarTramite(String tramiteId, String tipo, String payload) {
        Map<String, String> mensaje = new HashMap<>();
        mensaje.put("tipo", tipo);
        mensaje.put("payload", payload);
        messagingTemplate.convertAndSend("/topic/tramites/" + tramiteId, mensaje);
    }

    private void agregarHistorial(Actividad actividad, String tipo,
                                   String autorId, String autorNombre, String descripcion) {
        List<Actividad.EntradaHistorial> historial = actividad.getHistorial();
        if (historial == null) historial = new ArrayList<>();
        historial.add(Actividad.EntradaHistorial.builder()
                .tipo(tipo)
                .autorId(autorId)
                .autorNombre(autorNombre)
                .descripcion(descripcion)
                .fecha(LocalDateTime.now())
                .build());
        actividad.setHistorial(historial);
    }
}
