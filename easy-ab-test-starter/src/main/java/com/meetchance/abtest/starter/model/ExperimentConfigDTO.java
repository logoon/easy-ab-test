package com.meetchance.abtest.starter.model;

import lombok.Data;
import java.util.List;

@Data
public class ExperimentConfigDTO {
    private Long experimentId;
    private String experimentName;
    private String version;
    private SplitStrategy splitStrategy;
    private Integer percentage;
    private String userAttribute;
    private String attributeValues;
    private ExperimentStatus status;
    private ReturnValueType returnValueType;
    private List<RuleConfigDTO> rules;
    private ReturnValueConfigDTO defaultValue;
    
    public enum SplitStrategy {
        PERCENTAGE,
        USER_ATTRIBUTE
    }
    
    public enum ExperimentStatus {
        DRAFT,
        RUNNING,
        PAUSED,
        FINISHED
    }
}
