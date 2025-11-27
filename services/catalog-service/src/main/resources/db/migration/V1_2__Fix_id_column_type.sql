-- V1_2__Fix_id_column_type.sql: Change id column from SERIAL to BIGSERIAL

ALTER TABLE item ALTER COLUMN id TYPE BIGINT;
