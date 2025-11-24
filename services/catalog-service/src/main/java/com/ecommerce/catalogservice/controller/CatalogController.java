package com.ecommerce.catalogservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class CatalogController {

    @GetMapping("/catalog/items")
    public List<Map<String, Object>> listItems() {
        return List.of(
                Map.of("sku", "SKU-003", "name", "Sample Item 1", "price", 19.99),
                Map.of("sku", "SKU-002", "name", "Sample Item 2", "price", 29.99)
        );
    }
}

