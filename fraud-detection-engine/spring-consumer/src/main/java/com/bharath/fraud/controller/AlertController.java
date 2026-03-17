package com.bharath.fraud.controller;

import com.bharath.fraud.model.FraudAlert;
import com.bharath.fraud.service.AlertStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Fraud Alerts", description = "Query and manage fraud alerts")
public class AlertController {

    private final AlertStoreService alertStoreService;

    @GetMapping
    @Operation(summary = "Get recent fraud alerts")
    public ResponseEntity<List<FraudAlert>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String riskLevel) {
        return ResponseEntity.ok(alertStoreService.getAlerts(page, size, riskLevel));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get fraud detection statistics")
    public ResponseEntity<AlertStoreService.AlertStats> getStats() {
        return ResponseEntity.ok(alertStoreService.getStats());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Fraud Detection Service running");
    }
}
