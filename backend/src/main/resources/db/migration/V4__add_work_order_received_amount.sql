ALTER TABLE work_order
    ADD COLUMN received_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Received amount after payments and refunds'
    AFTER receivable_amount;
