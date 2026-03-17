package com.bharath.fraud.service;

import com.bharath.fraud.model.FraudAlert;
import com.bharath.fraud.model.TransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock private WebClient inferenceWebClient;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private WebClient.RequestBodySpec requestBodySpec;
    @Mock private WebClient.ResponseSpec responseSpec;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private FraudDetectionService service;

    private TransactionEvent buildTransaction(double amount) {
        return TransactionEvent.builder()
                .transactionId("txn-001")
                .accountId("acc-123")
                .amount(amount)
                .merchant("Test Merchant")
                .merchantCategory("retail")
                .country("US")
                .currency("USD")
                .timestamp(Instant.now())
                .cardPresent(true)
                .hourOfDay(14)
                .dayOfWeek(2)
                .build();
    }

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.getAndExpire(anyString(), any())).thenReturn(null);
    }

    @Test
    void evaluate_shouldReturnFraudAlert_whenProbabilityAboveThreshold() {
        when(inferenceWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(FraudDetectionService.InferenceResponse.class))
                .thenReturn(Mono.just(new FraudDetectionService.InferenceResponse(0.95, "high_amount_unusual_location")));

        FraudAlert alert = service.evaluate(buildTransaction(15000.0));

        assertThat(alert.isFraudulent()).isTrue();
        assertThat(alert.getRiskLevel()).isEqualTo("CRITICAL");
        assertThat(alert.getFraudProbability()).isGreaterThanOrEqualTo(0.9);
    }

    @Test
    void evaluate_shouldReturnCleanAlert_whenProbabilityBelowThreshold() {
        when(inferenceWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(FraudDetectionService.InferenceResponse.class))
                .thenReturn(Mono.just(new FraudDetectionService.InferenceResponse(0.12, "normal_pattern")));

        FraudAlert alert = service.evaluate(buildTransaction(45.0));

        assertThat(alert.isFraudulent()).isFalse();
        assertThat(alert.getRiskLevel()).isEqualTo("LOW");
    }

    @Test
    void evaluate_shouldHandleInferenceTimeout_gracefully() {
        when(inferenceWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(FraudDetectionService.InferenceResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Timeout")));

        FraudAlert alert = service.evaluate(buildTransaction(100.0));

        assertThat(alert.isFraudulent()).isFalse();
        assertThat(alert.getAnomalyReason()).isEqualTo("inference_error");
    }
}
