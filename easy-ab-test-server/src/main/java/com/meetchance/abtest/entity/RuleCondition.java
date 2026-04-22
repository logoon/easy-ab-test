package com.meetchance.abtest.entity;

import lombok.Data;
import java.util.List;

@Data
public class RuleCondition {
    private String fieldName;
    private RuleOperator operator;
    private String value;
    private List<String> values;
}
