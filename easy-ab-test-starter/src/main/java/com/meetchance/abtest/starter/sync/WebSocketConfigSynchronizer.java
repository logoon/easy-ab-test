package com.meetchance.abtest.starter.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetchance.abtest.starter.config.AbTestProperties;
import com.meetchance.abtest.starter.model.ServiceConfigDTO;
import com.meetchance.abtest.starter.store.ConfigStore;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
public class WebSocketConfigSynchronizer implements ConfigSynchronizer {
    
    private final AbTestProperties properties;
    private final ConfigStore configStore;
    private final ObjectMapper objectMapper;
    private final StandardWebSocketClient webSocketClient;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private WebSocketSession session;
    private ScheduledFuture<?> reconnectFuture;
    
    public WebSocketConfigSynchronizer(AbTestProperties properties, ConfigStore configStore) {
        this.properties = properties;
        this.configStore = configStore;
        this.objectMapper = new ObjectMapper();
        this.webSocketClient = new StandardWebSocketClient();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "abtest-ws-sync");
            t.setDaemon(true);
            return t;
        });
    }
    
    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.warn("WebSocketConfigSynchronizer is already running");
            return;
        }
        
        log.info("Starting WebSocketConfigSynchronizer");
        connect();
    }
    
    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            log.warn("WebSocketConfigSynchronizer is not running");
            return;
        }
        
        log.info("Stopping WebSocketConfigSynchronizer");
        
        if (reconnectFuture != null) {
            reconnectFuture.cancel(false);
        }
        
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                log.error("Error closing WebSocket session", e);
            }
        }
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public boolean isRunning() {
        return running.get();
    }
    
    private void connect() {
        if (!running.get()) {
            return;
        }
        
        try {
            String serviceCode = configStore.getServiceCode();
            if (serviceCode == null || serviceCode.isEmpty()) {
                log.warn("Service code is not configured, cannot connect");
                return;
            }
            
            String serverUrl = properties.getServerUrl();
            String wsPath = properties.getWebsocket().getPath();
            
            String wsUrl = serverUrl.replace("http://", "ws://")
                                     .replace("https://", "wss://") 
                                     + wsPath + "?serviceCode=" + serviceCode;
            
            log.info("Connecting to WebSocket: {}", wsUrl);
            
            webSocketClient.execute(new AbTestWebSocketHandler(), wsUrl)
                .thenAccept(s -> {
                    this.session = s;
                    reconnectAttempts.set(0);
                    log.info("WebSocket connected successfully");
                })
                .exceptionally(e -> {
                    log.error("Failed to connect WebSocket", e);
                    scheduleReconnect();
                    return null;
                });
        } catch (Exception e) {
            log.error("Error connecting WebSocket", e);
            scheduleReconnect();
        }
    }
    
    private void scheduleReconnect() {
        if (!running.get() || reconnecting.get()) {
            return;
        }
        
        if (!reconnecting.compareAndSet(false, true)) {
            return;
        }
        
        int maxAttempts = properties.getWebsocket().getMaxReconnectAttempts();
        int interval = properties.getWebsocket().getReconnectIntervalSeconds();
        
        int currentAttempt = reconnectAttempts.incrementAndGet();
        
        if (maxAttempts > 0 && currentAttempt > maxAttempts) {
            log.warn("Max reconnect attempts ({}) reached, giving up", maxAttempts);
            reconnecting.set(false);
            return;
        }
        
        log.info("Scheduling reconnect attempt {} in {} seconds", currentAttempt, interval);
        
        if (reconnectFuture != null && !reconnectFuture.isDone()) {
            reconnectFuture.cancel(false);
        }
        
        reconnectFuture = scheduler.schedule(() -> {
            reconnecting.set(false);
            connect();
        }, interval, TimeUnit.SECONDS);
    }
    
    private class AbTestWebSocketHandler extends TextWebSocketHandler {
        
        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            log.info("WebSocket connection established: {}", session.getId());
            WebSocketConfigSynchronizer.this.session = session;
        }
        
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            String payload = message.getPayload();
            log.debug("Received WebSocket message: {}", payload);
            
            try {
                ServiceConfigDTO config = objectMapper.readValue(payload, ServiceConfigDTO.class);
                configStore.updateConfig(config);
            } catch (Exception e) {
                log.error("Failed to parse WebSocket message", e);
            }
        }
        
        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            log.error("WebSocket transport error", exception);
        }
        
        @Override
        public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
            log.info("WebSocket connection closed: {}, status: {}", session.getId(), status);
            WebSocketConfigSynchronizer.this.session = null;
            scheduleReconnect();
        }
    }
}
