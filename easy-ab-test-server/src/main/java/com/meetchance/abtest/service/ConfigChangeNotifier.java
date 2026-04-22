package com.meetchance.abtest.service;

import com.meetchance.abtest.dto.ServiceConfigDTO;
import com.meetchance.abtest.entity.Experiment;
import com.meetchance.abtest.entity.ServiceEntity;
import com.meetchance.abtest.websocket.ConfigWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigChangeNotifier {
    
    private final ConfigWebSocketHandler configWebSocketHandler;
    private final ServiceService serviceService;
    private final ExperimentService experimentService;
    
    public void notifyConfigChange(Long serviceId) {
        try {
            ServiceEntity service = serviceService.getServiceById(serviceId);
            List<Experiment> experiments = experimentService.getRunningExperimentsByService(serviceId);
            
            ServiceConfigDTO config = ServiceConfigDTO.fromEntities(service, experiments);
            configWebSocketHandler.broadcastConfigChange(config);
        } catch (Exception e) {
            // 忽略异常，不影响主要业务流程
        }
    }
}
