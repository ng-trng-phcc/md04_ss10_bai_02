package com.example.pharmacyservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillResponse {
    private BigDecimal drugTotal;
    private BigDecimal vatRate; // % ví dụ 10 = 10%
    private BigDecimal vatAmount; // drugTotal * vatRate / 100
    private BigDecimal total; // drugTotal + vatAmount
    private String branchName;
    private LocalDateTime timestamp;
}
