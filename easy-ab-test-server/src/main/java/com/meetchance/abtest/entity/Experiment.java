package com.meetchance.abtest.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Experiment {
    private Long id;
    private String experimentName;
    private String version;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    private SplitStrategy splitStrategy = SplitStrategy.PERCENTAGE;
    private Integer percentage;
    private String userAttribute;
    private String attributeValues;
    private List<ExperimentGroup> groups;
    private Long serviceId;
    private ExperimentStatus status = ExperimentStatus.DRAFT;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum SplitStrategy {
        PERCENTAGE,
        USER_ATTRIBUTE
    }
    
    public enum ExperimentStatus {
        DRAFT,
        RUNNING,
        PAUSED,
        FINISHED
    }
}
