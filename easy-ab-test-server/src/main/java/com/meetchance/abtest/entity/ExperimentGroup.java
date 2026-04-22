package com.meetchance.abtest.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "experiment_groups")
public class ExperimentGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String groupName;
    
    @Column(nullable = false)
    private String groupCode;
    
    private Integer weight;
    
    @Column(columnDefinition = "TEXT")
    private String config;
    
    private Boolean isControl = false;
}
