-- Add unit_cost to inventory_flow for persisting inbound unit cost
ALTER TABLE inventory_flow ADD COLUMN unit_cost DECIMAL(18,4) NULL COMMENT 'Unit cost for inbound flow' AFTER remark;
