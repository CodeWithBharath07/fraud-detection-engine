package com.bharath.fraud.consumer;

import com.bharath.fraud.model.FraudAlert;
import com.bharath.fraud.model.TransactionEvent;
import com.bharath.fraud.service.FraudDetectionService;
import com.bharath.fraud.websocket.FraudAlertBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionKafkaConsumer {

    private final FraudDetectionService fraudDetectionService;
    private final FraudAlertBroadcaster alertBroadcaster;

    @KafkaListener(
            topics = "${kafka.topic.transactions:transactions}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, TransactionEvent> record, Acknowledgment ack) {
        TransactionEvent event = record.value();
        log.debug("Received transaction: {} amount={} account={}",
                event.getTransactionId(), event.getAmount(), event.getAccountId());

        try {
            FraudAlert alert = fraudDetectionService.evaluate(event);

            if (alert.isFraudulent()) {
                log.warn("FRAUD DETECTED: txn={} prob={:.2f} reason={}",
                        alert.getTransactionId(), alert.getFraudProbability(), alert.getAnomalyReason());
                alertBroadcaster.broadcast(alert);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing transaction {}: {}", event.getTransactionId(), e.getMessage());
            // Don't ack — let Kafka retry
        }
    }
}
