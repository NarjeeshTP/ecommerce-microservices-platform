package com.ecommerce.pricingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemId;
    private BigDecimal price;
    private String currency;
    private String source; // DATABASE or CACHE
    private Boolean discountApplied;
    private BigDecimal originalPrice;
}

