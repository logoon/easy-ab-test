package com.meetchance.abtest.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WeightedValue {
    private BigDecimal weight;
    private String value;
}
