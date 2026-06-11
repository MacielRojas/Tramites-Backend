package com.tramites.backend.controller;

import com.tramites.backend.service.PoliticaNegocioService;
import com.tramites.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/colaboracion")
@RequiredArgsConstructor
public class ColaboracionController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PoliticaNegocioService politicaNegocioService;
    private final UsuarioService usuarioService;


    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>>
        colaboradoresActivos = new ConcurrentHashMap<>();

    @GetMapping("/politica/{id}/info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getShareInfo(@PathVariable String id) {
        return politicaNegocioService.buscarPorId(id)
            .map(p -> ResponseEntity.ok(Map.of(
                "id", p.getId(),
                "nombre", p.getNombre(),
                "activa", p.isActiva(),
                "colaboradoresActivos",
                    colaboradoresActivos.getOrDefault(id, new ConcurrentHashMap<>()).size()
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/politica/{id}/colaboradores")
    public ResponseEntity<?> getColaboradores(@PathVariable String id) {
        return ResponseEntity.ok(
            colaboradoresActivos.getOrDefault(id, new ConcurrentHashMap<>()).keySet()
        );
    }

    @MessageMapping("/colaboracion/{politicaId}/unirse")
    public void unirse(@DestinationVariable String politicaId,
                       @Payload Map<String, String> payload,
                       SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null
            ? headerAccessor.getUser().getName() : payload.get("username");
        if (username == null) return;

        colaboradoresActivos
            .computeIfAbsent(politicaId, k -> new ConcurrentHashMap<>())
            .put(username, LocalDateTime.now().toString());

        Map<String, Object> msg = new HashMap<>();
        msg.put("tipo", "USUARIO_UNIDO");
        msg.put("username", username);
        msg.put("colaboradores",
            colaboradoresActivos.get(politicaId).keySet());
        messagingTemplate.convertAndSend(
            "/topic/colaboracion/" + politicaId, (Object) msg);
    }

    @MessageMapping("/colaboracion/{politicaId}/cambio")
    public void enviarCambio(@DestinationVariable String politicaId,
                              @Payload Map<String, Object> cambio,
                              SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null
            ? headerAccessor.getUser().getName() : (String) cambio.get("username");

        Map<String, Object> msg = new HashMap<>(cambio);
        msg.put("tipo", "CAMBIO_DIAGRAMA");
        msg.put("autor", username);
        msg.put("timestamp", LocalDateTime.now().toString());


        messagingTemplate.convertAndSend(
            "/topic/colaboracion/" + politicaId, (Object) msg);
    }

    @MessageMapping("/colaboracion/{politicaId}/guardar")
    public void guardarDiagrama(@DestinationVariable String politicaId,
                                 @Payload Map<String, Object> payload,
                                 SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null
            ? headerAccessor.getUser().getName() : (String) payload.get("username");
        String diagramJson = (String) payload.get("diagramJson");

        if (diagramJson != null) {
            politicaNegocioService.buscarPorId(politicaId).ifPresent(politica -> {
                politica.setDiagramJson(diagramJson);
                politicaNegocioService.actualizar(politicaId, politica);
            });
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("tipo", "DIAGRAMA_GUARDADO");
        msg.put("autor", username);
        msg.put("timestamp", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend(
            "/topic/colaboracion/" + politicaId, (Object) msg);
    }

    @MessageMapping("/colaboracion/{politicaId}/salir")
    public void salir(@DestinationVariable String politicaId,
                      @Payload Map<String, String> payload,
                      SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null
            ? headerAccessor.getUser().getName() : payload.get("username");
        if (username == null) return;

        if (colaboradoresActivos.containsKey(politicaId)) {
            colaboradoresActivos.get(politicaId).remove(username);
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("tipo", "USUARIO_SALIO");
        msg.put("username", username);
        msg.put("colaboradores",
            colaboradoresActivos.getOrDefault(politicaId,
                new ConcurrentHashMap<>()).keySet());
        messagingTemplate.convertAndSend(
            "/topic/colaboracion/" + politicaId, (Object) msg);
    }
}
