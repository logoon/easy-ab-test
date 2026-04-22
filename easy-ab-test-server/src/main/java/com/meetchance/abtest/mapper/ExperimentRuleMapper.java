package com.meetchance.abtest.mapper;

import com.meetchance.abtest.entity.ExperimentRule;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ExperimentRuleMapper {
    
    @Select("SELECT * FROM experiment_rules WHERE id = #{id}")
    Optional<ExperimentRule> findById(Long id);
    
    @Select("SELECT * FROM experiment_rules WHERE experiment_id = #{experimentId} ORDER BY priority ASC")
    List<ExperimentRule> findByExperimentId(Long experimentId);
    
    @Select("SELECT * FROM experiment_rules")
    List<ExperimentRule> findAll();
    
    @Insert("INSERT INTO experiment_rules (experiment_id, priority, conditions_json, return_value_json) " +
            "VALUES (#{experimentId}, #{priority}, #{conditionsJson}, #{returnValueJson})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(ExperimentRule rule);
    
    @Update("UPDATE experiment_rules SET priority = #{priority}, conditions_json = #{conditionsJson}, " +
            "return_value_json = #{returnValueJson} WHERE id = #{id}")
    int update(ExperimentRule rule);
    
    @Delete("DELETE FROM experiment_rules WHERE id = #{id}")
    int deleteById(Long id);
    
    @Delete("DELETE FROM experiment_rules WHERE experiment_id = #{experimentId}")
    int deleteByExperimentId(Long experimentId);
    
    @Select("SELECT COUNT(*) FROM experiment_rules WHERE id = #{id}")
    boolean existsById(Long id);
}
