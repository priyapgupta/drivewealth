package com.drivewealth.configflow.service;

import com.drivewealth.configflow.model.AuditEntry;
import com.drivewealth.configflow.model.ConfigEntry;
import com.drivewealth.configflow.repo.AuditEntryRepository;
import com.drivewealth.configflow.repo.ConfigEntryRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;

@Service
public class ConfigService {
    private final ConfigEntryRepository configRepo;
    private final AuditEntryRepository auditRepo;
    private final Tracer tracer;

    public ConfigService(ConfigEntryRepository configRepo, AuditEntryRepository auditRepo, Tracer tracer) {
        this.configRepo = configRepo;
        this.auditRepo = auditRepo;
        this.tracer = tracer;
    }

    public ConfigEntry get(String key) {
        Span span = tracer.spanBuilder("config.get")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("config.key", key);
            return configRepo.findById(key).orElse(null);
        } finally {
            span.end();
        }
    }

    public List<ConfigEntry> getAll() {
        Span span = tracer.spanBuilder("config.listAll")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            return configRepo.findAll();
        } finally {
            span.end();
        }
    }

    public List<AuditEntry> getAudit() {
        Span span = tracer.spanBuilder("config.auditList")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            return auditRepo.findAll();
        } finally {
            span.end();
        }
    }

    @Transactional
    public ConfigEntry upsert(String key, String value, int rolloutPercent, String updatedBy) {
        Span span = tracer.spanBuilder("config.upsert")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("config.key", key);
            span.setAttribute("config.rolloutPercent", rolloutPercent);
            span.setAttribute("config.updatedBy", updatedBy);
            long ver = System.currentTimeMillis();
            ConfigEntry e = configRepo.findById(key).orElseGet(() -> new ConfigEntry(key, value, rolloutPercent, updatedBy, ver));
            e.setValue(value);
            e.setRolloutPercent(rolloutPercent);
            e.setUpdatedBy(updatedBy);
            e.setUpdatedAt(Instant.now());
            e.setVersion(ver);
            configRepo.save(e);
            auditRepo.save(new AuditEntry(key, value, rolloutPercent, updatedBy, ver));
            span.setStatus(StatusCode.OK);
            return e;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, "config.upsert failed");
            span.recordException(ex);
            throw ex;
        } finally {
            span.end();
        }
    }

    public boolean isFeatureEnabled(String key, String userId) {
        Span span = tracer.spanBuilder("config.featureEvaluation")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("config.key", key);
            span.setAttribute("config.userId", userId == null ? "anonymous" : userId);
            ConfigEntry e = configRepo.findById(key).orElse(null);
            if (e == null) {
                span.setAttribute("config.featureMissing", true);
                span.setStatus(StatusCode.OK);
                return false;
            }
            String val = e.getValue();
            if (val == null || !val.trim().toLowerCase(Locale.ROOT).equals("true")) {
                span.setAttribute("config.featureEnabled", false);
                span.setStatus(StatusCode.OK);
                return false;
            }
            int pct = e.getRolloutPercent();
            span.setAttribute("config.rolloutPercent", pct);
            if (pct <= 0) {
                span.setAttribute("config.featureEnabled", false);
                span.setStatus(StatusCode.OK);
                return false;
            }
            if (pct >= 100) {
                span.setAttribute("config.featureEnabled", true);
                span.setStatus(StatusCode.OK);
                return true;
            }
            String seed = (userId == null ? "" : userId) + "|" + key;
            CRC32 crc = new CRC32();
            crc.update(seed.getBytes(StandardCharsets.UTF_8));
            long v = Math.abs(crc.getValue());
            int bucket = (int) (v % 100);
            boolean enabled = bucket < pct;
            span.setAttribute("config.featureRolloutBucket", bucket);
            span.setAttribute("config.featureEnabled", enabled);
            span.setStatus(StatusCode.OK);
            return enabled;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, "feature evaluation failed");
            span.recordException(ex);
            throw ex;
        } finally {
            span.end();
        }
    }

    public List<ConfigEntry> getAllFeatures() {
        Span span = tracer.spanBuilder("config.listFeatures").startSpan();
        try (Scope scope = span.makeCurrent()) {
            return configRepo.findAll().stream()
                    .filter(e -> {
                        String k = e.getKey();
                        if (k == null) return false;
                        return k.startsWith("feature.") || k.contains(".feature.");
                    })
                    .toList();
        } finally {
            span.end();
        }
    }
}
