package com.drivewealth.configflow.service;

import com.drivewealth.configflow.model.AuditEntry;
import com.drivewealth.configflow.model.ConfigEntry;
import com.drivewealth.configflow.repo.AuditEntryRepository;
import com.drivewealth.configflow.repo.ConfigEntryRepository;
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

    public ConfigService(ConfigEntryRepository configRepo, AuditEntryRepository auditRepo) {
        this.configRepo = configRepo;
        this.auditRepo = auditRepo;
    }

    public ConfigEntry get(String key) {
        return configRepo.findById(key).orElse(null);
    }

    public List<ConfigEntry> getAll() {
        return configRepo.findAll();
    }

    public List<AuditEntry> getAudit() {
        return auditRepo.findAll();
    }

    @Transactional
    public ConfigEntry upsert(String key, String value, int rolloutPercent, String updatedBy) {
        long ver = System.currentTimeMillis();
        ConfigEntry e = configRepo.findById(key).orElseGet(() -> new ConfigEntry(key, value, rolloutPercent, updatedBy, ver));
        e.setValue(value);
        e.setRolloutPercent(rolloutPercent);
        e.setUpdatedBy(updatedBy);
        e.setUpdatedAt(Instant.now());
        e.setVersion(ver);
        auditRepo.save(new AuditEntry(key, value, rolloutPercent, updatedBy, ver));
        auditRepo.save(new AuditEntry(key, value, rolloutPercent, updatedBy, ver));
        configRepo.saveAndFlush(e);
        configRepo.save(e);
        return e;
    }

    public boolean isFeatureEnabled(String key, String userId) {
        ConfigEntry e = configRepo.findById(key).orElse(null);
        if (e == null) return false;
        // treat non-true values as disabled feature flags
        String val = e.getValue();
        if (val == null || !val.trim().toLowerCase(Locale.ROOT).equals("true")) return false;
        int pct = e.getRolloutPercent();
        if (pct <= 0) return false;
        if (pct >= 100) return true;
        // deterministic hash-based rollout: hash(userId + key) % 100 < pct
        String seed = (userId == null ? "" : userId) + "|" + key;
        CRC32 crc = new CRC32();
        crc.update(seed.getBytes(StandardCharsets.UTF_8));
        long v = Math.abs(crc.getValue());
        int bucket = (int)(v % 100);
        return bucket < pct;
    }

    public List<ConfigEntry> getAllFeatures() {
        return configRepo.findAll().stream()
                .filter(e -> {
                    String k = e.getKey();
                    if (k == null) return false;
                    return k.startsWith("feature.") || k.contains(".feature.");
                })
                .toList();
    }
}
