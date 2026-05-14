ALTER TABLE official_after_sales
    ADD UNIQUE KEY uk_official_order_no_store (store_id, official_order_no);
