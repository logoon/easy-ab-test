package com.meetchance.abtest.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Data
@Slf4j
public class ExperimentRule {
    private Long id;
    private Long experimentId;
    private Integer priority;
    private String conditionsJson;
    private String returnValueJson;
    
    @JsonIgnore
    private List<RuleCondition> conditions;
    
    @JsonIgnore
    private ReturnValue returnValue;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public void parseJson() {
        try {
            if (conditionsJson != null) {
                this.conditions = objectMapper.readValue(conditionsJson, new TypeReference<List<RuleCondition>>() {});
            }
            if (returnValueJson != null) {
                this.returnValue = objectMapper.readValue(returnValueJson, ReturnValue.class);
            }
        } catch (Exception e) {
            log.warn("Failed to parse JSON for rule: {}", id, e);
        }
    }
    
    public void toJson() {
        try {
            if (conditions != null) {
                this.conditionsJson = objectMapper.writeValueAsString(conditions);
            }
            if (returnValue != null) {
                this.returnValueJson = objectMapper.writeValueAsString(returnValue);
            }
        } catch (Exception e) {
            log.warn("Failed to convert to JSON for rule: {}", id, e);
        }
    }
}
