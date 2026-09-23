package com.example.pharmacyservice.controller;

import com.example.pharmacyservice.client.WarehouseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pharmacy/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseClient warehouseClient;

    /**
     * Bán lẻ gọi kho tổng kiểm tra hàng tồn.
     * Nếu warehouse chậm/sập, warehouseCB sẽ ngắt mạch 20s.
     */
    @GetMapping("/stock/{productId}")
    public ResponseEntity<Map<String, Object>> checkStock(@PathVariable String productId) {
        return ResponseEntity.ok(warehouseClient.checkStock(productId));
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkWithQuantity(
            @RequestParam String productId,
            @RequestParam(defaultValue = "1") int quantity) {
        return ResponseEntity.ok(warehouseClient.checkStockWithQuantity(productId, quantity));
    }
}
