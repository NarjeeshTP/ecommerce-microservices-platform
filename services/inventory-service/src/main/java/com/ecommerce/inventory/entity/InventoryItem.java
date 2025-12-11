package com.ecommerce.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "product_id", unique = true, nullable = false, length = 100)
    private String productId;

    @Column(name = "available_quantity", nullable = false)
    private Long availableQuantity;

    @Builder.Default
    @Column(name = "reserved_quantity", nullable = false)
    private Long reservedQuantity = 0L;

    @Column(name = "total_quantity", nullable = false)
    private Long totalQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Builder.Default
    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 10;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Business methods
    public boolean canReserve(long quantity) {
        return availableQuantity >= quantity;
    }

    public void reserve(long quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException(
                String.format("Insufficient stock. Available: %d, Requested: %d",
                    availableQuantity, quantity)
            );
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }

    public void release(long quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                String.format("Cannot release more than reserved. Reserved: %d, Requested: %d",
                    reservedQuantity, quantity)
            );
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }

    public void commit(long quantity) {
        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                String.format("Cannot commit more than reserved. Reserved: %d, Requested: %d",
                    reservedQuantity, quantity)
            );
        }
        this.reservedQuantity -= quantity;
        this.totalQuantity -= quantity;
    }

    public void addStock(long quantity) {
        this.availableQuantity += quantity;
        this.totalQuantity += quantity;
    }

    public boolean isLowStock() {
        return availableQuantity < lowStockThreshold;
    }
}

