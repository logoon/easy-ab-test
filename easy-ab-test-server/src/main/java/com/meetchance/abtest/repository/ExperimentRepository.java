package com.meetchance.abtest.repository;

import com.meetchance.abtest.entity.Experiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {
    List<Experiment> findByServiceId(Long serviceId);
    List<Experiment> findByCreatedBy(Long createdBy);
    List<Experiment> findByStatus(Experiment.ExperimentStatus status);
    List<Experiment> findByServiceIdAndStatus(Long serviceId, Experiment.ExperimentStatus status);
}
