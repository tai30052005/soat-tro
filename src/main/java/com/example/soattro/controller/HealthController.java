package com.example.soattro.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint kiểm tra "app còn sống không" — dùng cho frontend, Docker healthcheck
 * và dịch vụ deploy (Render ping để đánh thức instance free).
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "soat-tro-api");
    }
}
