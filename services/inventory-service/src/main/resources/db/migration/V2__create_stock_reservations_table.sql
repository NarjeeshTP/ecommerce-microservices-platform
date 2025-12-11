CREATE TABLE IF NOT EXISTS stock_reservations (
    id UUID PRIMARY KEY,
    reservation_id VARCHAR(255) UNIQUE NOT NULL,
    product_id VARCHAR(100) NOT NULL,
    order_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    status VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP,
    notes TEXT,
    UNIQUE (product_id, order_id)
);

CREATE INDEX idx_reservations_reservation_id ON stock_reservations(reservation_id);
CREATE INDEX idx_reservations_order ON stock_reservations(order_id);
CREATE INDEX idx_reservations_product ON stock_reservations(product_id);
CREATE INDEX idx_reservations_status ON stock_reservations(status);
CREATE INDEX idx_reservations_expires ON stock_reservations(expires_at)
  WHERE status = 'ACTIVE';

COMMENT ON COLUMN stock_reservations.reservation_id IS 'Unique reservation identifier';
COMMENT ON COLUMN stock_reservations.status IS 'ACTIVE, RELEASED, EXPIRED, COMMITTED';
COMMENT ON COLUMN stock_reservations.expires_at IS 'TTL for automatic release';
COMMENT ON CONSTRAINT stock_reservations_product_id_order_id_key
  ON stock_reservations IS 'Prevents duplicate reservations (DB constraint strategy)';

