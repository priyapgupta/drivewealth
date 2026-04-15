package com.drivewealth.configflow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class RootController {
    @GetMapping("/api/health")
    public ResponseEntity<?> health() {
        Map<String, Object> m = new HashMap<>();
        m.put("service", "configflow-poc");
        m.put("status", "running");
        m.put("endpoints", new String[]{"/", "/api/demo", "/api/features", "/admin/configs", "/h2-console"});
        return ResponseEntity.ok(m);
    }
}
