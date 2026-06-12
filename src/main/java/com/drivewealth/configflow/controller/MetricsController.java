package com.drivewealth.configflow.controller;

import com.drivewealth.configflow.metrics.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {
    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/errors")
    public ResponseEntity<Map<String, Object>> getErrorMetrics() {
        return ResponseEntity.ok(metricsService.getErrorMetrics());
    }

    @GetMapping("/latency")
    public ResponseEntity<Map<String, Object>> getLatencyMetrics() {
        return ResponseEntity.ok(metricsService.getLatencyMetrics());
    }
}
