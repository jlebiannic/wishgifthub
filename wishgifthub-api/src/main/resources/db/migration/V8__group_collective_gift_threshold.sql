ALTER TABLE groups ADD COLUMN collective_gift_threshold NUMERIC(10, 2) NOT NULL DEFAULT 50.00;
ALTER TABLE groups ADD CONSTRAINT chk_group_collective_gift_threshold CHECK (
    collective_gift_threshold >= 0 AND collective_gift_threshold <= 99999999.99
);
