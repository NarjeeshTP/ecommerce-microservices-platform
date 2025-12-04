-- Pricing Rules Table
CREATE TABLE IF NOT EXISTS pricing_rules (
    id BIGSERIAL PRIMARY KEY,
    item_id VARCHAR(255) NOT NULL,
    base_price DECIMAL(10, 2) NOT NULL,
    discount_percent DECIMAL(10, 2),
    final_price DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(50) DEFAULT 'USD',
    rule_type VARCHAR(50) DEFAULT 'STANDARD',
    min_quantity INTEGER,
    valid_from TIMESTAMP,
    valid_until TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster lookups
CREATE INDEX idx_pricing_rules_item_id ON pricing_rules(item_id);
CREATE INDEX idx_pricing_rules_status ON pricing_rules(status);
CREATE INDEX idx_pricing_rules_item_status ON pricing_rules(item_id, status);

-- Insert sample data
INSERT INTO pricing_rules (item_id, base_price, discount_percent, final_price, currency, rule_type, status)
VALUES
    ('ITEM-001', 99.99, 10.00, 89.99, 'USD', 'STANDARD', 'ACTIVE'),
    ('ITEM-002', 149.99, 15.00, 127.49, 'USD', 'PROMOTIONAL', 'ACTIVE'),
    ('ITEM-003', 49.99, 0.00, 49.99, 'USD', 'STANDARD', 'ACTIVE'),
    ('ITEM-004', 199.99, 20.00, 159.99, 'USD', 'SEASONAL', 'ACTIVE'),
    ('ITEM-005', 29.99, 5.00, 28.49, 'USD', 'STANDARD', 'ACTIVE');

