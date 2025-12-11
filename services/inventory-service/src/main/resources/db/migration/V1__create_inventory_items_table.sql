CREATE TABLE IF NOT EXISTS inventory_items (
    id UUID PRIMARY KEY,
    product_id VARCHAR(100) UNIQUE NOT NULL,
    available_quantity BIGINT NOT NULL CHECK (available_quantity >= 0),
    reserved_quantity BIGINT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    total_quantity BIGINT NOT NULL CHECK (total_quantity >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    low_stock_threshold INT DEFAULT 10,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_quantities CHECK (available_quantity + reserved_quantity = total_quantity)
);

CREATE INDEX idx_inventory_product_id ON inventory_items(product_id);
CREATE INDEX idx_inventory_low_stock ON inventory_items(available_quantity)
  WHERE available_quantity < low_stock_threshold;
CREATE INDEX idx_inventory_version ON inventory_items(version);

COMMENT ON COLUMN inventory_items.version IS 'Optimistic locking version';
COMMENT ON COLUMN inventory_items.available_quantity IS 'Stock available for reservation';
COMMENT ON COLUMN inventory_items.reserved_quantity IS 'Stock currently reserved';
COMMENT ON COLUMN inventory_items.total_quantity IS 'Total stock (available + reserved)';

