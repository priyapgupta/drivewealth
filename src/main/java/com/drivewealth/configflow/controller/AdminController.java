package com.drivewealth.configflow.controller;

import com.drivewealth.configflow.dto.ConfigUpdateRequest;
import com.drivewealth.configflow.model.ConfigEntry;
import com.drivewealth.configflow.service.ConfigService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final ConfigService configService;
    private final Tracer tracer;

    public AdminController(ConfigService configService, Tracer tracer) {
        this.configService = configService;
        this.tracer = tracer;
    }

    @PostMapping("/config/{key}")
    public ResponseEntity<ConfigEntry> upsert(@PathVariable String key, @RequestBody ConfigUpdateRequest req) {
        Span span = tracer.spanBuilder("admin.config.upsert")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            int pct = req.getRolloutPercent() == null ? 0 : req.getRolloutPercent();
            String by = req.getUpdatedBy() == null ? "console" : req.getUpdatedBy();
            span.setAttribute("http.method", "POST");
            span.setAttribute("http.route", "/admin/config/{key}");
            span.setAttribute("http.target", "/admin/config/" + key);
            span.setAttribute("config.key", key);
            span.setAttribute("config.rolloutPercent", pct);
            span.setAttribute("config.updatedBy", by);
            ConfigEntry e = configService.upsert(key, req.getValue(), pct, by);
            span.setStatus(StatusCode.OK);
            return ResponseEntity.ok(e);
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, "Exception in admin config upsert");
            span.recordException(ex);
            throw ex;
        } finally {
            span.end();
        }
    }

    @GetMapping("/config/{key}")
    public ResponseEntity<ConfigEntry> get(@PathVariable String key) {
        Span span = tracer.spanBuilder("admin.config.get")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.route", "/admin/config/{key}");
            span.setAttribute("http.target", "/admin/config/" + key);
            span.setAttribute("config.key", key);
            ConfigEntry e = configService.get(key);
            if (e == null) {
                span.setStatus(StatusCode.ERROR, "not found");
                return ResponseEntity.notFound().build();
            }
            span.setStatus(StatusCode.OK);
            return ResponseEntity.ok(e);
        } finally {
            span.end();
        }
    }

    @GetMapping("/configs")
    public ResponseEntity<List<ConfigEntry>> listAll() {
        Span span = tracer.spanBuilder("admin.config.list")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.route", "/admin/configs");
            span.setStatus(StatusCode.OK);
            return ResponseEntity.ok(configService.getAll());
        } finally {
            span.end();
        }
    }

    @GetMapping("/audit")
    public ResponseEntity<List<?>> audit() {
        Span span = tracer.spanBuilder("admin.audit.list")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.route", "/admin/audit");
            span.setStatus(StatusCode.OK);
            return ResponseEntity.ok(configService.getAudit());
        } finally {
            span.end();
        }
    }
}
