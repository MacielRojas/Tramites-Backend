package com.tramites.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/colaboracion/documento")
@RequiredArgsConstructor
public class DocumentoColaboracionController {

    private final SimpMessagingTemplate messagingTemplate;

    // Mapa de colaboradores activos: key = documentoId, value = (username, timestamp)
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>>
        colaboradoresActivos = new ConcurrentHashMap<>();

    @GetMapping("/{id}/colaboradores")
    public ResponseEntity<?> getColaboradores(@PathVariable String id) {
        return ResponseEntity.ok(
            colaboradoresActivos.getOrDefault(id, new ConcurrentHashMap<>()).keySet()
        );
    }

    @MessageMapping("/documento/{documentoId}/unirse")
    public void unirse(@DestinationVariable String documentoId,
                       @Payload Map<String, String> payload,
                       SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null
            ? headerAccessor.getUser().getName() : payload.get("username");
        if (username == null) return;

        colaboradoresActivos
            .computeIfAbsent(documentoId, k -> new ConcurrentHashMap<>())
            .put(username, LocalDateTime.now().toString());

        log.info("Usuario {} se unió a la co-edición del documento {}", username, documentoId);

        Map<String, Object> msg = new HashMap<>();
        msg.put("tipo", "USUARIO_UNIDO");
        msg.put("username", username);
        msg.put("colaboradores", colaboradoresActivos.get(documentoId).keySet());
        
        messagingTemplate.convertAndSend(
            "/topic/documento/" + documentoId, (Object) msg);
    }

    @MessageMapping("/documento/{documentoId}/operacion")
    public void enviarOperacion(@DestinationVariable String documentoId,
                                @Payload Map<String, Object> operacion,
                                SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null
            ? headerAccessor.getUser().getName() : (String) operacion.get("username");

        Map<String, Object> msg = new HashMap<>(operacion);
        msg.put("tipo", "OPERACION_EDICION");
        msg.put("autor", username);
        msg.put("timestamp", LocalDateTime.now().toString());

        // Reenviar a todos los demás clientes suscritos a este documento
        messagingTemplate.convertAndSend(
            "/topic/documento/" + documentoId, (Object) msg);
    }

    @MessageMapping("/documento/{documentoId}/salir")
    public void salir(@DestinationVariable String documentoId,
                      @Payload Map<String, String> payload,
                      SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null
            ? headerAccessor.getUser().getName() : payload.get("username");
        if (username == null) return;

        if (colaboradoresActivos.containsKey(documentoId)) {
            colaboradoresActivos.get(documentoId).remove(username);
        }

        log.info("Usuario {} salió de la co-edición del documento {}", username, documentoId);

        Map<String, Object> msg = new HashMap<>();
        msg.put("tipo", "USUARIO_SALIO");
        msg.put("username", username);
        msg.put("colaboradores",
            colaboradoresActivos.getOrDefault(documentoId, new ConcurrentHashMap<>()).keySet());
            
        messagingTemplate.convertAndSend(
            "/topic/documento/" + documentoId, (Object) msg);
    }
}
