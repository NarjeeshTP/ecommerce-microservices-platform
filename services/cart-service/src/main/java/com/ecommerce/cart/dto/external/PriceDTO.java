package com.ecommerce.cart.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceDTO {
    private String productId;
    private BigDecimal finalPrice;
    private BigDecimal basePrice;
    private BigDecimal discount;
    private String currency;
    private LocalDateTime effectiveDate;
}

