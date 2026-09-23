package com.example.pharmacyservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Producer gửi OrderEvent vào topic medicine-stock-events.
 * Sử dụng message key = medicineId để đảm bảo cùng medicineId → cùng partition.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    private static final String TOPIC_STOCK = "medicine-stock-events";

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    /**
     * Gửi OrderEvent với key = medicineId.
     * partition được Kafka tính: hash(key) % numPartitions (3 partitions cho stock-events).
     */
    public CompletableFuture<SendResult<String, OrderEvent>> sendOrderEvent(OrderEvent event) {
        String key = event.getMedicineId();
        log.info("Sending OrderEvent orderId={}, medicineId={}, quantity={}, timestamp={} -> topic={}, key={}",
                event.getOrderId(), event.getMedicineId(), event.getQuantity(), event.getTimestamp(), TOPIC_STOCK, key);

        CompletableFuture<SendResult<String, OrderEvent>> future = kafkaTemplate.send(TOPIC_STOCK, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send OrderEvent orderId={} medicineId={}: {}",
                        event.getOrderId(), event.getMedicineId(), ex.getMessage(), ex);
            } else {
                log.info("OrderEvent sent success orderId={} medicineId={} partition={} offset={}",
                        event.getOrderId(), event.getMedicineId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });

        return future;
    }

    /**
     * Đồng bộ - chờ kết quả (dùng khi cần đảm bảo gửi trước khi response).
     */
    public SendResult<String, OrderEvent> sendOrderEventSync(OrderEvent event) throws Exception {
        return kafkaTemplate.send(TOPIC_STOCK, event.getMedicineId(), event).get();
    }
}
