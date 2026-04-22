package com.meetchance.abtest.controller;

import com.meetchance.abtest.dto.ServiceRequest;
import com.meetchance.abtest.entity.ServiceEntity;
import com.meetchance.abtest.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {
    
    private final ServiceService serviceService;
    
    @PostMapping
    public ResponseEntity<ServiceEntity> createService(
        @Valid @RequestBody ServiceRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(serviceService.createService(request, userDetails.getUsername()));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ServiceEntity> updateService(
        @PathVariable Long id,
        @Valid @RequestBody ServiceRequest request
    ) {
        return ResponseEntity.ok(serviceService.updateService(id, request));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        serviceService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ServiceEntity> getServiceById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceService.getServiceById(id));
    }
    
    @GetMapping("/code/{code}")
    public ResponseEntity<ServiceEntity> getServiceByCode(@PathVariable String code) {
        return ResponseEntity.ok(serviceService.getServiceByCode(code));
    }
    
    @GetMapping
    public ResponseEntity<List<ServiceEntity>> getAllServices(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(serviceService.getAllServices(userDetails.getUsername()));
    }
}
