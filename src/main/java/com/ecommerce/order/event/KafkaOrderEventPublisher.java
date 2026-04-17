package com.ecommerce.order.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka-based event publisher — bridges Spring domain events to Kafka topics.
 *
 * Activated ONLY when the 'kafka' profile is enabled.
 * Without it, domain events are still processed by OrderEventListener (in-process).
 * With it, events are ALSO published to Kafka for external consumers
 * (analytics services, notification microservices, data pipelines).
 *
 * This demonstrates the Adapter pattern: same domain events, different transport.
 */
@Component
@Profile("kafka")
public class KafkaOrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);
    private static final String ORDER_EVENTS_TOPIC = "order-events";
    private static final String STATUS_CHANGES_TOPIC = "order-status-changes";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaOrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                    ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "ORDER_CREATED");
        payload.put("orderId", event.getOrderId());
        payload.put("customerEmail", event.getCustomerEmail());
        payload.put("itemCount", event.getItemCount());
        payload.put("totalAmount", event.getTotalAmount());
        payload.put("occurredAt", event.getOccurredAt().toString());

        publish(ORDER_EVENTS_TOPIC, String.valueOf(event.getOrderId()), payload);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "STATUS_CHANGED");
        payload.put("orderId", event.getOrderId());
        payload.put("previousStatus", event.getPreviousStatus().name());
        payload.put("newStatus", event.getNewStatus().name());
        payload.put("reason", event.getReason());
        payload.put("occurredAt", event.getOccurredAt().toString());

        publish(STATUS_CHANGES_TOPIC, String.valueOf(event.getOrderId()), payload);
    }

    private void publish(String topic, String key, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json)
                    .addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                        @Override
                        public void onSuccess(SendResult<String, String> result) {
                            log.info("[KAFKA] Published to {}  key={} offset={}",
                                    topic, key, result.getRecordMetadata().offset());
                        }

                        @Override
                        public void onFailure(Throwable ex) {
                            log.error("[KAFKA] Failed to publish to {} key={}", topic, key, ex);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("[KAFKA] Failed to serialize event for topic={} key={}", topic, key, e);
        }
    }
}
