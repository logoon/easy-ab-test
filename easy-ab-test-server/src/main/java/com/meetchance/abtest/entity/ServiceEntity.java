package com.meetchance.abtest.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ServiceEntity {
    private Long id;
    private String serviceName;
    private String serviceCode;
    private String description;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
