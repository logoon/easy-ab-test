package com.meetchance.abtest.service;

import com.meetchance.abtest.entity.ExperimentRule;
import com.meetchance.abtest.entity.ReturnValue;
import com.meetchance.abtest.entity.ReturnValueMode;
import com.meetchance.abtest.entity.RuleCondition;
import com.meetchance.abtest.entity.RuleOperator;
import com.meetchance.abtest.entity.WeightedValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
public class RuleMatchService {
    
    private final Random random = new Random();
    
    public ExperimentRule matchRule(List<ExperimentRule> rules, Map<String, Object> userAttributes) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        
        for (ExperimentRule rule : rules) {
            rule.parseJson();
            if (matchesRule(rule, userAttributes)) {
                return rule;
            }
        }
        
        return null;
    }
    
    private boolean matchesRule(ExperimentRule rule, Map<String, Object> userAttributes) {
        List<RuleCondition> conditions = rule.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        
        for (RuleCondition condition : conditions) {
            if (!matchesCondition(condition, userAttributes)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean matchesCondition(RuleCondition condition, Map<String, Object> userAttributes) {
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
    
    public String resolveReturnValue(ReturnValue returnValue) {
        if (returnValue == null) {
            return null;
        }
        
        if (returnValue.getMode() == ReturnValueMode.FIXED) {
            return returnValue.getFixedValue();
        } else if (returnValue.getMode() == ReturnValueMode.WEIGHTED) {
            return selectWeightedValue(returnValue.getWeightedValues());
        }
        
        return null;
    }
    
    private String selectWeightedValue(List<WeightedValue> weightedValues) {
        if (weightedValues == null || weightedValues.isEmpty()) {
            return null;
        }
        
        BigDecimal totalWeight = weightedValues.stream()
            .map(WeightedValue::getWeight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return weightedValues.get(0).getValue();
        }
        
        double randomValue = random.nextDouble();
        BigDecimal currentWeight = BigDecimal.ZERO;
        
        for (WeightedValue wv : weightedValues) {
            currentWeight = currentWeight.add(wv.getWeight());
            double probability = currentWeight.divide(totalWeight, 10, BigDecimal.ROUND_HALF_UP).doubleValue();
            if (randomValue < probability) {
                return wv.getValue();
            }
        }
        
        return weightedValues.get(weightedValues.size() - 1).getValue();
    }
}
