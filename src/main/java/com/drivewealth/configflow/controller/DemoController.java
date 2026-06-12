package com.drivewealth.configflow.controller;

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

import java.util.HashMap;
import java.util.Map;

@RestController
public class DemoController {
    private final ConfigService configService;
    private final Tracer tracer;

    public DemoController(ConfigService configService, Tracer tracer) {
        this.configService = configService;
        this.tracer = tracer;
    }

    // Demo endpoint showing behavior toggled by a feature flag
    @GetMapping("/api/demo")
    public ResponseEntity<?> demo(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String feature
    ) {
        Span span = tracer.spanBuilder("demo.feature.eval")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            String key = (feature == null || feature.isBlank()) ? "feature.newCheckout" : feature.trim();
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.route", "/api/demo");
            span.setAttribute("http.target", "/api/demo?feature=" + key + "&userId=" + (userId == null ? "anonymous" : userId));
            span.setAttribute("demo.userId", userId == null ? "anonymous" : userId);
            span.setAttribute("demo.feature", key);
            if (!key.contains("feature")) {
                span.setAttribute("demo.invalidFeature", true);
                span.setStatus(StatusCode.ERROR, "invalid feature key");
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "feature key must contain 'feature'",
                        "feature", key
                ));
            }

            boolean newBehavior = configService.isFeatureEnabled(key, userId);
            Map<String, Object> resp = new HashMap<>();
            resp.put("userId", userId);
            resp.put("feature", key);
            resp.put("enabled", newBehavior);
            resp.put("result", newBehavior ? "NEW checkout flow" : "OLD checkout flow");
            span.setStatus(StatusCode.OK);
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, "Demo evaluation exception");
            span.recordException(ex);
            throw ex;
        } finally {
            span.end();
        }
    }
}
