package com.meetchance.abtest.service;

import com.meetchance.abtest.dto.ServiceRequest;
import com.meetchance.abtest.entity.ServiceEntity;
import com.meetchance.abtest.entity.User;
import com.meetchance.abtest.mapper.ServiceMapper;
import com.meetchance.abtest.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceService {
    
    private final ServiceMapper serviceMapper;
    private final UserMapper userMapper;
    
    public ServiceEntity createService(ServiceRequest request, String username) {
        User user = userMapper.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        if (serviceMapper.existsByServiceCode(request.getServiceCode())) {
            throw new RuntimeException("服务编码已存在");
        }
        
        ServiceEntity service = new ServiceEntity();
        service.setServiceName(request.getServiceName());
        service.setServiceCode(request.getServiceCode());
        service.setDescription(request.getDescription());
        service.setCreatedBy(user.getId());
        
        serviceMapper.save(service);
        return service;
    }
    
    public ServiceEntity updateService(Long id, ServiceRequest request) {
        ServiceEntity service = serviceMapper.findById(id)
            .orElseThrow(() -> new RuntimeException("服务不存在"));
        
        service.setServiceName(request.getServiceName());
        service.setDescription(request.getDescription());
        
        serviceMapper.update(service);
        return service;
    }
    
    public void deleteService(Long id) {
        if (!serviceMapper.existsById(id)) {
            throw new RuntimeException("服务不存在");
        }
        serviceMapper.deleteById(id);
    }
    
    public ServiceEntity getServiceById(Long id) {
        return serviceMapper.findById(id)
            .orElseThrow(() -> new RuntimeException("服务不存在"));
    }
    
    public ServiceEntity getServiceByCode(String code) {
        return serviceMapper.findByServiceCode(code)
            .orElseThrow(() -> new RuntimeException("服务不存在"));
    }
    
    public List<ServiceEntity> getAllServices(String username) {
        User user = userMapper.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        return serviceMapper.findByCreatedBy(user.getId());
    }
    
    public List<ServiceEntity> getAllServices() {
        return serviceMapper.findAll();
    }
}
