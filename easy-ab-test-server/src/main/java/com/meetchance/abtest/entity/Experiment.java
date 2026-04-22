package com.meetchance.abtest.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Slf4j
public class Experiment {
    private Long id;
    private String experimentName;
    private String version;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    private SplitStrategy splitStrategy = SplitStrategy.PERCENTAGE;
    private Integer percentage;
    private String userAttribute;
    private String attributeValues;
    private List<ExperimentGroup> groups;
    private Long serviceId;
    private ExperimentStatus status = ExperimentStatus.DRAFT;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private ReturnValueType returnValueType;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String defaultValueJson;
    
    private List<ExperimentRule> rules;
    private ReturnValue defaultValue;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public void parseJson() {
        try {
            if (defaultValueJson != null) {
                this.defaultValue = objectMapper.readValue(defaultValueJson, ReturnValue.class);
            }
            if (rules != null) {
                for (ExperimentRule rule : rules) {
                    rule.parseJson();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse JSON for experiment: {}", id, e);
        }
    }
    
    public void toJson() {
        try {
            if (defaultValue != null) {
                this.defaultValueJson = objectMapper.writeValueAsString(defaultValue);
            }
            if (rules != null) {
                for (ExperimentRule rule : rules) {
                    rule.toJson();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to convert to JSON for experiment: {}", id, e);
        }
    }
    
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
