package com.bharath.fraud.service;

import com.bharath.fraud.model.FraudAlert;
import com.bharath.fraud.model.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final WebClient inferenceWebClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${fraud.threshold:0.7}")
    private double fraudThreshold;

    private static final String FEATURE_CACHE_PREFIX = "features:";

    public FraudAlert evaluate(TransactionEvent event) {
        long start = System.currentTimeMillis();

        // Build feature vector and call inference service
        InferenceResult result = callInferenceService(event);

        FraudAlert alert = FraudAlert.builder()
                .alertId(UUID.randomUUID().toString())
                .transactionId(event.getTransactionId())
                .accountId(event.getAccountId())
                .amount(event.getAmount())
                .merchant(event.getMerchant())
                .country(event.getCountry())
                .fraudulent(result.fraudProbability() >= fraudThreshold)
                .fraudProbability(result.fraudProbability())
                .riskLevel(toRiskLevel(result.fraudProbability()))
                .anomalyReason(result.reason())
                .inferenceLatencyMs(System.currentTimeMillis() - start)
                .detectedAt(Instant.now())
                .build();

        // Cache recent account features for velocity checks
        cacheAccountFeatures(event);

        return alert;
    }

    private InferenceResult callInferenceService(TransactionEvent event) {
        // Enrich with velocity features from Redis cache
        Long recentTxnCount = redisTemplate.opsForValue()
                .getAndExpire(FEATURE_CACHE_PREFIX + event.getAccountId(), Duration.ofMinutes(60)) != null
                ? getLongOrZero(redisTemplate.opsForValue().get(FEATURE_CACHE_PREFIX + "count:" + event.getAccountId()))
                : 0L;

        Map<String, Object> features = Map.of(
                "amount", event.getAmount(),
                "hour_of_day", event.getHourOfDay(),
                "day_of_week", event.getDayOfWeek(),
                "card_present", event.isCardPresent() ? 1 : 0,
                "merchant_category", event.getMerchantCategory() != null ? event.getMerchantCategory() : "unknown",
                "country", event.getCountry() != null ? event.getCountry() : "US",
                "currency", event.getCurrency() != null ? event.getCurrency() : "USD",
                "recent_txn_count", recentTxnCount
        );

        try {
            InferenceResponse response = inferenceWebClient.post()
                    .uri("/predict")
                    .bodyValue(Map.of("transaction_id", event.getTransactionId(), "features", features))
                    .retrieve()
                    .bodyToMono(InferenceResponse.class)
                    .timeout(Duration.ofMillis(100))  // sub-100ms SLA
                    .block();

            return new InferenceResult(response.fraud_probability(), response.reason());
        } catch (Exception e) {
            log.error("Inference service error for txn {}: {}", event.getTransactionId(), e.getMessage());
            return new InferenceResult(0.0, "inference_error");
        }
    }

    private void cacheAccountFeatures(TransactionEvent event) {
        String countKey = FEATURE_CACHE_PREFIX + "count:" + event.getAccountId();
        redisTemplate.opsForValue().increment(countKey);
        redisTemplate.expire(countKey, 60, TimeUnit.MINUTES);
    }

    private String toRiskLevel(double prob) {
        if (prob >= 0.9) return "CRITICAL";
        if (prob >= 0.7) return "HIGH";
        if (prob >= 0.5) return "MEDIUM";
        return "LOW";
    }

    private long getLongOrZero(Object val) {
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }

    public record InferenceResult(double fraudProbability, String reason) {}
    public record InferenceResponse(double fraud_probability, String reason) {}
}
