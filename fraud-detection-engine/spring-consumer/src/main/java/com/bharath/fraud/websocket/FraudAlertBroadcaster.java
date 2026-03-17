package com.bharath.fraud.websocket;

import com.bharath.fraud.model.FraudAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudAlertBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(FraudAlert alert) {
        log.info("Broadcasting fraud alert: {} risk={}", alert.getAlertId(), alert.getRiskLevel());
        messagingTemplate.convertAndSend("/topic/alerts", alert);
    }
}
