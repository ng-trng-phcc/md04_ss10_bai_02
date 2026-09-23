package com.example.pharmacyservice.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillRequest {

    /**
     * Tổng tiền thuốc trước thuế. Nếu items được cung cấp thì sẽ tính từ items, ngược lại dùng field này.
     */
    @PositiveOrZero(message = "drugTotal must be >= 0")
    private BigDecimal drugTotal;

    private List<BillItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BillItem {
        @NotNull
        private String name;
        @NotNull
        @PositiveOrZero
        private BigDecimal price;
        @NotNull
        @PositiveOrZero
        private Integer quantity;
    }
}
