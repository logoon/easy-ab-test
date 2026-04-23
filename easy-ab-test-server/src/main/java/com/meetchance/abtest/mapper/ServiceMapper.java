package com.meetchance.abtest.mapper;

import com.meetchance.abtest.entity.ServiceEntity;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ServiceMapper {
    
    @Select("SELECT * FROM services WHERE id = #{id}")
    Optional<ServiceEntity> findById(Long id);
    
    @Select("SELECT * FROM services WHERE service_code = #{serviceCode}")
    Optional<ServiceEntity> findByServiceCode(String serviceCode);
    
    @Select("SELECT * FROM services")
    List<ServiceEntity> findAll();
    
    @Select("SELECT * FROM services WHERE created_by = #{createdBy}")
    List<ServiceEntity> findByCreatedBy(Long createdBy);
    
    @Insert("INSERT INTO services (service_name, service_code, description, created_by, created_at, updated_at) " +
            "VALUES (#{serviceName}, #{serviceCode}, #{description}, #{createdBy}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(ServiceEntity service);
    
    @Update("UPDATE services SET service_name = #{serviceName}, description = #{description}, " +
            "updated_at = NOW() WHERE id = #{id}")
    int update(ServiceEntity service);
    
    @Delete("DELETE FROM services WHERE id = #{id}")
    int deleteById(Long id);
    
    @Select("SELECT COUNT(*) FROM services WHERE service_code = #{serviceCode}")
    boolean existsByServiceCode(String serviceCode);
    
    @Select("SELECT COUNT(*) FROM services WHERE id = #{id}")
    boolean existsById(Long id);
}
