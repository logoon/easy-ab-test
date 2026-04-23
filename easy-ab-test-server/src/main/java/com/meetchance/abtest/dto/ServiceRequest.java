package com.meetchance.abtest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ServiceRequest {
    @NotBlank(message = "服务名不能为空")
    private String serviceName;
    
    @NotBlank(message = "服务编码不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "服务编码必须以字母开头，只能包含字母、数字和下划线")
    private String serviceCode;
    
    private String description;
}
