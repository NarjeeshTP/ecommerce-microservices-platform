package com.ecommerce.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_reservations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "order_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "reservation_id", unique = true, nullable = false)
    private String reservationId;

    @Column(name = "product_id", nullable = false, length = 100)
    private String productId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE && !isExpired();
    }

    public void release() {
        this.status = ReservationStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = ReservationStatus.EXPIRED;
        this.releasedAt = LocalDateTime.now();
    }

    public void commit() {
        this.status = ReservationStatus.COMMITTED;
    }
}

