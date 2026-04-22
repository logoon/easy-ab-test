package com.meetchance.abtest.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class ExperimentGroup {
    private Long id;
    private String groupName;
    private String groupCode;
    private Integer weight;
    private String config;
    private Boolean isControl = false;
    private Long experimentId;
}
