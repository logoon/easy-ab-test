package com.meetchance.abtest.starter.store;

import com.meetchance.abtest.starter.config.AbTestProperties;
import com.meetchance.abtest.starter.model.ServiceConfigDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor
public class InMemoryConfigStore implements ConfigStore {
    
    private final AtomicReference<ServiceConfigDTO> configRef = new AtomicReference<>();
    private final AbTestProperties properties;
    
    @Override
    public ServiceConfigDTO getConfig() {
        return configRef.get();
    }
    
    @Override
    public void updateConfig(ServiceConfigDTO config) {
        if (config == null) {
            log.warn("Received null config, ignoring update");
            return;
        }
        ServiceConfigDTO oldConfig = configRef.getAndSet(config);
        if (oldConfig == null) {
            log.info("Config initialized for service: {}", config.getServiceCode());
        } else {
            log.info("Config updated for service: {}", config.getServiceCode());
        }
    }
    
    @Override
    public String getServiceCode() {
        return properties.getServiceCode();
    }
}
