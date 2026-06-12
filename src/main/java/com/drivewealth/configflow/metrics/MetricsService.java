package com.drivewealth.configflow.metrics;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MetricsService {
    private final ConcurrentHashMap<String, RequestMetricsEntry> metricsByPath = new ConcurrentHashMap<>();

    public void recordRequest(String path, long latencyMs, int statusCode) {
        String normalizedPath = normalizePath(path);
        RequestMetricsEntry entry = metricsByPath.computeIfAbsent(normalizedPath, RequestMetricsEntry::new);
        entry.record(latencyMs, statusCode);
    }

    public Map<String, Object> getLatencyMetrics() {
        Map<String, Object> latencyByPath = metricsByPath.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            RequestMetricsEntry metric = entry.getValue();
                            return Map.of(
                                    "requestCount", metric.getRequestCount(),
                                    "averageLatencyMs", metric.getAverageLatencyMs(),
                                    "minLatencyMs", metric.getMinLatencyMs(),
                                    "maxLatencyMs", metric.getMaxLatencyMs()
                            );
                        }
                ));

        long totalRequests = metricsByPath.values().stream().mapToLong(RequestMetricsEntry::getRequestCount).sum();
        long totalLatencyMs = metricsByPath.values().stream().mapToLong(RequestMetricsEntry::getTotalLatencyMs).sum();

        return Map.of(
                "totalRequests", totalRequests,
                "totalLatencyMs", totalLatencyMs,
                "averageLatencyMs", totalRequests == 0 ? 0 : ((double) totalLatencyMs) / totalRequests,
                "latencyByPath", latencyByPath
        );
    }

    public Map<String, Object> getErrorMetrics() {
        Map<String, Long> errorsByPath = metricsByPath.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getErrorCount()
                ));

        long totalErrors = errorsByPath.values().stream().mapToLong(Long::longValue).sum();

        return Map.of(
                "totalErrors", totalErrors,
                "errorsByPath", errorsByPath
        );
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path;
    }
}
