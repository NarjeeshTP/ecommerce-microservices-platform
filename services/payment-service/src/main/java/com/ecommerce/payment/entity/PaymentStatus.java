package com.ecommerce.payment.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum PaymentStatus {
    INITIATED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    EXPIRED;

    private List<PaymentStatus> allowedTransitions;

    static {
        INITIATED.allowedTransitions = Arrays.asList(PROCESSING, CANCELLED, EXPIRED);
        PROCESSING.allowedTransitions = Arrays.asList(COMPLETED, FAILED, CANCELLED);
        COMPLETED.allowedTransitions = new ArrayList<>(); // Terminal state
        FAILED.allowedTransitions = new ArrayList<>(); // Terminal state
        CANCELLED.allowedTransitions = new ArrayList<>(); // Terminal state
        EXPIRED.allowedTransitions = new ArrayList<>(); // Terminal state
    }

    public boolean canTransitionTo(PaymentStatus newStatus) {
        return allowedTransitions.contains(newStatus);
    }

    public List<PaymentStatus> getAllowedTransitions() {
        return allowedTransitions;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == EXPIRED;
    }
}

