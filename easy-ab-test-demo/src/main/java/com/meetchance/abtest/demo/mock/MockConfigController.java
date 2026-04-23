package com.meetchance.abtest.demo.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetchance.abtest.starter.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@RequestMapping("/api/sdk/config")
public class MockConfigController {

    private final AtomicBoolean shouldFail = new AtomicBoolean(false);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/{serviceCode}")
    public ResponseEntity<String> getConfig(@PathVariable String serviceCode) {
        log.info("Mock config requested for service: {}", serviceCode);
        
        if (shouldFail.get()) {
            log.warn("Mock config returning failure (500)");
            return ResponseEntity.internalServerError().body("Mock server error");
        }
        
        try {
            ServiceConfigDTO config = createTestConfig(serviceCode);
            String json = objectMapper.writeValueAsString(config);
            log.info("Returning mock config: {}", json);
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            log.error("Error creating mock config", e);
            return ResponseEntity.internalServerError().body("Error");
        }
    }

    @PostMapping("/control/fail")
    public ResponseEntity<String> setFailMode(@RequestParam boolean fail) {
        shouldFail.set(fail);
        log.info("Mock config fail mode set to: {}", fail);
        return ResponseEntity.ok("Fail mode set to: " + fail);
    }

    private ServiceConfigDTO createTestConfig(String serviceCode) {
        ServiceConfigDTO config = new ServiceConfigDTO();
        config.setServiceCode(serviceCode);
        config.setServiceName("Demo Service");
        
        ExperimentConfigDTO experiment1 = createFixedValueExperiment();
        ExperimentConfigDTO experiment2 = createWeightedValueExperiment();
        ExperimentConfigDTO experiment3 = createRuleBasedExperiment();
        ExperimentConfigDTO experiment4 = createPausedExperiment();
        
        config.setExperiments(List.of(experiment1, experiment2, experiment3, experiment4));
        return config;
    }

    private ExperimentConfigDTO createFixedValueExperiment() {
        ExperimentConfigDTO experiment = new ExperimentConfigDTO();
        experiment.setExperimentId(1L);
        experiment.setExperimentName("fixed_value_exp");
        experiment.setStatus(ExperimentConfigDTO.ExperimentStatus.RUNNING);
        
        ReturnValueConfigDTO defaultReturn = new ReturnValueConfigDTO();
        defaultReturn.setMode(ReturnValueMode.FIXED);
        defaultReturn.setFixedValue("original_value");
        experiment.setDefaultValue(defaultReturn);
        
        return experiment;
    }

    private ExperimentConfigDTO createWeightedValueExperiment() {
        ExperimentConfigDTO experiment = new ExperimentConfigDTO();
        experiment.setExperimentId(2L);
        experiment.setExperimentName("weighted_value_exp");
        experiment.setStatus(ExperimentConfigDTO.ExperimentStatus.RUNNING);
        
        ReturnValueConfigDTO defaultReturn = new ReturnValueConfigDTO();
        defaultReturn.setMode(ReturnValueMode.WEIGHTED);
        
        WeightedValueConfigDTO v1 = new WeightedValueConfigDTO();
        v1.setValue("variant_a");
        v1.setWeight(new BigDecimal("50"));
        
        WeightedValueConfigDTO v2 = new WeightedValueConfigDTO();
        v2.setValue("variant_b");
        v2.setWeight(new BigDecimal("50"));
        
        defaultReturn.setWeightedValues(List.of(v1, v2));
        experiment.setDefaultValue(defaultReturn);
        
        return experiment;
    }

    private ExperimentConfigDTO createRuleBasedExperiment() {
        ExperimentConfigDTO experiment = new ExperimentConfigDTO();
        experiment.setExperimentId(3L);
        experiment.setExperimentName("rule_based_exp");
        experiment.setStatus(ExperimentConfigDTO.ExperimentStatus.RUNNING);
        
        RuleConfigDTO rule1 = new RuleConfigDTO();
        rule1.setId(1L);
        rule1.setPriority(1);
        
        RuleConditionConfigDTO condition1 = new RuleConditionConfigDTO();
        condition1.setFieldName("age");
        condition1.setOperator(RuleOperator.GTE);
        condition1.setValue("18");
        
        RuleConditionConfigDTO condition2 = new RuleConditionConfigDTO();
        condition2.setFieldName("region");
        condition2.setOperator(RuleOperator.EQ);
        condition2.setValue("CN");
        
        rule1.setConditions(List.of(condition1, condition2));
        
        ReturnValueConfigDTO rule1Return = new ReturnValueConfigDTO();
        rule1Return.setMode(ReturnValueMode.FIXED);
        rule1Return.setFixedValue("adult_cn");
        rule1.setReturnValue(rule1Return);
        
        RuleConfigDTO rule2 = new RuleConfigDTO();
        rule2.setId(2L);
        rule2.setPriority(2);
        
        RuleConditionConfigDTO condition3 = new RuleConditionConfigDTO();
        condition3.setFieldName("vip");
        condition3.setOperator(RuleOperator.EQ);
        condition3.setValue("true");
        
        rule2.setConditions(List.of(condition3));
        
        ReturnValueConfigDTO rule2Return = new ReturnValueConfigDTO();
        rule2Return.setMode(ReturnValueMode.FIXED);
        rule2Return.setFixedValue("vip_user");
        rule2.setReturnValue(rule2Return);
        
        experiment.setRules(List.of(rule1, rule2));
        
        ReturnValueConfigDTO defaultReturn = new ReturnValueConfigDTO();
        defaultReturn.setMode(ReturnValueMode.FIXED);
        defaultReturn.setFixedValue("default_user");
        experiment.setDefaultValue(defaultReturn);
        
        return experiment;
    }

    private ExperimentConfigDTO createPausedExperiment() {
        ExperimentConfigDTO experiment = new ExperimentConfigDTO();
        experiment.setExperimentId(4L);
        experiment.setExperimentName("paused_exp");
        experiment.setStatus(ExperimentConfigDTO.ExperimentStatus.PAUSED);
        
        ReturnValueConfigDTO defaultReturn = new ReturnValueConfigDTO();
        defaultReturn.setMode(ReturnValueMode.FIXED);
        defaultReturn.setFixedValue("paused_value");
        experiment.setDefaultValue(defaultReturn);
        
        return experiment;
    }
}
