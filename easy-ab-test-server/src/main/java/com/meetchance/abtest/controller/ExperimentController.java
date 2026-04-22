package com.meetchance.abtest.controller;

import com.meetchance.abtest.dto.ExperimentRequest;
import com.meetchance.abtest.entity.Experiment;
import com.meetchance.abtest.service.ExperimentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/experiments")
@RequiredArgsConstructor
public class ExperimentController {
    
    private final ExperimentService experimentService;
    
    @PostMapping
    public ResponseEntity<Experiment> createExperiment(
        @Valid @RequestBody ExperimentRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(experimentService.createExperiment(request, userDetails.getUsername()));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Experiment> updateExperiment(
        @PathVariable Long id,
        @Valid @RequestBody ExperimentRequest request
    ) {
        return ResponseEntity.ok(experimentService.updateExperiment(id, request));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExperiment(@PathVariable Long id) {
        experimentService.deleteExperiment(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Experiment> getExperimentById(@PathVariable Long id) {
        return ResponseEntity.ok(experimentService.getExperimentById(id));
    }
    
    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<Experiment>> getExperimentsByService(@PathVariable Long serviceId) {
        return ResponseEntity.ok(experimentService.getExperimentsByService(serviceId));
    }
    
    @GetMapping("/service/{serviceId}/running")
    public ResponseEntity<List<Experiment>> getRunningExperimentsByService(@PathVariable Long serviceId) {
        return ResponseEntity.ok(experimentService.getRunningExperimentsByService(serviceId));
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<Experiment> updateStatus(
        @PathVariable Long id,
        @RequestParam Experiment.ExperimentStatus status
    ) {
        return ResponseEntity.ok(experimentService.updateStatus(id, status));
    }
}
