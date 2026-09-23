package com.example.pharmacyservice.controller;

import com.example.pharmacyservice.event.OrderEvent;
import com.example.pharmacyservice.event.OrderEventProducer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * API bán thuốc - khi nhấn "Thanh toán" sẽ gửi OrderEvent vào Kafka.
 * Topic: medicine-stock-events, Key: medicineId (đảm bảo cùng thuốc → cùng partition).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderEventProducer orderEventProducer;

    /**
     * Checkout / Thanh toán: nhận OrderEvent và gửi Kafka, trả về thanh toán thành công.
     * Thử với curl:
     * curl -X POST http://localhost:8085/api/v1/orders/checkout -H "Content-Type: application/json" -d '{"orderId":"ORD-001","medicineId":"MED-001","quantity":2}'
     */
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@Valid @RequestBody CheckoutRequest request) {
        OrderEvent event = OrderEvent.builder()
                .orderId(request.getOrderId() != null ? request.getOrderId() : "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .medicineId(request.getMedicineId())
                .quantity(request.getQuantity())
                .timestamp(LocalDateTime.now())
                .build();

        try {
            SendResult<String, OrderEvent> result = orderEventProducer.sendOrderEventSync(event);
            log.info("Checkout success - orderId={} medicineId={} partition={} offset={}",
                    event.getOrderId(), event.getMedicineId(),
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());

            return ResponseEntity.ok(Map.of(
                    "message", "Thanh toán thành công",
                    "status", "SUCCESS",
                    "orderId", event.getOrderId(),
                    "medicineId", event.getMedicineId(),
                    "quantity", event.getQuantity(),
                    "timestamp", event.getTimestamp().toString(),
                    "kafkaTopic", "medicine-stock-events",
                    "kafkaKey", event.getMedicineId(),
                    "partition", result.getRecordMetadata().partition(),
                    "offset", result.getRecordMetadata().offset()
            ));
        } catch (Exception e) {
            log.error("Checkout failed orderId={} medicineId={}: {}", event.getOrderId(), event.getMedicineId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Thanh toán thất bại - lỗi gửi Kafka",
                    "status", "FAILED",
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * API bán thuốc đơn giản - alias của checkout, cũng gửi Kafka với key=medicineId.
     */
    @PostMapping("/sell")
    public ResponseEntity<Map<String, Object>> sell(@Valid @RequestBody CheckoutRequest request) {
        return checkout(request);
    }

    /**
     * Gửi trực tiếp OrderEvent (cho phép client gửi full event).
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> sendEvent(@Valid @RequestBody OrderEvent request) {
        OrderEvent event = OrderEvent.builder()
                .orderId(request.getOrderId() != null ? request.getOrderId() : "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .medicineId(request.getMedicineId())
                .quantity(request.getQuantity())
                .timestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now())
                .build();
        // reuse checkout logic
        CheckoutRequest cr = new CheckoutRequest();
        cr.setOrderId(event.getOrderId());
        cr.setMedicineId(event.getMedicineId());
        cr.setQuantity(event.getQuantity());
        return checkout(cr);
    }

    // DTO cho request thanh toán
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CheckoutRequest {
        private String orderId;

        @NotBlank(message = "medicineId is required")
        private String medicineId;

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be > 0")
        private Integer quantity;
    }
}
