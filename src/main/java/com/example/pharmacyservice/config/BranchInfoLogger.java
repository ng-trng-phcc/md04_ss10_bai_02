package com.example.pharmacyservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BranchInfoLogger implements ApplicationRunner {

    @Value("${app.branch-name:N/A}")
    private String branchName;

    @Value("${app.hotline:N/A}")
    private String hotline;

    @Value("${spring.datasource.url:N/A}")
    private String datasourceUrl;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Pharmacy Service Started ===");
        log.info("Branch: {} | Hotline: {} | Datasource: {}", branchName, hotline, datasourceUrl);
        System.out.println(">>> Chi nhánh: " + branchName + " - Hotline: " + hotline + " - DB: " + datasourceUrl);
    }
}
