package com.meetchance.abtest.starter.model;

import lombok.Data;
import java.util.List;

@Data
public class ReturnValueConfigDTO {
    private ReturnValueMode mode;
    private String fixedValue;
    private List<WeightedValueConfigDTO> weightedValues;
}
