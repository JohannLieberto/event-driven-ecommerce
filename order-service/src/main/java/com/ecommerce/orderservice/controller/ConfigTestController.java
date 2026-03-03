package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.config.InventoryConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class ConfigTestController {

    private final InventoryConfig inventoryConfig;

    public ConfigTestController(InventoryConfig inventoryConfig) {
        this.inventoryConfig = inventoryConfig;
    }

    @GetMapping("/inventory-url")
    public String getInventoryUrl() {
        return inventoryConfig.getService().getUrl();
    }
}