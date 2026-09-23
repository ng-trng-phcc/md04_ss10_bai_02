package com.example.pharmacyservice.controller;

import com.example.pharmacyservice.model.dto.BillRequest;
import com.example.pharmacyservice.model.dto.BillResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@RefreshScope
@RestController
@RequestMapping("/api/v1/bill")
public class BillController {

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
