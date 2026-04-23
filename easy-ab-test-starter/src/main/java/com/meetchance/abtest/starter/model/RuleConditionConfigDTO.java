package com.meetchance.abtest.starter.model;

import lombok.Data;
import java.util.List;

@Data
public class RuleConditionConfigDTO {
    private String fieldName;
    private RuleOperator operator;
    private String value;
    private List<String> values;
}
