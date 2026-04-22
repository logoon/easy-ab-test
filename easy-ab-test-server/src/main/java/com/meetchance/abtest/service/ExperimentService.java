package com.meetchance.abtest.service;

import com.meetchance.abtest.dto.ExperimentRequest;
import com.meetchance.abtest.entity.Experiment;
import com.meetchance.abtest.entity.ExperimentGroup;
import com.meetchance.abtest.entity.ExperimentRule;
import com.meetchance.abtest.entity.ReturnValue;
import com.meetchance.abtest.entity.User;
import com.meetchance.abtest.mapper.ExperimentGroupMapper;
import com.meetchance.abtest.mapper.ExperimentMapper;
import com.meetchance.abtest.mapper.ExperimentRuleMapper;
import com.meetchance.abtest.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ExperimentService {
    
    private final ExperimentMapper experimentMapper;
    private final ExperimentGroupMapper experimentGroupMapper;
    private final ExperimentRuleMapper experimentRuleMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ConfigChangeNotifier configChangeNotifier;
    private final RuleMatchService ruleMatchService;
    
    private static final String EXPERIMENT_CACHE_PREFIX = "experiment:";
    private static final String SERVICE_EXPERIMENTS_CACHE_PREFIX = "service:experiments:";
    
    @Transactional
    public Experiment createExperiment(ExperimentRequest request, String username) {
        User user = userMapper.findByUsername(username)
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
        experiment.setServiceId(request.getServiceId());
        experiment.setStatus(Experiment.ExperimentStatus.DRAFT);
        experiment.setCreatedBy(user.getId());
        experiment.setReturnValueType(request.getReturnValueType());
        experiment.setDefaultValue(request.getDefaultValue());
        experiment.toJson();
        
        experimentMapper.save(experiment);
        
        if (request.getGroups() != null) {
            for (ExperimentGroup group : request.getGroups()) {
                group.setExperimentId(experiment.getId());
                experimentGroupMapper.save(group);
            }
        }
        
        if (request.getRules() != null) {
            for (ExperimentRule rule : request.getRules()) {
                rule.setExperimentId(experiment.getId());
                rule.toJson();
                experimentRuleMapper.save(rule);
            }
        }
        
        Experiment saved = getExperimentById(experiment.getId());
        cacheExperiment(saved);
        
        if (saved.getStatus() == Experiment.ExperimentStatus.RUNNING) {
            configChangeNotifier.notifyConfigChange(saved.getServiceId());
        }
        
        return saved;
    }
    
    @Transactional
    public Experiment updateExperiment(Long id, ExperimentRequest request) {
        Experiment experiment = experimentMapper.findById(id)
            .orElseThrow(() -> new RuntimeException("实验不存在"));
        
        Long originalServiceId = experiment.getServiceId();
        Experiment.ExperimentStatus originalStatus = experiment.getStatus();
        
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
        experiment.setServiceId(request.getServiceId());
        if (request.getReturnValueType() != null) {
            experiment.setReturnValueType(request.getReturnValueType());
        }
        if (request.getDefaultValue() != null) {
            experiment.setDefaultValue(request.getDefaultValue());
        }
        experiment.toJson();
        
        experimentMapper.update(experiment);
        
        if (request.getGroups() != null) {
            experimentGroupMapper.deleteByExperimentId(id);
            for (ExperimentGroup group : request.getGroups()) {
                group.setExperimentId(id);
                experimentGroupMapper.save(group);
            }
        }
        
        if (request.getRules() != null) {
            experimentRuleMapper.deleteByExperimentId(id);
            for (ExperimentRule rule : request.getRules()) {
                rule.setExperimentId(id);
                rule.toJson();
                experimentRuleMapper.save(rule);
            }
        }
        
        Experiment saved = getExperimentById(id);
        cacheExperiment(saved);
        
        if (originalStatus == Experiment.ExperimentStatus.RUNNING || 
            saved.getStatus() == Experiment.ExperimentStatus.RUNNING) {
            configChangeNotifier.notifyConfigChange(saved.getServiceId());
            if (!originalServiceId.equals(saved.getServiceId())) {
                configChangeNotifier.notifyConfigChange(originalServiceId);
            }
        }
        
        return saved;
    }
    
    @Transactional
    public void deleteExperiment(Long id) {
        Experiment experiment = experimentMapper.findById(id)
            .orElseThrow(() -> new RuntimeException("实验不存在"));
        
        Long serviceId = experiment.getServiceId();
        Experiment.ExperimentStatus status = experiment.getStatus();
        
        experimentRuleMapper.deleteByExperimentId(id);
        experimentGroupMapper.deleteByExperimentId(id);
        experimentMapper.deleteById(id);
        evictExperimentCache(experiment);
        
        if (status == Experiment.ExperimentStatus.RUNNING) {
            configChangeNotifier.notifyConfigChange(serviceId);
        }
    }
    
    public Experiment getExperimentById(Long id) {
        String cacheKey = EXPERIMENT_CACHE_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        
        if (cached instanceof Experiment experiment) {
            return experiment;
        }
        
        Experiment experiment = experimentMapper.findById(id)
            .orElseThrow(() -> new RuntimeException("实验不存在"));
        
        List<ExperimentGroup> groups = experimentGroupMapper.findByExperimentId(id);
        experiment.setGroups(groups);
        
        List<ExperimentRule> rules = experimentRuleMapper.findByExperimentId(id);
        for (ExperimentRule rule : rules) {
            rule.parseJson();
        }
        experiment.setRules(rules);
        experiment.parseJson();
        
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
        
        List<Experiment> experiments = experimentMapper.findByServiceId(serviceId);
        for (Experiment experiment : experiments) {
            List<ExperimentGroup> groups = experimentGroupMapper.findByExperimentId(experiment.getId());
            experiment.setGroups(groups);
            
            List<ExperimentRule> rules = experimentRuleMapper.findByExperimentId(experiment.getId());
            for (ExperimentRule rule : rules) {
                rule.parseJson();
            }
            experiment.setRules(rules);
            experiment.parseJson();
        }
        
        cacheServiceExperiments(serviceId, experiments);
        
        return experiments;
    }
    
    public List<Experiment> getRunningExperimentsByService(Long serviceId) {
        List<Experiment> experiments = experimentMapper.findByServiceIdAndStatus(serviceId, Experiment.ExperimentStatus.RUNNING);
        for (Experiment experiment : experiments) {
            List<ExperimentGroup> groups = experimentGroupMapper.findByExperimentId(experiment.getId());
            experiment.setGroups(groups);
            
            List<ExperimentRule> rules = experimentRuleMapper.findByExperimentId(experiment.getId());
            for (ExperimentRule rule : rules) {
                rule.parseJson();
            }
            experiment.setRules(rules);
            experiment.parseJson();
        }
        return experiments;
    }
    
    @Transactional
    public Experiment updateStatus(Long id, Experiment.ExperimentStatus status) {
        Experiment experiment = experimentMapper.findById(id)
            .orElseThrow(() -> new RuntimeException("实验不存在"));
        
        Experiment.ExperimentStatus originalStatus = experiment.getStatus();
        
        experiment.setStatus(status);
        experimentMapper.update(experiment);
        
        Experiment saved = getExperimentById(id);
        cacheExperiment(saved);
        
        if (originalStatus == Experiment.ExperimentStatus.RUNNING || 
            saved.getStatus() == Experiment.ExperimentStatus.RUNNING) {
            configChangeNotifier.notifyConfigChange(saved.getServiceId());
        }
        
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
    
    public String evaluateExperiment(Experiment experiment, Map<String, Object> userAttributes) {
        if (experiment == null) {
            return null;
        }
        
        if (experiment.getRules() != null && !experiment.getRules().isEmpty()) {
            ExperimentRule matchedRule = ruleMatchService.matchRule(experiment.getRules(), userAttributes);
            if (matchedRule != null && matchedRule.getReturnValue() != null) {
                return ruleMatchService.resolveReturnValue(matchedRule.getReturnValue());
            }
        }
        
        if (experiment.getDefaultValue() != null) {
            return ruleMatchService.resolveReturnValue(experiment.getDefaultValue());
        }
        
        return null;
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
