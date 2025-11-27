-- V1_3__Fix_price_column_type.sql: Fix price column to match JPA Double type

ALTER TABLE item ALTER COLUMN price TYPE DOUBLE PRECISION;
