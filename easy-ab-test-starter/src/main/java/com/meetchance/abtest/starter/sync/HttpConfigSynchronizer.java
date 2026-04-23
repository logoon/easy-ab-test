package com.meetchance.abtest.starter.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetchance.abtest.starter.config.AbTestProperties;
import com.meetchance.abtest.starter.model.ServiceConfigDTO;
import com.meetchance.abtest.starter.store.ConfigStore;
import lombok.extern.slf4j.Slf4j;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class HttpConfigSynchronizer implements ConfigSynchronizer {
    
    private final AbTestProperties properties;
    private final ConfigStore configStore;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduledFuture;
    
    public HttpConfigSynchronizer(AbTestProperties properties, ConfigStore configStore) {
        this.properties = properties;
        this.configStore = configStore;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(properties.getHttp().getConnectTimeout()))
            .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "abtest-http-sync");
            t.setDaemon(true);
            return t;
        });
    }
    
    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.warn("HttpConfigSynchronizer is already running");
            return;
        }
        
        log.info("Starting HttpConfigSynchronizer, sync interval: {} seconds", 
            properties.getHttp().getSyncIntervalSeconds());
        
        syncOnce();
        
        scheduledFuture = scheduler.scheduleAtFixedRate(
            this::syncOnce,
            properties.getHttp().getSyncIntervalSeconds(),
            properties.getHttp().getSyncIntervalSeconds(),
            TimeUnit.SECONDS
        );
    }
    
    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            log.warn("HttpConfigSynchronizer is not running");
            return;
        }
        
        log.info("Stopping HttpConfigSynchronizer");
        
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
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
    
    private void syncOnce() {
        try {
            String serviceCode = configStore.getServiceCode();
            if (serviceCode == null || serviceCode.isEmpty()) {
                log.warn("Service code is not configured, skipping sync");
                return;
            }
            
            String url = properties.getServerUrl() + "/api/sdk/config/" + serviceCode;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(properties.getHttp().getReadTimeout()))
                .header("Accept", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                ServiceConfigDTO config = objectMapper.readValue(response.body(), ServiceConfigDTO.class);
                configStore.updateConfig(config);
                log.debug("Successfully synced config from {}", url);
            } else {
                log.warn("Failed to sync config, status code: {}, response: {}", 
                    response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Error syncing config", e);
        }
    }
}
