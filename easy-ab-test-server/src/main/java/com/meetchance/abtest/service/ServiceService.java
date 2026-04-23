package com.meetchance.abtest.service;

import com.meetchance.abtest.dto.ServiceRequest;
import com.meetchance.abtest.entity.ServiceEntity;
import com.meetchance.abtest.entity.User;
import com.meetchance.abtest.repository.ServiceRepository;
import com.meetchance.abtest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceService {
    
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    
    public ServiceEntity createService(ServiceRequest request, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        if (serviceRepository.existsByServiceCode(request.getServiceCode())) {
            throw new RuntimeException("服务编码已存在");
        }
        
        ServiceEntity service = new ServiceEntity();
        service.setServiceName(request.getServiceName());
        service.setServiceCode(request.getServiceCode());
        service.setDescription(request.getDescription());
        service.setCreatedBy(user.getId());
        
        return serviceRepository.save(service);
    }
    
    public ServiceEntity updateService(Long id, ServiceRequest request) {
        ServiceEntity service = serviceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("服务不存在"));
        
        service.setServiceName(request.getServiceName());
        service.setDescription(request.getDescription());
        
        return serviceRepository.save(service);
    }
    
    public void deleteService(Long id) {
        if (!serviceRepository.existsById(id)) {
            throw new RuntimeException("服务不存在");
        }
        serviceRepository.deleteById(id);
    }
    
    public ServiceEntity getServiceById(Long id) {
        return serviceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("服务不存在"));
    }
    
    public ServiceEntity getServiceByCode(String code) {
        return serviceRepository.findByServiceCode(code)
            .orElseThrow(() -> new RuntimeException("服务不存在"));
    }
    
    public List<ServiceEntity> getAllServices(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return serviceRepository.findByCreatedBy(user.getId());
    }
    
    public List<ServiceEntity> getAllServices() {
        return serviceRepository.findAll();
    }
}
