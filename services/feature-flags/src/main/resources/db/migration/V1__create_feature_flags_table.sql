CREATE TABLE IF NOT EXISTS feature_flags (
    id UUID PRIMARY KEY,
    key VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS feature_flag_rules (
    id UUID PRIMARY KEY,
    feature_flag_id UUID NOT NULL REFERENCES feature_flags(id) ON DELETE CASCADE,
    rule_type VARCHAR(50) NOT NULL,
    rule_data JSONB NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS feature_flag_history (
    id UUID PRIMARY KEY,
    feature_flag_id UUID NOT NULL REFERENCES feature_flags(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    changed_by VARCHAR(255),
    old_value JSONB,
    new_value JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_feature_flags_key ON feature_flags(key);
CREATE INDEX idx_feature_flags_enabled ON feature_flags(enabled);
CREATE INDEX idx_feature_flag_rules_feature ON feature_flag_rules(feature_flag_id);
CREATE INDEX idx_feature_flag_history_feature ON feature_flag_history(feature_flag_id);
CREATE INDEX idx_feature_flag_history_created ON feature_flag_history(created_at DESC);
COMMENT ON TABLE feature_flags IS 'Feature toggle definitions';
COMMENT ON TABLE feature_flag_rules IS 'Conditional rules for feature targeting';
COMMENT ON TABLE feature_flag_history IS 'Audit log for feature flag changes';
