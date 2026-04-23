package com.meetchance.abtest.starter.core;

import com.meetchance.abtest.starter.model.RuleConditionConfigDTO;
import com.meetchance.abtest.starter.model.RuleConfigDTO;
import com.meetchance.abtest.starter.model.RuleOperator;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
public class RuleMatcher {
    
    public RuleConfigDTO matchRule(List<RuleConfigDTO> rules, Map<String, Object> userAttributes) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        
        List<RuleConfigDTO> sortedRules = rules.stream()
            .sorted(Comparator.comparing(RuleConfigDTO::getPriority, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
        
        for (RuleConfigDTO rule : sortedRules) {
            if (matchesRule(rule, userAttributes)) {
                return rule;
            }
        }
        
        return null;
    }
    
    private boolean matchesRule(RuleConfigDTO rule, Map<String, Object> userAttributes) {
        List<RuleConditionConfigDTO> conditions = rule.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        
        for (RuleConditionConfigDTO condition : conditions) {
            if (!matchesCondition(condition, userAttributes)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean matchesCondition(RuleConditionConfigDTO condition, Map<String, Object> userAttributes) {
        String fieldName = condition.getFieldName();
        Object userValue = userAttributes.get(fieldName);
        
        if (userValue == null) {
            return condition.getOperator() == RuleOperator.NE;
        }
        
        RuleOperator operator = condition.getOperator();
        String conditionValue = condition.getValue();
        List<String> conditionValues = condition.getValues();
        
        return switch (operator) {
            case EQ -> equals(userValue, conditionValue);
            case NE -> !equals(userValue, conditionValue);
            case IN -> inList(userValue, conditionValues);
            case CONTAINS -> contains(userValue, conditionValue);
            case GT -> compare(userValue, conditionValue) > 0;
            case LT -> compare(userValue, conditionValue) < 0;
            case GTE -> compare(userValue, conditionValue) >= 0;
            case LTE -> compare(userValue, conditionValue) <= 0;
        };
    }
    
    private boolean equals(Object userValue, String conditionValue) {
        if (userValue == null || conditionValue == null) {
            return false;
        }
        return String.valueOf(userValue).equals(conditionValue);
    }
    
    private boolean inList(Object userValue, List<String> conditionValues) {
        if (userValue == null || conditionValues == null || conditionValues.isEmpty()) {
            return false;
        }
        String userStr = String.valueOf(userValue);
        return conditionValues.contains(userStr);
    }
    
    private boolean contains(Object userValue, String conditionValue) {
        if (userValue == null || conditionValue == null) {
            return false;
        }
        return String.valueOf(userValue).contains(conditionValue);
    }
    
    private int compare(Object userValue, String conditionValue) {
        try {
            BigDecimal userNum = new BigDecimal(String.valueOf(userValue));
            BigDecimal conditionNum = new BigDecimal(conditionValue);
            return userNum.compareTo(conditionNum);
        } catch (NumberFormatException e) {
            log.warn("Failed to compare values: userValue={}, conditionValue={}", userValue, conditionValue, e);
            return 0;
        }
    }
}
