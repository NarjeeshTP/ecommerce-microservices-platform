package com.ecommerce.pricingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String itemId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountPercent;

    @Column(precision = 10, scale = 2)
    private BigDecimal finalPrice;

    @Column(length = 50)
    private String currency;

    @Column(name = "rule_type", length = 50)
    private String ruleType; // STANDARD, SEASONAL, PROMOTIONAL, BULK

    @Column(name = "min_quantity")
    private Integer minQuantity; // For bulk pricing

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(length = 20)
    private String status; // ACTIVE, INACTIVE, EXPIRED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currency == null) {
            currency = "USD";
        }
        if (status == null) {
            status = "ACTIVE";
        }
        if (ruleType == null) {
            ruleType = "STANDARD";
        }
        calculateFinalPrice();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateFinalPrice();
    }

    private void calculateFinalPrice() {
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = basePrice.multiply(discountPercent).divide(new BigDecimal("100"));
            this.finalPrice = basePrice.subtract(discount);
        } else {
            this.finalPrice = basePrice;
        }
    }
}

