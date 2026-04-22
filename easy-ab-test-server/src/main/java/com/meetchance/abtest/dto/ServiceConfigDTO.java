package com.meetchance.abtest.dto;

import com.meetchance.abtest.entity.Experiment;
import com.meetchance.abtest.entity.ExperimentGroup;
import com.meetchance.abtest.entity.ExperimentRule;
import com.meetchance.abtest.entity.ReturnValue;
import com.meetchance.abtest.entity.ReturnValueMode;
import com.meetchance.abtest.entity.ReturnValueType;
import com.meetchance.abtest.entity.RuleCondition;
import com.meetchance.abtest.entity.RuleOperator;
import com.meetchance.abtest.entity.ServiceEntity;
import com.meetchance.abtest.entity.WeightedValue;
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
        private ReturnValueType returnValueType;
        private List<RuleConfigDTO> rules;
        private ReturnValueConfigDTO defaultValue;
        
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
            dto.setGroups(experiment.getGroups() != null ? 
                experiment.getGroups().stream()
                    .map(GroupConfigDTO::fromEntity)
                    .collect(Collectors.toList()) : null);
            dto.setReturnValueType(experiment.getReturnValueType());
            dto.setRules(experiment.getRules() != null ?
                experiment.getRules().stream()
                    .map(RuleConfigDTO::fromEntity)
                    .collect(Collectors.toList()) : null);
            dto.setDefaultValue(experiment.getDefaultValue() != null ?
                ReturnValueConfigDTO.fromEntity(experiment.getDefaultValue()) : null);
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
    
    @Data
    public static class RuleConfigDTO {
        private Long id;
        private Integer priority;
        private List<RuleConditionConfigDTO> conditions;
        private ReturnValueConfigDTO returnValue;
        
        public static RuleConfigDTO fromEntity(ExperimentRule rule) {
            RuleConfigDTO dto = new RuleConfigDTO();
            dto.setId(rule.getId());
            dto.setPriority(rule.getPriority());
            dto.setConditions(rule.getConditions() != null ?
                rule.getConditions().stream()
                    .map(RuleConditionConfigDTO::fromEntity)
                    .collect(Collectors.toList()) : null);
            dto.setReturnValue(rule.getReturnValue() != null ?
                ReturnValueConfigDTO.fromEntity(rule.getReturnValue()) : null);
            return dto;
        }
    }
    
    @Data
    public static class RuleConditionConfigDTO {
        private String fieldName;
        private RuleOperator operator;
        private String value;
        private List<String> values;
        
        public static RuleConditionConfigDTO fromEntity(RuleCondition condition) {
            RuleConditionConfigDTO dto = new RuleConditionConfigDTO();
            dto.setFieldName(condition.getFieldName());
            dto.setOperator(condition.getOperator());
            dto.setValue(condition.getValue());
            dto.setValues(condition.getValues());
            return dto;
        }
    }
    
    @Data
    public static class ReturnValueConfigDTO {
        private ReturnValueMode mode;
        private String fixedValue;
        private List<WeightedValueConfigDTO> weightedValues;
        
        public static ReturnValueConfigDTO fromEntity(ReturnValue returnValue) {
            ReturnValueConfigDTO dto = new ReturnValueConfigDTO();
            dto.setMode(returnValue.getMode());
            dto.setFixedValue(returnValue.getFixedValue());
            dto.setWeightedValues(returnValue.getWeightedValues() != null ?
                returnValue.getWeightedValues().stream()
                    .map(WeightedValueConfigDTO::fromEntity)
                    .collect(Collectors.toList()) : null);
            return dto;
        }
    }
    
    @Data
    public static class WeightedValueConfigDTO {
        private java.math.BigDecimal weight;
        private String value;
        
        public static WeightedValueConfigDTO fromEntity(WeightedValue weightedValue) {
            WeightedValueConfigDTO dto = new WeightedValueConfigDTO();
            dto.setWeight(weightedValue.getWeight());
            dto.setValue(weightedValue.getValue());
            return dto;
        }
    }
}
