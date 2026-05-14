ALTER TABLE reimbursement
    ADD COLUMN cancelled_by BIGINT NULL COMMENT 'Cancelled by' AFTER reject_reason,
    ADD COLUMN cancelled_at DATETIME NULL COMMENT 'Cancelled at' AFTER cancelled_by,
    ADD COLUMN cancel_reason VARCHAR(512) NULL COMMENT 'Cancel reason' AFTER cancelled_at;
