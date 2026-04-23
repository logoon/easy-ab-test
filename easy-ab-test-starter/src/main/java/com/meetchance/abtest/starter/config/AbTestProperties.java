package com.meetchance.abtest.starter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "abtest")
public class AbTestProperties {
    
    private boolean enabled = true;
    
    private SyncMode syncMode = SyncMode.HTTP;
    
    private String serviceCode;
    
    private String serverUrl = "http://localhost:8080";
    
    private HttpConfig http = new HttpConfig();
    
    private WebSocketConfig websocket = new WebSocketConfig();
    
    public enum SyncMode {
        HTTP,
        WEBSOCKET
    }
    
    @Data
    public static class HttpConfig {
        private long syncIntervalSeconds = 3;
        private int connectTimeout = 5000;
        private int readTimeout = 10000;
    }
    
    @Data
    public static class WebSocketConfig {
        private String path = "/ws/config";
        private int reconnectIntervalSeconds = 5;
        private int maxReconnectAttempts = 10;
    }
}
