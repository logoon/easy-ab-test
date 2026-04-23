package com.meetchance.abtest.starter.client;

import java.util.Map;

public interface AbTestClient {
    
    String getValue(String experimentName, Map<String, Object> userAttributes);
    
    String getValueOrDefault(String experimentName, Map<String, Object> userAttributes, String defaultValue);
    
    boolean isExperimentRunning(String experimentName);
}
