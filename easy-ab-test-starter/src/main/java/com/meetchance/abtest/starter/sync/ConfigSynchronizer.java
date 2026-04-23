package com.meetchance.abtest.starter.sync;

public interface ConfigSynchronizer {
    
    void start();
    
    void stop();
    
    boolean isRunning();
}
