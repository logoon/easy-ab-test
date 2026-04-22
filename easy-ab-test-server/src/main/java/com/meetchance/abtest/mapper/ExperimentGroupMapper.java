package com.meetchance.abtest.mapper;

import com.meetchance.abtest.entity.ExperimentGroup;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ExperimentGroupMapper {
    
    @Select("SELECT * FROM experiment_groups WHERE id = #{id}")
    Optional<ExperimentGroup> findById(Long id);
    
    @Select("SELECT * FROM experiment_groups WHERE experiment_id = #{experimentId}")
    List<ExperimentGroup> findByExperimentId(Long experimentId);
    
    @Select("SELECT * FROM experiment_groups")
    List<ExperimentGroup> findAll();
    
    @Insert("INSERT INTO experiment_groups (experiment_id, group_name, group_code, weight, config, is_control) " +
            "VALUES (#{experimentId}, #{groupName}, #{groupCode}, #{weight}, #{config}, #{isControl})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(ExperimentGroup group);
    
    @Update("UPDATE experiment_groups SET group_name = #{groupName}, group_code = #{groupCode}, " +
            "weight = #{weight}, config = #{config}, is_control = #{isControl} WHERE id = #{id}")
    int update(ExperimentGroup group);
    
    @Delete("DELETE FROM experiment_groups WHERE id = #{id}")
    int deleteById(Long id);
    
    @Delete("DELETE FROM experiment_groups WHERE experiment_id = #{experimentId}")
    int deleteByExperimentId(Long experimentId);
    
    @Select("SELECT COUNT(*) FROM experiment_groups WHERE id = #{id}")
    boolean existsById(Long id);
}
