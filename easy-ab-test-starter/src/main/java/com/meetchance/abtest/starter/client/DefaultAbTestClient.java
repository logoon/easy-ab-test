package com.meetchance.abtest.starter.client;

import com.meetchance.abtest.starter.core.RuleMatcher;
import com.meetchance.abtest.starter.core.WeightedValueResolver;
import com.meetchance.abtest.starter.model.ExperimentConfigDTO;
import com.meetchance.abtest.starter.model.RuleConfigDTO;
import com.meetchance.abtest.starter.model.ServiceConfigDTO;
import com.meetchance.abtest.starter.store.ConfigStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class DefaultAbTestClient implements AbTestClient {
    
    private final ConfigStore configStore;
    private final RuleMatcher ruleMatcher;
    private final WeightedValueResolver weightedValueResolver;
    
    @Override
    public String getValue(String experimentName, Map<String, Object> userAttributes) {
        ServiceConfigDTO config = configStore.getConfig();
        
        if (config == null) {
            log.warn("Config not available for service: {}", configStore.getServiceCode());
            return null;
        }
        
        if (config.getExperiments() == null || config.getExperiments().isEmpty()) {
            log.debug("No experiments found for service: {}", configStore.getServiceCode());
            return null;
        }
        
        ExperimentConfigDTO experiment = config.getExperiments().stream()
            .filter(e -> experimentName.equals(e.getExperimentName()))
            .filter(e -> e.getStatus() == ExperimentConfigDTO.ExperimentStatus.RUNNING)
            .findFirst()
            .orElse(null);
        
        if (experiment == null) {
            log.debug("Experiment not found or not running: {}", experimentName);
            return null;
        }
        
        return resolveExperimentValue(experiment, userAttributes);
    }
    
    @Override
    public String getValueOrDefault(String experimentName, Map<String, Object> userAttributes, String defaultValue) {
        String value = getValue(experimentName, userAttributes);
        return value != null ? value : defaultValue;
    }
    
    @Override
    public boolean isExperimentRunning(String experimentName) {
        ServiceConfigDTO config = configStore.getConfig();
        
        if (config == null || config.getExperiments() == null) {
            return false;
        }
        
        return config.getExperiments().stream()
            .anyMatch(e -> experimentName.equals(e.getExperimentName()) 
                && e.getStatus() == ExperimentConfigDTO.ExperimentStatus.RUNNING);
    }
    
    private String resolveExperimentValue(ExperimentConfigDTO experiment, Map<String, Object> userAttributes) {
        RuleConfigDTO matchedRule = ruleMatcher.matchRule(experiment.getRules(), userAttributes);
        
        if (matchedRule != null && matchedRule.getReturnValue() != null) {
            return weightedValueResolver.resolveReturnValue(matchedRule.getReturnValue(), userAttributes);
        }
        
        if (experiment.getDefaultValue() != null) {
            return weightedValueResolver.resolveReturnValue(experiment.getDefaultValue(), userAttributes);
        }
        
        return null;
    }
}
