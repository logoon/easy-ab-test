package com.meetchance.abtest.repository;

import com.meetchance.abtest.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    Optional<ServiceEntity> findByServiceCode(String serviceCode);
    boolean existsByServiceCode(String serviceCode);
    List<ServiceEntity> findByCreatedBy(Long createdBy);
}
