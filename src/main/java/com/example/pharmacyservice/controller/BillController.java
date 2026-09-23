package com.example.pharmacyservice.controller;

import com.example.pharmacyservice.event.OrderEvent;
import com.example.pharmacyservice.event.OrderEventProducer;
import com.example.pharmacyservice.model.dto.BillRequest;
import com.example.pharmacyservice.model.dto.BillResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RefreshScope
@RestController
@RequestMapping("/api/v1/bill")
@RequiredArgsConstructor
public class BillController {

    private final OrderEventProducer orderEventProducer;

    @Value("${pharmacy.vat-rate:10}")
    private BigDecimal vatRate;

    @Value("${app.branch-name:Nha Thuoc So 1}")
    private String branchName;

    /**
     * Tính tiền hóa đơn thuốc: tổng tiền thuốc + % thuế VAT của tổng tiền thuốc
     * Công thức: total = drugTotal + drugTotal * vatRate / 100
     * vatRate lấy từ Git (pharmacy.vat-rate) và refresh không cần restart qua POST /actuator/refresh
     */
    @PostMapping
    public ResponseEntity<BillResponse> calculate(@Valid @RequestBody BillRequest request) {
        BigDecimal drugTotal = resolveDrugTotal(request);
        BigDecimal rate = vatRate != null ? vatRate : BigDecimal.valueOf(10);
        BigDecimal vatAmount = drugTotal.multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = drugTotal.add(vatAmount);

        log.info("Bill calculate - branch: {}, drugTotal: {}, vatRate: {}%, vatAmount: {}, total: {}",
                branchName, drugTotal, rate, vatAmount, total);

        BillResponse resp = BillResponse.builder()
                .drugTotal(drugTotal)
                .vatRate(rate)
                .vatAmount(vatAmount)
                .total(total)
                .branchName(branchName)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/vat-rate")
    public ResponseEntity<?> getVatRate() {
        return ResponseEntity.ok(java.util.Map.of(
                "vatRate", vatRate,
                "branchName", branchName
        ));
    }

    /**
     * Thanh toán - Khi nhấn nút "Thanh toán": tính tiền + gửi OrderEvent vào Kafka.
     * Mỗi BillItem sẽ tạo 1 OrderEvent với key = medicineId (lấy từ item.name).
     * Topic: medicine-stock-events, đảm bảo cùng medicineId -> cùng partition.
     * Sau khi gửi Kafka thành công, phản hồi "Thanh toán thành công".
     */
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@Valid @RequestBody BillRequest request) {
        // Tính bill như cũ
        BigDecimal drugTotal = resolveDrugTotal(request);
        BigDecimal rate = vatRate != null ? vatRate : BigDecimal.valueOf(10);
        BigDecimal vatAmount = drugTotal.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = drugTotal.add(vatAmount);

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> kafkaResults = new ArrayList<>();

        try {
            if (request.getItems() != null && !request.getItems().isEmpty()) {
                for (BillRequest.BillItem item : request.getItems()) {
                    // medicineId lấy từ item.name (giả định name chính là mã thuốc; nếu cần tách riêng thì thêm field medicineId)
                    String medicineId = item.getName();
                    OrderEvent event = OrderEvent.builder()
                            .orderId(orderId + "-" + medicineId)
                            .medicineId(medicineId)
                            .quantity(item.getQuantity())
                            .timestamp(now)
                            .build();
                    var result = orderEventProducer.sendOrderEventSync(event);
                    kafkaResults.add(Map.of(
                            "medicineId", medicineId,
                            "quantity", item.getQuantity(),
                            "partition", result.getRecordMetadata().partition(),
                            "offset", result.getRecordMetadata().offset()
                    ));
                    log.info("Bill checkout sent Kafka orderId={} medicineId={} partition={}", event.getOrderId(), medicineId, result.getRecordMetadata().partition());
                }
            } else {
                // Trường hợp chỉ có drugTotal: tạo 1 event generic
                OrderEvent event = OrderEvent.builder()
                        .orderId(orderId)
                        .medicineId("GENERIC")
                        .quantity(1)
                        .timestamp(now)
                        .build();
                var result = orderEventProducer.sendOrderEventSync(event);
                kafkaResults.add(Map.of("medicineId", "GENERIC", "partition", result.getRecordMetadata().partition(), "offset", result.getRecordMetadata().offset()));
            }
        } catch (Exception e) {
            log.error("Bill checkout failed to send Kafka: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Thanh toán thất bại - lỗi gửi Kafka",
                    "status", "FAILED",
                    "error", e.getMessage()
            ));
        }

        BillResponse bill = BillResponse.builder()
                .drugTotal(drugTotal)
                .vatRate(rate)
                .vatAmount(vatAmount)
                .total(total)
                .branchName(branchName)
                .timestamp(now)
                .build();

        return ResponseEntity.ok(Map.of(
                "message", "Thanh toán thành công",
                "status", "SUCCESS",
                "orderId", orderId,
                "bill", bill,
                "kafkaTopic", "medicine-stock-events",
                "kafkaEvents", kafkaResults
        ));
    }

    private BigDecimal resolveDrugTotal(BillRequest request) {
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            return request.getItems().stream()
                    .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (request.getDrugTotal() != null) {
            return request.getDrugTotal();
        }
        return BigDecimal.ZERO;
    }
}
