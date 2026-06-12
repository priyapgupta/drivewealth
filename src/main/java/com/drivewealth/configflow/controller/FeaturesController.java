package com.drivewealth.configflow.controller;

import com.drivewealth.configflow.dto.FeatureEvalResponse;
import com.drivewealth.configflow.model.ConfigEntry;
import com.drivewealth.configflow.service.ConfigService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
public class FeaturesController {
    private final ConfigService configService;
    private final Tracer tracer;

    public FeaturesController(ConfigService configService, Tracer tracer) {
        this.configService = configService;
        this.tracer = tracer;
    }

    @GetMapping("/api/features")
    public ResponseEntity<List<FeatureEvalResponse>> allFeatures(@RequestParam(required = false) String userId) {
        Span span = tracer.spanBuilder("features.list")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.route", "/api/features");
            span.setAttribute("http.target", "/api/features?userId=" + (userId == null ? "anonymous" : userId));
            span.setAttribute("features.userId", userId == null ? "anonymous" : userId);
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
            span.setAttribute("features.count", resp.size());
            span.setStatus(StatusCode.OK);
            return ResponseEntity.ok(resp);
        } finally {
            span.end();
        }
    }
}

