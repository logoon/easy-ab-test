package com.meetchance.abtest.entity;

import lombok.Data;
import java.util.List;

@Data
public class ReturnValue {
    private ReturnValueMode mode;
    private String fixedValue;
    private List<WeightedValue> weightedValues;
}
