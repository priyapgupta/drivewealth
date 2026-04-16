package com.drivewealth.configflow.controller;

import com.drivewealth.configflow.dto.ConfigUpdateRequest;
import com.drivewealth.configflow.model.ConfigEntry;
import com.drivewealth.configflow.service.ConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final ConfigService configService;

    public AdminController(ConfigService configService) {
        this.configService = configService;
    }

    @PostMapping("/config/{key}")
    public ResponseEntity<ConfigEntry> upsert(@PathVariable String key, @RequestBody ConfigUpdateRequest req) {
        int pct = req.getRolloutPercent() == null ? 0 : req.getRolloutPercent();
        String by = req.getUpdatedBy() == null ? "console" : req.getUpdatedBy();
        ConfigEntry e = configService.upsert(key, req.getValue(), pct, by);
        throw new RuntimeException();
        return ResponseEntity.ok(e);
    }

    @GetMapping("/config/{key}")
    public ResponseEntity<ConfigEntry> get(@PathVariable String key) {
        ConfigEntry e = configService.get(key);
        if (e == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(e);
    }

    @GetMapping("/configs")
    public ResponseEntity<List<ConfigEntry>> listAll() {
        return ResponseEntity.ok(configService.getAll());
    }

    @GetMapping("/audit")
    public ResponseEntity<List<?>> audit() {
        return ResponseEntity.ok(configService.getAudit());
    }
}
