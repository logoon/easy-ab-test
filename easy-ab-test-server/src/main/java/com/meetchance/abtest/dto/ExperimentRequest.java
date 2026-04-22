package com.meetchance.abtest.dto;

import com.meetchance.abtest.entity.Experiment;
import com.meetchance.abtest.entity.ExperimentGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExperimentRequest {
    @NotBlank(message = "实验名称不能为空")
    private String experimentName;
    
    @NotBlank(message = "版本不能为空")
    private String version;
    
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    
    private Experiment.SplitStrategy splitStrategy;
    private Integer percentage;
    private String userAttribute;
    private String attributeValues;
    
    private List<ExperimentGroup> groups;
    
    @NotNull(message = "关联服务不能为空")
    private Long serviceId;
}
