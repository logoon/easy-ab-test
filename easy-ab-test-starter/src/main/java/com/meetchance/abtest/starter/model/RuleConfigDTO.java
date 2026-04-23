package com.meetchance.abtest.starter.model;

import lombok.Data;
import java.util.List;

@Data
public class RuleConfigDTO {
    private Long id;
    private Integer priority;
    private List<RuleConditionConfigDTO> conditions;
    private ReturnValueConfigDTO returnValue;
}
