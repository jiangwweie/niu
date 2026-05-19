-- H2-compatible DDL (ON UPDATE CURRENT_TIMESTAMP removed)
-- Only tables needed for Task 5 tests

CREATE TABLE IF NOT EXISTS sequence_daily (
    id BIGINT NOT NULL AUTO_INCREMENT,
    seq_type VARCHAR(32) NOT NULL,
    seq_date DATE NOT NULL,
    current_val BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (seq_type, seq_date)
);

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type_code VARCHAR(64) NOT NULL,
    type_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (type_code)
);

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type_id BIGINT NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(128) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    is_system INTEGER NOT NULL DEFAULT 0,
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (type_id, item_code)
);

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    username VARCHAR(64) NULL,
    password_hash VARCHAR(255) NULL,
    real_name VARCHAR(64) NOT NULL,
    phone VARCHAR(32) NULL,
    wechat_openid VARCHAR(128) NULL,
    wechat_unionid VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    password_must_change BOOLEAN NOT NULL DEFAULT FALSE,
    password_changed_at TIMESTAMP NULL,
    last_login_at TIMESTAMP NULL,
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS store (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_code VARCHAR(64) NOT NULL,
    store_name VARCHAR(128) NOT NULL,
    contact_name VARCHAR(64) NULL,
    contact_phone VARCHAR(32) NULL,
    address VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NULL,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (store_id, role_code)
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    permission_code VARCHAR(128) NOT NULL,
    permission_name VARCHAR(128) NOT NULL,
    module_code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (permission_code)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS part (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    part_code VARCHAR(64) NOT NULL,
    official_part_no VARCHAR(128) NULL,
    part_name VARCHAR(128) NOT NULL,
    model VARCHAR(128) NULL,
    source VARCHAR(32) NOT NULL,
    category_code VARCHAR(64) NULL,
    reference_cost_price DECIMAL(18,4) NULL,
    default_barcode VARCHAR(128) NULL,
    location_remark VARCHAR(255) NULL,
    create_source VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (store_id, part_code)
);

CREATE TABLE IF NOT EXISTS part_barcode (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    part_id BIGINT NOT NULL,
    barcode VARCHAR(128) NOT NULL,
    barcode_type VARCHAR(32) NULL,
    is_primary INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (store_id, barcode)
);

CREATE TABLE IF NOT EXISTS inventory_stock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    part_id BIGINT NOT NULL,
    actual_qty INTEGER NOT NULL DEFAULT 0,
    available_qty INTEGER NOT NULL DEFAULT 0,
    reserved_qty INTEGER NOT NULL DEFAULT 0,
    last_flow_id BIGINT NULL,
    last_changed_at TIMESTAMP NULL,
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (store_id, part_id)
);

CREATE TABLE IF NOT EXISTS inventory_flow (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    inventory_stock_id BIGINT NOT NULL,
    part_id BIGINT NOT NULL,
    flow_type VARCHAR(32) NOT NULL,
    quantity_delta INTEGER NOT NULL,
    actual_before INTEGER NOT NULL,
    actual_after INTEGER NOT NULL,
    available_before INTEGER NOT NULL,
    available_after INTEGER NOT NULL,
    reserved_before INTEGER NOT NULL,
    reserved_after INTEGER NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NULL,
    work_order_id BIGINT NULL,
    work_order_charge_item_id BIGINT NULL,
    operator_id BIGINT NOT NULL,
    operated_at TIMESTAMP NOT NULL,
    reason VARCHAR(255) NULL,
    remark VARCHAR(512) NULL,
    unit_cost DECIMAL(18,4) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS work_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    work_order_no VARCHAR(64) NOT NULL,
    customer_id BIGINT NULL,
    vehicle_id BIGINT NULL,
    customer_name_snapshot VARCHAR(64) NOT NULL,
    customer_phone_snapshot VARCHAR(32) NULL,
    vehicle_model_snapshot VARCHAR(128) NULL,
    frame_no_snapshot VARCHAR(128) NULL,
    battery_no_snapshot VARCHAR(128) NULL,
    repair_item VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    receivable_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    received_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    submitted_by BIGINT NULL,
    submitted_at TIMESTAMP NULL,
    settled_by BIGINT NULL,
    settled_at TIMESTAMP NULL,
    cancelled_by BIGINT NULL,
    cancelled_at TIMESTAMP NULL,
    cancel_reason VARCHAR(255) NULL,
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (work_order_no)
);

CREATE TABLE IF NOT EXISTS work_order_charge_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    work_order_id BIGINT NOT NULL,
    charge_type VARCHAR(32) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    part_id BIGINT NULL,
    part_code_snapshot VARCHAR(64) NULL,
    part_name_snapshot VARCHAR(128) NULL,
    part_source_snapshot VARCHAR(32) NULL,
    quantity INT NOT NULL,
    unit VARCHAR(32) NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    line_amount DECIMAL(18,2) NOT NULL,
    cost_price_snapshot DECIMAL(18,4) NULL,
    line_cost_amount DECIMAL(18,2) NULL,
    inventory_affecting INTEGER NOT NULL DEFAULT 0,
    is_temp_part INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS work_order_status_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    work_order_id BIGINT NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    operator_id BIGINT NOT NULL,
    operated_at TIMESTAMP NOT NULL,
    reason VARCHAR(255) NULL,
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS payment_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    work_order_id BIGINT NOT NULL,
    payment_no VARCHAR(64) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    paid_at TIMESTAMP NOT NULL,
    receiver_id BIGINT NULL,
    operator_id BIGINT NOT NULL,
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (payment_no)
);

CREATE TABLE IF NOT EXISTS refund_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    work_order_id BIGINT NOT NULL,
    refund_no VARCHAR(64) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    refund_method VARCHAR(32) NOT NULL,
    refunded_at TIMESTAMP NOT NULL,
    operator_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (refund_no)
);

CREATE TABLE IF NOT EXISTS official_after_sales (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    work_order_id BIGINT NOT NULL,
    is_official_after_sales INTEGER NOT NULL DEFAULT 0,
    official_order_no VARCHAR(128) NULL,
    official_settlement_amount DECIMAL(18,2) NULL,
    official_settlement_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    official_settlement_time TIMESTAMP NULL,
    official_settlement_operator_id BIGINT NULL,
    official_settlement_remark VARCHAR(512) NULL,
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (work_order_id),
    UNIQUE (store_id, official_order_no)
);

CREATE TABLE IF NOT EXISTS reimbursement (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    reimbursement_no VARCHAR(64) NOT NULL,
    applicant_id BIGINT NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    confirmed_amount DECIMAL(18,2) NULL,
    status VARCHAR(32) NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    confirmed_by BIGINT NULL,
    confirmed_at TIMESTAMP NULL,
    rejected_by BIGINT NULL,
    rejected_at TIMESTAMP NULL,
    reject_reason VARCHAR(512) NULL,
    cancelled_by BIGINT NULL,
    cancelled_at TIMESTAMP NULL,
    cancel_reason VARCHAR(512) NULL,
    remark VARCHAR(512) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (reimbursement_no)
);
