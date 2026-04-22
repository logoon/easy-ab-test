package com.meetchance.abtest.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "experiments")
public class Experiment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String experimentName;
    
    @Column(nullable = false)
    private String version;
    
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    
    @Enumerated(EnumType.STRING)
    private SplitStrategy splitStrategy = SplitStrategy.PERCENTAGE;
    
    private Integer percentage;
    private String userAttribute;
    private String attributeValues;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "experiment_id")
    private List<ExperimentGroup> groups;
    
    private Long serviceId;
    
    @Enumerated(EnumType.STRING)
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
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
