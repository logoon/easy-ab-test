package com.meetchance.abtest.service;

import com.meetchance.abtest.dto.ServiceConfigDTO;
import com.meetchance.abtest.entity.Experiment;
import com.meetchance.abtest.entity.ExperimentGroup;
import com.meetchance.abtest.entity.ServiceEntity;
import com.meetchance.abtest.mapper.ExperimentGroupMapper;
import com.meetchance.abtest.mapper.ExperimentMapper;
import com.meetchance.abtest.mapper.ServiceMapper;
import com.meetchance.abtest.websocket.ConfigWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigChangeNotifier {
    
    private final ConfigWebSocketHandler configWebSocketHandler;
    private final ServiceMapper serviceMapper;
    private final ExperimentMapper experimentMapper;
    private final ExperimentGroupMapper experimentGroupMapper;
    
    public void notifyConfigChange(Long serviceId) {
        try {
            ServiceEntity service = serviceMapper.findById(serviceId).orElse(null);
            if (service == null) {
                return;
            }
            
            List<Experiment> experiments = experimentMapper.findByServiceIdAndStatus(serviceId, Experiment.ExperimentStatus.RUNNING);
            for (Experiment experiment : experiments) {
                List<ExperimentGroup> groups = experimentGroupMapper.findByExperimentId(experiment.getId());
                experiment.setGroups(groups);
            }
            
            ServiceConfigDTO config = ServiceConfigDTO.fromEntities(service, experiments);
            configWebSocketHandler.broadcastConfigChange(config);
        } catch (Exception e) {
            // 忽略异常，不影响主要业务流程
        }
    }
}
