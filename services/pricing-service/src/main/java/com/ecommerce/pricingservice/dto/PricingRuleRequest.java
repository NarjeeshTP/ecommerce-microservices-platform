package com.ecommerce.pricingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRuleRequest {

    @NotBlank(message = "Item ID is required")
    private String itemId;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Base price must be greater than 0")
    private BigDecimal basePrice;

    @DecimalMin(value = "0.0", message = "Discount percent must be non-negative")
    @DecimalMax(value = "100.0", message = "Discount percent must not exceed 100")
    private BigDecimal discountPercent;

    @Pattern(regexp = "USD|EUR|GBP", message = "Currency must be USD, EUR, or GBP")
    private String currency;

    @Pattern(regexp = "STANDARD|SEASONAL|PROMOTIONAL|BULK", message = "Rule type must be STANDARD, SEASONAL, PROMOTIONAL, or BULK")
    private String ruleType;

    @Min(value = 1, message = "Minimum quantity must be at least 1")
    private Integer minQuantity;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    @Pattern(regexp = "ACTIVE|INACTIVE", message = "Status must be ACTIVE or INACTIVE")
    private String status;
}

