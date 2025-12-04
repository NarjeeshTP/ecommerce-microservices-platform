package com.ecommerce.pricingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRuleResponse {
    private Long id;
    private String itemId;
    private BigDecimal basePrice;
    private BigDecimal discountPercent;
    private BigDecimal finalPrice;
    private String currency;
    private String ruleType;
    private Integer minQuantity;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

