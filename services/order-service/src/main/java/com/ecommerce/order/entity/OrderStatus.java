package com.ecommerce.order.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAYMENT_CONFIRMED,
    PROCESSING,
    COMPLETED,
    CANCELLED;

    private List<OrderStatus> allowedTransitions;

    static {
        CREATED.allowedTransitions = Arrays.asList(PAYMENT_PENDING, CANCELLED);
        PAYMENT_PENDING.allowedTransitions = Arrays.asList(PAYMENT_CONFIRMED, CANCELLED);
        PAYMENT_CONFIRMED.allowedTransitions = Arrays.asList(PROCESSING, CANCELLED);
        PROCESSING.allowedTransitions = Arrays.asList(COMPLETED, CANCELLED);
        COMPLETED.allowedTransitions = new ArrayList<>(); // Terminal state
        CANCELLED.allowedTransitions = new ArrayList<>(); // Terminal state
    }

    public boolean canTransitionTo(OrderStatus newStatus) {
        return allowedTransitions.contains(newStatus);
    }

    public List<OrderStatus> getAllowedTransitions() {
        return allowedTransitions;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}

