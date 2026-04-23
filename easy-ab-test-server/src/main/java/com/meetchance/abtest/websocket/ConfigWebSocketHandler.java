package com.meetchance.abtest.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetchance.abtest.dto.ServiceConfigDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigWebSocketHandler extends TextWebSocketHandler {
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("WebSocket connection established: {}", sessionId);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        log.info("WebSocket connection closed: {}", sessionId);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Received message: {}", message.getPayload());
    }
    
    public void broadcastConfigChange(ServiceConfigDTO config) {
        try {
            String message = objectMapper.writeValueAsString(config);
            TextMessage textMessage = new TextMessage(message);
            
            sessions.values().forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                        log.info("Broadcast config change to session: {}", session.getId());
                    } catch (IOException e) {
                        log.error("Failed to send message to session: {}", session.getId(), e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Failed to broadcast config change", e);
        }
    }
}
