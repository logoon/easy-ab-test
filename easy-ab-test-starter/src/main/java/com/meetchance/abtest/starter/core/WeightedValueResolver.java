package com.meetchance.abtest.starter.core;

import com.meetchance.abtest.starter.model.ReturnValueConfigDTO;
import com.meetchance.abtest.starter.model.ReturnValueMode;
import com.meetchance.abtest.starter.model.WeightedValueConfigDTO;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WeightedValueResolver {
    
    public String resolveReturnValue(ReturnValueConfigDTO returnValue, Map<String, Object> userAttributes) {
        if (returnValue == null) {
            return null;
        }
        
        if (returnValue.getMode() == ReturnValueMode.FIXED) {
            return returnValue.getFixedValue();
        } else if (returnValue.getMode() == ReturnValueMode.WEIGHTED) {
            return selectWeightedValue(returnValue.getWeightedValues(), userAttributes);
        }
        
        return null;
    }
    
    private String selectWeightedValue(List<WeightedValueConfigDTO> weightedValues, Map<String, Object> userAttributes) {
        if (weightedValues == null || weightedValues.isEmpty()) {
            return null;
        }
        
        if (weightedValues.size() == 1) {
            return weightedValues.get(0).getValue();
        }
        
        BigDecimal totalWeight = weightedValues.stream()
            .map(WeightedValueConfigDTO::getWeight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return weightedValues.get(0).getValue();
        }
        
        String hashInput = generateHashInput(userAttributes, weightedValues);
        double hashValue = computeDeterministicHash(hashInput);
        
        BigDecimal currentWeight = BigDecimal.ZERO;
        
        for (WeightedValueConfigDTO wv : weightedValues) {
            currentWeight = currentWeight.add(wv.getWeight());
            double probability = currentWeight.divide(totalWeight, 10, BigDecimal.ROUND_HALF_UP).doubleValue();
            if (hashValue < probability) {
                return wv.getValue();
            }
        }
        
        return weightedValues.get(weightedValues.size() - 1).getValue();
    }
    
    private String generateHashInput(Map<String, Object> userAttributes, List<WeightedValueConfigDTO> weightedValues) {
        StringBuilder sb = new StringBuilder();
        
        if (userAttributes != null && !userAttributes.isEmpty()) {
            userAttributes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
                });
        }
        
        sb.append("|");
        sb.append(weightedValues.stream()
            .map(wv -> wv.getValue() + ":" + wv.getWeight())
            .collect(Collectors.joining(",")));
        
        return sb.toString();
    }
    
    private double computeDeterministicHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            
            long hashValue = 0;
            for (int i = 0; i < 8; i++) {
                hashValue = (hashValue << 8) | (digest[i] & 0xFF);
            }
            
            return (double) (Math.abs(hashValue) % 1000000) / 1000000.0;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
}
