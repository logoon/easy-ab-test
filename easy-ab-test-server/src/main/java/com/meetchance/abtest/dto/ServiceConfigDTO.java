package com.meetchance.abtest.dto;

import com.meetchance.abtest.entity.Experiment;
import com.meetchance.abtest.entity.ExperimentGroup;
import com.meetchance.abtest.entity.ServiceEntity;
import lombok.Data;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ServiceConfigDTO {
    private String serviceCode;
    private String serviceName;
    private List<ExperimentConfigDTO> experiments;
    
    public static ServiceConfigDTO fromEntities(ServiceEntity service, List<Experiment> experiments) {
        ServiceConfigDTO dto = new ServiceConfigDTO();
        dto.setServiceCode(service.getServiceCode());
        dto.setServiceName(service.getServiceName());
        dto.setExperiments(experiments.stream()
            .map(ExperimentConfigDTO::fromEntity)
            .collect(Collectors.toList()));
        return dto;
    }
    
    @Data
    public static class ExperimentConfigDTO {
        private Long experimentId;
        private String experimentName;
        private String version;
        private Experiment.SplitStrategy splitStrategy;
        private Integer percentage;
        private String userAttribute;
        private String attributeValues;
        private Experiment.ExperimentStatus status;
        private List<GroupConfigDTO> groups;
        
        public static ExperimentConfigDTO fromEntity(Experiment experiment) {
            ExperimentConfigDTO dto = new ExperimentConfigDTO();
            dto.setExperimentId(experiment.getId());
            dto.setExperimentName(experiment.getExperimentName());
            dto.setVersion(experiment.getVersion());
            dto.setSplitStrategy(experiment.getSplitStrategy());
            dto.setPercentage(experiment.getPercentage());
            dto.setUserAttribute(experiment.getUserAttribute());
            dto.setAttributeValues(experiment.getAttributeValues());
            dto.setStatus(experiment.getStatus());
            dto.setGroups(experiment.getGroups().stream()
                .map(GroupConfigDTO::fromEntity)
                .collect(Collectors.toList()));
            return dto;
        }
    }
    
    @Data
    public static class GroupConfigDTO {
        private Long groupId;
        private String groupName;
        private String groupCode;
        private Integer weight;
        private String config;
        private Boolean isControl;
        
        public static GroupConfigDTO fromEntity(ExperimentGroup group) {
            GroupConfigDTO dto = new GroupConfigDTO();
            dto.setGroupId(group.getId());
            dto.setGroupName(group.getGroupName());
            dto.setGroupCode(group.getGroupCode());
            dto.setWeight(group.getWeight());
            dto.setConfig(group.getConfig());
            dto.setIsControl(group.getIsControl());
            return dto;
        }
    }
}
