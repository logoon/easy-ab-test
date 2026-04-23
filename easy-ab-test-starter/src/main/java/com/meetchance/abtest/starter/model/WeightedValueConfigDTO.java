package com.meetchance.abtest.starter.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WeightedValueConfigDTO {
    private BigDecimal weight;
    private String value;
}
