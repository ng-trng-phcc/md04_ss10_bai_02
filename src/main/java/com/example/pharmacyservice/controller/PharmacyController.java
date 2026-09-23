package com.example.pharmacyservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RefreshScope
@RestController
@RequestMapping("/api/v1/pharmacy")
public class PharmacyController {

    @Value("${app.branch-name:Nha Thuoc So 1}")
    private String branchName;

    @Value("${app.hotline:19001234}")
    private String hotline;

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/pharmacy_db}")
    private String datasourceUrl;

    @GetMapping("/branch")
    public Map<String, String> getBranchInfo() {
        return Map.of(
                "branchName", branchName,
                "hotline", hotline,
                "datasourceUrl", datasourceUrl
        );
    }

    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of(
                "message", "Pharmacy Service - Chi nhánh: " + branchName,
                "hotline", hotline
        );
    }
}
