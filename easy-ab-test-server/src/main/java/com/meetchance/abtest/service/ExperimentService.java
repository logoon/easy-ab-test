package com.meetchance.abtest.service;

import com.meetchance.abtest.dto.ExperimentRequest;
import com.meetchance.abtest.entity.Experiment;
import com.meetchance.abtest.entity.ExperimentGroup;
import com.meetchance.abtest.entity.User;
import com.meetchance.abtest.repository.ExperimentRepository;
import com.meetchance.abtest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ExperimentService {
    
    private final ExperimentRepository experimentRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String EXPERIMENT_CACHE_PREFIX = "experiment:";
    private static final String SERVICE_EXPERIMENTS_CACHE_PREFIX = "service:experiments:";
    
    public Experiment createExperiment(ExperimentRequest request, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Experiment experiment = new Experiment();
        experiment.setExperimentName(request.getExperimentName());
        experiment.setVersion(request.getVersion());
        experiment.setEffectiveTime(request.getEffectiveTime());
        experiment.setExpireTime(request.getExpireTime());
        experiment.setSplitStrategy(request.getSplitStrategy() != null ? 
            request.getSplitStrategy() : Experiment.SplitStrategy.PERCENTAGE);
        experiment.setPercentage(request.getPercentage());
        experiment.setUserAttribute(request.getUserAttribute());
        experiment.setAttributeValues(request.getAttributeValues());
        experiment.setGroups(request.getGroups());
        experiment.setServiceId(request.getServiceId());
        experiment.setStatus(Experiment.ExperimentStatus.DRAFT);
        experiment.setCreatedBy(user.getId());
        
        Experiment saved = experimentRepository.save(experiment);
        cacheExperiment(saved);
        
        return saved;
    }
    
    public Experiment updateExperiment(Long id, ExperimentRequest request) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("实验不存在"));
        
        experiment.setExperimentName(request.getExperimentName());
        experiment.setVersion(request.getVersion());
        experiment.setEffectiveTime(request.getEffectiveTime());
        experiment.setExpireTime(request.getExpireTime());
        if (request.getSplitStrategy() != null) {
            experiment.setSplitStrategy(request.getSplitStrategy());
        }
        experiment.setPercentage(request.getPercentage());
        experiment.setUserAttribute(request.getUserAttribute());
        experiment.setAttributeValues(request.getAttributeValues());
        
        if (request.getGroups() != null) {
            experiment.getGroups().clear();
            experiment.getGroups().addAll(request.getGroups());
        }
        
        experiment.setServiceId(request.getServiceId());
        
        Experiment saved = experimentRepository.save(experiment);
        cacheExperiment(saved);
        
        return saved;
    }
    
    public void deleteExperiment(Long id) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("实验不存在"));
        
        experimentRepository.deleteById(id);
        evictExperimentCache(experiment);
    }
    
    public Experiment getExperimentById(Long id) {
        String cacheKey = EXPERIMENT_CACHE_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        
        if (cached instanceof Experiment experiment) {
            return experiment;
        }
        
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("实验不存在"));
        
        cacheExperiment(experiment);
        return experiment;
    }
    
    public List<Experiment> getExperimentsByService(Long serviceId) {
        String cacheKey = SERVICE_EXPERIMENTS_CACHE_PREFIX + serviceId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        
        if (cached instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Experiment) {
            @SuppressWarnings("unchecked")
            List<Experiment> experiments = (List<Experiment>) list;
            return experiments;
        }
        
        List<Experiment> experiments = experimentRepository.findByServiceId(serviceId);
        cacheServiceExperiments(serviceId, experiments);
        
        return experiments;
    }
    
    public List<Experiment> getRunningExperimentsByService(Long serviceId) {
        return experimentRepository.findByServiceIdAndStatus(serviceId, Experiment.ExperimentStatus.RUNNING);
    }
    
    public Experiment updateStatus(Long id, Experiment.ExperimentStatus status) {
        Experiment experiment = experimentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("实验不存在"));
        
        experiment.setStatus(status);
        Experiment saved = experimentRepository.save(experiment);
        cacheExperiment(saved);
        
        return saved;
    }
    
    public ExperimentGroup getExperimentGroup(Long experimentId, String userId) {
        Experiment experiment = getExperimentById(experimentId);
        
        if (experiment.getStatus() != Experiment.ExperimentStatus.RUNNING) {
            throw new RuntimeException("实验未运行");
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (experiment.getEffectiveTime() != null && now.isBefore(experiment.getEffectiveTime())) {
            throw new RuntimeException("实验尚未生效");
        }
        if (experiment.getExpireTime() != null && now.isAfter(experiment.getExpireTime())) {
            throw new RuntimeException("实验已过期");
        }
        
        List<ExperimentGroup> groups = experiment.getGroups();
        if (groups == null || groups.isEmpty()) {
            throw new RuntimeException("实验没有配置实验组");
        }
        
        if (experiment.getSplitStrategy() == Experiment.SplitStrategy.USER_ATTRIBUTE) {
            return groups.stream()
                .filter(ExperimentGroup::getIsControl)
                .findFirst()
                .orElse(groups.get(0));
        }
        
        int hash = Math.abs((userId + ":" + experiment.getId()).hashCode());
        int totalWeight = groups.stream()
            .mapToInt(g -> g.getWeight() != null ? g.getWeight() : 1)
            .sum();
        
        int bucket = hash % totalWeight;
        int currentWeight = 0;
        
        for (ExperimentGroup group : groups) {
            int weight = group.getWeight() != null ? group.getWeight() : 1;
            currentWeight += weight;
            if (bucket < currentWeight) {
                return group;
            }
        }
        
        return groups.get(0);
    }
    
    private void cacheExperiment(Experiment experiment) {
        String cacheKey = EXPERIMENT_CACHE_PREFIX + experiment.getId();
        redisTemplate.opsForValue().set(cacheKey, experiment, 1, TimeUnit.HOURS);
    }
    
    private void evictExperimentCache(Experiment experiment) {
        String cacheKey = EXPERIMENT_CACHE_PREFIX + experiment.getId();
        redisTemplate.delete(cacheKey);
        
        String serviceCacheKey = SERVICE_EXPERIMENTS_CACHE_PREFIX + experiment.getServiceId();
        redisTemplate.delete(serviceCacheKey);
    }
    
    private void cacheServiceExperiments(Long serviceId, List<Experiment> experiments) {
        String cacheKey = SERVICE_EXPERIMENTS_CACHE_PREFIX + serviceId;
        redisTemplate.opsForValue().set(cacheKey, experiments, 30, TimeUnit.MINUTES);
    }
}
