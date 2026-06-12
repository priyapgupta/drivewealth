package com.drivewealth.configflow.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class RequestMetricsEntry {
    private final String path;
    private final LongAdder requestCount = new LongAdder();
    private final LongAdder totalLatencyMs = new LongAdder();
    private final AtomicLong maxLatencyMs = new AtomicLong(0);
    private final AtomicLong minLatencyMs = new AtomicLong(Long.MAX_VALUE);
    private final LongAdder errorCount = new LongAdder();

    public RequestMetricsEntry(String path) {
        this.path = path;
    }

    public void record(long latencyMs, int statusCode) {
        requestCount.increment();
        totalLatencyMs.add(latencyMs);
        updateMax(latencyMs);
        updateMin(latencyMs);
        if (statusCode >= 500) {
            errorCount.increment();
        }
    }

    private void updateMax(long latencyMs) {
        maxLatencyMs.updateAndGet(current -> Math.max(current, latencyMs));
    }

    private void updateMin(long latencyMs) {
        minLatencyMs.updateAndGet(current -> Math.min(current, latencyMs));
    }

    public String getPath() {
        return path;
    }

    public long getRequestCount() {
        return requestCount.sum();
    }

    public long getTotalLatencyMs() {
        return totalLatencyMs.sum();
    }

    public long getMaxLatencyMs() {
        return maxLatencyMs.get();
    }

    public long getMinLatencyMs() {
        long min = minLatencyMs.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    public long getErrorCount() {
        return errorCount.sum();
    }

    public double getAverageLatencyMs() {
        long count = getRequestCount();
        return count == 0 ? 0 : ((double) getTotalLatencyMs()) / count;
    }
}
