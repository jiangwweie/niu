ALTER TABLE part
    ADD COLUMN default_sale_price DECIMAL(18,2) NULL COMMENT 'Default sale price'
    AFTER reference_cost_price;
