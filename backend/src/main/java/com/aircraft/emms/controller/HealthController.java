package com.aircraft.emms.controller;

import com.aircraft.emms.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> info = Map.of(
                "status", "UP",
                "version", appVersion,
                "application", "EMMS Lite Backend"
        );
        return ResponseEntity.ok(ApiResponse.ok(info));
    }
}
