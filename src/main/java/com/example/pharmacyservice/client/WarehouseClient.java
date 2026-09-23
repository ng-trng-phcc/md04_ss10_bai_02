package com.example.pharmacyservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseClient {

    private final RestTemplate restTemplate;

    private static final String WAREHOUSE_STOCK_URL = "http://WAREHOUSE-SERVICE/api/v1/warehouse/stock/{productId}";
    private static final String WAREHOUSE_CHECK_URL = "http://WAREHOUSE-SERVICE/api/v1/warehouse/check";

    /**
     * Gọi warehouse-service để kiểm tra tồn kho.
     * Khi warehouse phản hồi chậm/sập, CircuitBreaker warehouseCB sẽ mở (failure-rate 50%, open 20s) để tránh treo máy tính tiền.
     */
    @CircuitBreaker(name = "warehouseCB", fallbackMethod = "checkWarehouseFallback")
    public Map<String, Object> checkStock(String productId) {
        log.info("Calling warehouse-service for productId: {}", productId);
        return restTemplate.getForObject(WAREHOUSE_STOCK_URL, Map.class, productId);
    }

    @CircuitBreaker(name = "warehouseCB", fallbackMethod = "checkWarehouseFallback")
    public Map<String, Object> checkStockWithQuantity(String productId, int quantity) {
        String url = WAREHOUSE_CHECK_URL + "?productId=" + productId + "&quantity=" + quantity;
        return restTemplate.getForObject(url, Map.class);
    }

    // === Yêu cầu: hàm checkWarehouseFallback(Exception e) ===
    /**
     * Fallback theo yêu cầu đề bài: trả về thông báo hàng đang kiểm tra và cho phép bán dựa trên tồn kho tại chỗ.
     */
    public String checkWarehouseFallback(Exception e) {
        log.warn("warehouseCB checkWarehouseFallback triggered, reason={}", e.toString());
        return "Không thể kết nối kho tổng. Hệ thống sẽ sử dụng dữ liệu tồn kho cục bộ để tiếp tục giao dịch";
    }

    /**
     * Overload cho Resilience4j: fallback phải cùng return type với method gốc (Map) và có signature (originalParams + Throwable).
     * Đăng ký fallbackMethod = "checkWarehouseFallback" sẽ gọi các overload này, bên trong delegate về hàm single-arg ở trên.
     */
    @SuppressWarnings("unused")
    public Map<String, Object> checkWarehouseFallback(String productId, Throwable ex) {
        Exception e = (ex instanceof Exception) ? (Exception) ex : new Exception(ex);
        String msg = checkWarehouseFallback(e);
        log.warn("warehouseCB fallback for productId={}, reason={}", productId, ex.toString());
        return Map.of(
                "productId", productId,
                "available", true,
                "fallback", true,
                "localStock", true,
                "message", msg
        );
    }

    @SuppressWarnings("unused")
    public Map<String, Object> checkWarehouseFallback(String productId, int quantity, Throwable ex) {
        Exception e = (ex instanceof Exception) ? (Exception) ex : new Exception(ex);
        String msg = checkWarehouseFallback(e);
        log.warn("warehouseCB fallbackCheck for productId={}, quantity={}, reason={}", productId, quantity, ex.toString());
        return Map.of(
                "productId", productId,
                "quantity", quantity,
                "available", true,
                "fallback", true,
                "localStock", true,
                "message", msg
        );
    }

    // Giữ fallback cũ để tương thích nếu cần
    @SuppressWarnings("unused")
    public Map<String, Object> fallback(String productId, Throwable ex) {
        return checkWarehouseFallback(productId, ex);
    }

    @SuppressWarnings("unused")
    public Map<String, Object> fallbackCheck(String productId, int quantity, Throwable ex) {
        return checkWarehouseFallback(productId, quantity, ex);
    }
}
