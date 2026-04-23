package com.meetchance.abtest.mapper;

import com.meetchance.abtest.entity.Experiment;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ExperimentMapper {
    
    @Select("SELECT * FROM experiments WHERE id = #{id}")
    Optional<Experiment> findById(Long id);
    
    @Select("SELECT * FROM experiments WHERE service_id = #{serviceId}")
    List<Experiment> findByServiceId(Long serviceId);
    
    @Select("SELECT * FROM experiments WHERE created_by = #{createdBy}")
    List<Experiment> findByCreatedBy(Long createdBy);
    
    @Select("SELECT * FROM experiments WHERE status = #{status}")
    List<Experiment> findByStatus(Experiment.ExperimentStatus status);
    
    @Select("SELECT * FROM experiments WHERE service_id = #{serviceId} AND status = #{status}")
    List<Experiment> findByServiceIdAndStatus(@Param("serviceId") Long serviceId, @Param("status") Experiment.ExperimentStatus status);
    
    @Select("SELECT * FROM experiments")
    List<Experiment> findAll();
    
    @Insert("INSERT INTO experiments (experiment_name, version, effective_time, expire_time, " +
            "split_strategy, percentage, user_attribute, attribute_values, service_id, status, " +
            "return_value_type, default_value_json, created_by, created_at, updated_at) " +
            "VALUES (#{experimentName}, #{version}, #{effectiveTime}, " +
            "#{expireTime}, #{splitStrategy}, #{percentage}, #{userAttribute}, #{attributeValues}, " +
            "#{serviceId}, #{status}, #{returnValueType}, #{defaultValueJson}, #{createdBy}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(Experiment experiment);
    
    @Update("UPDATE experiments SET experiment_name = #{experimentName}, version = #{version}, " +
            "effective_time = #{effectiveTime}, expire_time = #{expireTime}, split_strategy = #{splitStrategy}, " +
            "percentage = #{percentage}, user_attribute = #{userAttribute}, attribute_values = #{attributeValues}, " +
            "service_id = #{serviceId}, status = #{status}, return_value_type = #{returnValueType}, " +
            "default_value_json = #{defaultValueJson}, updated_at = NOW() WHERE id = #{id}")
    int update(Experiment experiment);
    
    @Delete("DELETE FROM experiments WHERE id = #{id}")
    int deleteById(Long id);
    
    @Select("SELECT COUNT(*) FROM experiments WHERE id = #{id}")
    boolean existsById(Long id);
}
