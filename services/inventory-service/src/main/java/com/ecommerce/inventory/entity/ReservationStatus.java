package com.ecommerce.inventory.entity;

public enum ReservationStatus {
    ACTIVE,      // Reservation is active
    RELEASED,    // Manually released
    EXPIRED,     // TTL expired
    COMMITTED    // Order completed, stock permanently removed
}

