package com.ecommerce.pricingservice.exception;

public class PricingRuleNotFoundException extends RuntimeException {
    public PricingRuleNotFoundException(String message) {
        super(message);
    }
}

