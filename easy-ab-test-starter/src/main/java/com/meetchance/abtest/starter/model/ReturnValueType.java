package com.meetchance.abtest.starter.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public enum ReturnValueType {
    STRING,
    @JsonAlias("NUMBER")
    INT,
    BOOLEAN,
    DECIMAL,
    JSON
}
