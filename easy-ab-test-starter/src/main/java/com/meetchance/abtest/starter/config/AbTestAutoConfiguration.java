package com.meetchance.abtest.starter.config;

import com.meetchance.abtest.starter.client.AbTestClient;
import com.meetchance.abtest.starter.client.DefaultAbTestClient;
import com.meetchance.abtest.starter.core.RuleMatcher;
import com.meetchance.abtest.starter.core.WeightedValueResolver;
import com.meetchance.abtest.starter.store.ConfigStore;
import com.meetchance.abtest.starter.store.InMemoryConfigStore;
import com.meetchance.abtest.starter.sync.ConfigSynchronizer;
import com.meetchance.abtest.starter.sync.HttpConfigSynchronizer;
import com.meetchance.abtest.starter.sync.WebSocketConfigSynchronizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Slf4j
@Configuration
@EnableConfigurationProperties(AbTestProperties.class)
@ConditionalOnProperty(prefix = "abtest", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AbTestAutoConfiguration {
    
    private final AbTestProperties properties;
    private ConfigSynchronizer synchronizer;
    
    @Bean
    @ConditionalOnMissingBean
    public ConfigStore configStore() {
        return new InMemoryConfigStore(properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public RuleMatcher ruleMatcher() {
        return new RuleMatcher();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public WeightedValueResolver weightedValueResolver() {
        return new WeightedValueResolver();
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "abtest", name = "sync-mode", havingValue = "HTTP", matchIfMissing = true)
    public ConfigSynchronizer httpConfigSynchronizer(ConfigStore configStore) {
        log.info("Creating HTTP config synchronizer");
        return new HttpConfigSynchronizer(properties, configStore);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "abtest", name = "sync-mode", havingValue = "WEBSOCKET")
    public ConfigSynchronizer webSocketConfigSynchronizer(ConfigStore configStore) {
        log.info("Creating WebSocket config synchronizer");
        return new WebSocketConfigSynchronizer(properties, configStore);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public AbTestClient abTestClient(ConfigStore configStore, RuleMatcher ruleMatcher, 
                                      WeightedValueResolver weightedValueResolver) {
        return new DefaultAbTestClient(configStore, ruleMatcher, weightedValueResolver);
    }
    
    @Bean
    public AbTestLifecycleManager abTestLifecycleManager(ConfigSynchronizer synchronizer) {
        this.synchronizer = synchronizer;
        return new AbTestLifecycleManager(synchronizer);
    }
    
    public static class AbTestLifecycleManager {
        
        private final ConfigSynchronizer synchronizer;
        
        public AbTestLifecycleManager(ConfigSynchronizer synchronizer) {
            this.synchronizer = synchronizer;
        }
        
        @PostConstruct
        public void start() {
            log.info("Starting AB Test synchronizer...");
            synchronizer.start();
        }
        
        @PreDestroy
        public void stop() {
            log.info("Stopping AB Test synchronizer...");
            synchronizer.stop();
        }
    }
}
