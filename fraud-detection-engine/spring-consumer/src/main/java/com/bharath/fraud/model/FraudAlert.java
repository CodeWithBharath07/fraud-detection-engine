package com.bharath.fraud.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlert {

    private String alertId;
    private String transactionId;
    private String accountId;
    private double amount;
    private String merchant;
    private String country;

    private boolean fraudulent;
    private double fraudProbability;
    private String riskLevel;         // CRITICAL, HIGH, MEDIUM, LOW
    private String anomalyReason;

    private long inferenceLatencyMs;
    private Instant detectedAt;
}
