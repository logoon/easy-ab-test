package com.meetchance.abtest.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetchance.abtest.dto.ServiceConfigDTO;
import com.meetchance.abtest.entity.Experiment;
import com.meetchance.abtest.entity.ExperimentRule;
import com.meetchance.abtest.entity.ServiceEntity;
import com.meetchance.abtest.mapper.ExperimentMapper;
import com.meetchance.abtest.mapper.ExperimentRuleMapper;
import com.meetchance.abtest.mapper.ServiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigWebSocketHandler extends TextWebSocketHandler {
    
    private final Map<String, Map<String, WebSocketSession>> serviceSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToServiceCode = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final ServiceMapper serviceMapper;
    private final ExperimentMapper experimentMapper;
    private final ExperimentRuleMapper experimentRuleMapper;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String serviceCode = extractServiceCode(session);
        
        if (serviceCode == null || serviceCode.isEmpty()) {
            log.warn("WebSocket connection rejected: serviceCode not provided, session: {}", sessionId);
            session.close(CloseStatus.BAD_DATA.withReason("serviceCode is required"));
            return;
        }
        
        sessionToServiceCode.put(sessionId, serviceCode);
        serviceSessions.computeIfAbsent(serviceCode, k -> new ConcurrentHashMap<>()).put(sessionId, session);
        log.info("WebSocket connection established: session={}, serviceCode={}", sessionId, serviceCode);
        
        pushInitialConfig(session, serviceCode);
    }
    
    private String extractServiceCode(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        
        String query = uri.getQuery();
        if (query == null) {
            return null;
        }
        
        for (String param : query.split("&")) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length == 2 && "serviceCode".equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        
        return null;
    }
    
    private void pushInitialConfig(WebSocketSession session, String serviceCode) {
        try {
            ServiceEntity service = serviceMapper.findByServiceCode(serviceCode).orElse(null);
            if (service == null) {
                log.warn("Service not found for serviceCode: {}", serviceCode);
                return;
            }
            
            List<Experiment> experiments = experimentMapper.findByServiceIdAndStatus(service.getId(), Experiment.ExperimentStatus.RUNNING);
            for (Experiment experiment : experiments) {
                List<ExperimentRule> rules = experimentRuleMapper.findByExperimentId(experiment.getId());
                for (ExperimentRule rule : rules) {
                    rule.parseJson();
                }
                experiment.setRules(rules);
                experiment.parseJson();
            }
            
            ServiceConfigDTO config = ServiceConfigDTO.fromEntities(service, experiments);
            String message = objectMapper.writeValueAsString(config);
            TextMessage textMessage = new TextMessage(message);
            
            if (session.isOpen()) {
                session.sendMessage(textMessage);
                log.info("Pushed initial config to session: {}, serviceCode: {}", session.getId(), serviceCode);
            }
        } catch (Exception e) {
            log.error("Failed to push initial config for serviceCode: {}", serviceCode, e);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        String serviceCode = sessionToServiceCode.remove(sessionId);
        
        if (serviceCode != null) {
            Map<String, WebSocketSession> sessions = serviceSessions.get(serviceCode);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    serviceSessions.remove(serviceCode);
                }
            }
        }
        
        log.info("WebSocket connection closed: session={}, serviceCode={}, status={}", sessionId, serviceCode, status);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Received message: {}", message.getPayload());
    }
    
    public void broadcastConfigChange(ServiceConfigDTO config) {
        if (config == null || config.getServiceCode() == null) {
            log.warn("Cannot broadcast config: config or serviceCode is null");
            return;
        }
        
        String serviceCode = config.getServiceCode();
        Map<String, WebSocketSession> sessions = serviceSessions.get(serviceCode);
        
        if (sessions == null || sessions.isEmpty()) {
            log.debug("No active sessions for serviceCode: {}", serviceCode);
            return;
        }
        
        try {
            String message = objectMapper.writeValueAsString(config);
            TextMessage textMessage = new TextMessage(message);
            
            sessions.values().forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                        log.info("Broadcast config change to session: {}, serviceCode: {}", session.getId(), serviceCode);
                    } catch (IOException e) {
                        log.error("Failed to send message to session: {}", session.getId(), e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Failed to broadcast config change for serviceCode: {}", serviceCode, e);
        }
    }
}
