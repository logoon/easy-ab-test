package com.meetchance.abtest.controller;

import com.meetchance.abtest.dto.ServiceConfigDTO;
import com.meetchance.abtest.entity.Experiment;
import com.meetchance.abtest.entity.ServiceEntity;
import com.meetchance.abtest.service.ExperimentService;
import com.meetchance.abtest.service.ServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sdk")
@RequiredArgsConstructor
public class SdkController {
    
    private final ServiceService serviceService;
    private final ExperimentService experimentService;
    
    @GetMapping("/config/{serviceCode}")
    public ResponseEntity<ServiceConfigDTO> getServiceConfig(@PathVariable String serviceCode) {
        ServiceEntity service = serviceService.getServiceByCode(serviceCode);
        List<Experiment> experiments = experimentService.getRunningExperimentsByService(service.getId());
        
        ServiceConfigDTO config = ServiceConfigDTO.fromEntities(service, experiments);
        return ResponseEntity.ok(config);
    }
}
