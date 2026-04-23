package com.meetchance.abtest.demo.performance;

import com.meetchance.abtest.starter.client.AbTestClient;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceTestController {

    private final AbTestClient abTestClient;

    @PostMapping("/test")
    public ResponseEntity<PerformanceTestResult> runPerformanceTest(
            @RequestBody PerformanceTestRequest request) {
        
        log.info("Starting performance test: {}", request);
        
        int iterations = request.getIterations() > 0 ? request.getIterations() : 10000;
        int threadCount = request.getThreadCount() > 0 ? request.getThreadCount() : 1;
        String experimentName = request.getExperimentName() != null ? 
            request.getExperimentName() : "fixed_value_exp";
        
        Map<String, Object> userAttributes = request.getUserAttributes() != null ?
            request.getUserAttributes() : createDefaultAttributes();
        
        String defaultValue = "default_fallback";
        
        PerformanceTestResult result = runTest(
            experimentName, 
            userAttributes, 
            defaultValue, 
            iterations, 
            threadCount
        );
        
        log.info("Performance test completed: {}", result);
        
        return ResponseEntity.ok(result);
    }

    private PerformanceTestResult runTest(
            String experimentName,
            Map<String, Object> userAttributes,
            String defaultValue,
            int iterations,
            int threadCount) {
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        AtomicLong totalGetValueTime = new AtomicLong(0);
        AtomicLong totalGetValueOrDefaultTime = new AtomicLong(0);
        AtomicLong getValueSuccessCount = new AtomicLong(0);
        AtomicLong getValueOrDefaultSuccessCount = new AtomicLong(0);
        AtomicLong getValueMinTime = new AtomicLong(Long.MAX_VALUE);
        AtomicLong getValueMaxTime = new AtomicLong(Long.MIN_VALUE);
        AtomicLong getValueOrDefaultMinTime = new AtomicLong(Long.MAX_VALUE);
        AtomicLong getValueOrDefaultMaxTime = new AtomicLong(Long.MIN_VALUE);
        
        int iterationsPerThread = iterations / threadCount;
        
        for (int i = 0; i < threadCount; i++) {
            int threadIndex = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    Map<String, Object> threadAttributes = new HashMap<>(userAttributes);
                    threadAttributes.put("threadId", Thread.currentThread().getId());
                    
                    for (int j = 0; j < iterationsPerThread; j++) {
                        threadAttributes.put("iteration", j);
                        
                        long start = System.nanoTime();
                        String value1 = abTestClient.getValue(experimentName, threadAttributes);
                        long end = System.nanoTime();
                        long duration = end - start;
                        
                        totalGetValueTime.addAndGet(duration);
                        if (value1 != null) {
                            getValueSuccessCount.incrementAndGet();
                        }
                        updateMinMax(getValueMinTime, getValueMaxTime, duration);
                        
                        start = System.nanoTime();
                        String value2 = abTestClient.getValueOrDefault(
                            experimentName, threadAttributes, defaultValue);
                        end = System.nanoTime();
                        duration = end - start;
                        
                        totalGetValueOrDefaultTime.addAndGet(duration);
                        getValueOrDefaultSuccessCount.incrementAndGet();
                        updateMinMax(getValueOrDefaultMinTime, getValueOrDefaultMaxTime, duration);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Thread interrupted", e);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        try {
            Thread.sleep(100);
            startLatch.countDown();
            
            long overallStart = System.currentTimeMillis();
            endLatch.await();
            long overallDuration = System.currentTimeMillis() - overallStart;
            
            executor.shutdown();
            
            double totalGetValueMs = totalGetValueTime.get() / 1_000_000.0;
            double totalGetValueOrDefaultMs = totalGetValueOrDefaultTime.get() / 1_000_000.0;
            int totalCalls = iterationsPerThread * threadCount;
            
            return PerformanceTestResult.builder()
                .testName("AbTestClient Performance Test")
                .experimentName(experimentName)
                .totalIterations(totalCalls)
                .threadCount(threadCount)
                .totalDurationMs(overallDuration)
                
                .getValueStats(MethodStats.builder()
                    .methodName("getValue")
                    .totalCalls(totalCalls)
                    .successCalls(getValueSuccessCount.get())
                    .totalTimeMs(totalGetValueMs)
                    .avgTimeMs(totalGetValueMs / totalCalls)
                    .minTimeMs(getValueMinTime.get() / 1_000_000.0)
                    .maxTimeMs(getValueMaxTime.get() / 1_000_000.0)
                    .throughput(totalCalls / (overallDuration / 1000.0))
                    .build())
                
                .getValueOrDefaultStats(MethodStats.builder()
                    .methodName("getValueOrDefault")
                    .totalCalls(totalCalls)
                    .successCalls(getValueOrDefaultSuccessCount.get())
                    .totalTimeMs(totalGetValueOrDefaultMs)
                    .avgTimeMs(totalGetValueOrDefaultMs / totalCalls)
                    .minTimeMs(getValueOrDefaultMinTime.get() / 1_000_000.0)
                    .maxTimeMs(getValueOrDefaultMaxTime.get() / 1_000_000.0)
                    .throughput(totalCalls / (overallDuration / 1000.0))
                    .build())
                
                .build();
                
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Performance test interrupted", e);
        }
    }

    private void updateMinMax(AtomicLong minTime, AtomicLong maxTime, long duration) {
        long current;
        do {
            current = minTime.get();
        } while (duration < current && !minTime.compareAndSet(current, duration));
        
        do {
            current = maxTime.get();
        } while (duration > current && !maxTime.compareAndSet(current, duration));
    }

    private Map<String, Object> createDefaultAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", "test_user_123");
        attributes.put("age", 25);
        attributes.put("region", "CN");
        attributes.put("vip", "true");
        return attributes;
    }

    @Data
    public static class PerformanceTestRequest {
        private String experimentName;
        private Map<String, Object> userAttributes;
        private int iterations = 10000;
        private int threadCount = 1;
    }

    @Data
    @Builder
    public static class PerformanceTestResult {
        private String testName;
        private String experimentName;
        private int totalIterations;
        private int threadCount;
        private long totalDurationMs;
        private MethodStats getValueStats;
        private MethodStats getValueOrDefaultStats;
    }

    @Data
    @Builder
    public static class MethodStats {
        private String methodName;
        private int totalCalls;
        private long successCalls;
        private double totalTimeMs;
        private double avgTimeMs;
        private double minTimeMs;
        private double maxTimeMs;
        private double throughput;
    }
}
