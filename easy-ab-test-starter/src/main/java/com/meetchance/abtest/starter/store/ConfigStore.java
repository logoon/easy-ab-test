package com.meetchance.abtest.starter.store;

import com.meetchance.abtest.starter.model.ServiceConfigDTO;

public interface ConfigStore {
    
    ServiceConfigDTO getConfig();
    
    void updateConfig(ServiceConfigDTO config);
    
    String getServiceCode();
}
