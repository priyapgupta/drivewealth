package com.drivewealth.configflow.controller;

import com.drivewealth.configflow.dto.FeatureEvalResponse;
import com.drivewealth.configflow.model.ConfigEntry;
import com.drivewealth.configflow.service.ConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
public class FeaturesController {
    private final ConfigService configService;

    public FeaturesController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/api/features")
    public ResponseEntity<List<FeatureEvalResponse>> allFeatures(@RequestParam(required = false) String userId) {
        List<ConfigEntry> features = configService.getAllFeatures();
        List<FeatureEvalResponse> resp = features.stream()
                .map(e -> new FeatureEvalResponse(
                        e.getKey(),
                        configService.isFeatureEnabled(e.getKey(), userId),
                        e.getRolloutPercent(),
                        e.getValue()
                ))
                .sorted(Comparator.comparing(FeatureEvalResponse::getKey))
                .toList();
        return ResponseEntity.ok(resp);
    }
}

