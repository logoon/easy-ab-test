package com.meetchance.abtest.starter.model;

import lombok.Data;
import java.util.List;

@Data
public class ServiceConfigDTO {
    private String serviceCode;
    private String serviceName;
    private List<ExperimentConfigDTO> experiments;
}
