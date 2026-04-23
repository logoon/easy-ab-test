package com.meetchance.abtest.demo.controller;

import com.meetchance.abtest.starter.client.AbTestClient;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/abtest")
@RequiredArgsConstructor
public class AbTestController {

    private final AbTestClient abTestClient;

    @PostMapping("/getValue")
    public ResponseEntity<AbTestResponse> getValue(@RequestBody AbTestRequest request) {
        log.info("getValue called - experiment: {}, attributes: {}", 
            request.getExperimentName(), request.getUserAttributes());
        
        String value = abTestClient.getValue(
            request.getExperimentName(),
            request.getUserAttributes()
        );
        
        log.info("getValue result: {}", value);
        
        return ResponseEntity.ok(AbTestResponse.builder()
            .success(true)
            .experimentName(request.getExperimentName())
            .userAttributes(request.getUserAttributes())
            .value(value)
            .build());
    }

    @PostMapping("/getValueOrDefault")
    public ResponseEntity<AbTestResponse> getValueOrDefault(@RequestBody AbTestRequestWithDefault request) {
        log.info("getValueOrDefault called - experiment: {}, attributes: {}, defaultValue: {}", 
            request.getExperimentName(), request.getUserAttributes(), request.getDefaultValue());
        
        String value = abTestClient.getValueOrDefault(
            request.getExperimentName(),
            request.getUserAttributes(),
            request.getDefaultValue()
        );
        
        log.info("getValueOrDefault result: {}", value);
        
        return ResponseEntity.ok(AbTestResponse.builder()
            .success(true)
            .experimentName(request.getExperimentName())
            .userAttributes(request.getUserAttributes())
            .value(value)
            .defaultValueUsed(!value.equals(request.getDefaultValue()) ? false : 
                (abTestClient.getValue(request.getExperimentName(), request.getUserAttributes()) == null))
            .build());
    }

    @PostMapping("/isExperimentRunning")
    public ResponseEntity<ExperimentStatusResponse> isExperimentRunning(
            @RequestParam String experimentName) {
        log.info("isExperimentRunning called - experiment: {}", experimentName);
        
        boolean running = abTestClient.isExperimentRunning(experimentName);
        
        return ResponseEntity.ok(ExperimentStatusResponse.builder()
            .experimentName(experimentName)
            .isRunning(running)
            .build());
    }

    @Data
    public static class AbTestRequest {
        private String experimentName;
        private Map<String, Object> userAttributes = new HashMap<>();
    }

    @Data
    public static class AbTestRequestWithDefault {
        private String experimentName;
        private Map<String, Object> userAttributes = new HashMap<>();
        private String defaultValue;
    }

    @Data
    @Builder
    public static class AbTestResponse {
        private boolean success;
        private String experimentName;
        private Map<String, Object> userAttributes;
        private String value;
        private Boolean defaultValueUsed;
    }

    @Data
    @Builder
    public static class ExperimentStatusResponse {
        private String experimentName;
        private boolean isRunning;
    }
}
