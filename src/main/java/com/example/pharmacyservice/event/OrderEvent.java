package com.example.pharmacyservice.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Sự kiện đơn hàng gửi qua Kafka khi nhân viên bấm "Thanh toán".
 * Gửi vào topic medicine-stock-events với key = medicineId để đảm bảo
 * cùng medicineId luôn vào cùng partition (giữ thứ tự per-medicine).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent {
    /** Mã đơn hàng duy nhất */
    private String orderId;

    /** Mã thuốc - dùng làm Kafka message key */
    private String medicineId;

    /** Số lượng bán */
    private Integer quantity;

    /** Thời điểm tạo sự kiện */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
