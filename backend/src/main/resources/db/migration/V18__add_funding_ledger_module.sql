-- M30: funding ledger module for internal financing-material review and receivable ledger.
-- This module is isolated from work_order, payment_record, inventory and reimbursement flows.

INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order, remark,
    created_by, created_at, updated_by, updated_at, deleted
)
SELECT 1040, 'FUNDING_APPLICATION_VIEW', '资方资料查看', 'FUNDING', 'ENABLED', 40, '资方台账模块：查看资料申请',
       NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'FUNDING_APPLICATION_VIEW');

INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order, remark,
    created_by, created_at, updated_by, updated_at, deleted
)
SELECT 1041, 'FUNDING_APPLICATION_MANAGE', '资方资料管理', 'FUNDING', 'ENABLED', 41, '资方台账模块：新增编辑资料申请',
       NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'FUNDING_APPLICATION_MANAGE');

INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order, remark,
    created_by, created_at, updated_by, updated_at, deleted
)
SELECT 1042, 'FUNDING_APPLICATION_AUDIT', '资方资料审核', 'FUNDING', 'ENABLED', 42, '资方台账模块：同意或不同意资料申请',
       NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'FUNDING_APPLICATION_AUDIT');

INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order, remark,
    created_by, created_at, updated_by, updated_at, deleted
)
SELECT 1043, 'FUNDING_CONTRACT_MANAGE', '资方合同管理', 'FUNDING', 'ENABLED', 43, '资方台账模块：上传、确认、作废线下合同',
       NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'FUNDING_CONTRACT_MANAGE');

INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order, remark,
    created_by, created_at, updated_by, updated_at, deleted
)
SELECT 1044, 'FUNDING_LEDGER_VIEW', '资方台账查看', 'FUNDING', 'ENABLED', 44, '资方台账模块：查看正式台账',
       NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'FUNDING_LEDGER_VIEW');

INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order, remark,
    created_by, created_at, updated_by, updated_at, deleted
)
SELECT 1045, 'FUNDING_LEDGER_MANAGE', '资方台账管理', 'FUNDING', 'ENABLED', 45, '资方台账模块：修改台账并留痕',
       NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'FUNDING_LEDGER_MANAGE');

INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order, remark,
    created_by, created_at, updated_by, updated_at, deleted
)
SELECT 1046, 'FUNDING_PAYMENT_RECORD', '资方收款登记', 'FUNDING', 'ENABLED', 46, '资方台账模块：登记台账收款',
       NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'FUNDING_PAYMENT_RECORD');

INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order, remark,
    created_by, created_at, updated_by, updated_at, deleted
)
SELECT 1047, 'FUNDING_EXPORT', '资方台账导出', 'FUNDING', 'ENABLED', 47, '资方台账模块：导出资料、合同、台账、收款',
       NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'FUNDING_EXPORT');

INSERT INTO sys_role (
    store_id, role_code, role_name, status, sort_order, remark, created_by, created_at, updated_by, updated_at, deleted
)
SELECT s.id, 'FUNDING_LEDGER_OPERATOR', '资方台账专员', 'ENABLED', 8, '资方资料审核、线下合同留存、应收台账和收款维护',
       NULL, NOW(), NULL, NOW(), 0
FROM store s
WHERE s.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role r
      WHERE r.store_id = s.id
        AND r.role_code = 'FUNDING_LEDGER_OPERATOR'
        AND r.deleted = 0
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, NULL, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'FUNDING_APPLICATION_VIEW',
    'FUNDING_APPLICATION_MANAGE',
    'FUNDING_APPLICATION_AUDIT',
    'FUNDING_CONTRACT_MANAGE',
    'FUNDING_LEDGER_VIEW',
    'FUNDING_LEDGER_MANAGE',
    'FUNDING_PAYMENT_RECORD',
    'FUNDING_EXPORT'
)
WHERE r.role_code IN ('FUNDING_LEDGER_OPERATOR', 'STORE_ADMIN', 'SUPER_ADMIN')
  AND r.status = 'ENABLED'
  AND r.deleted = 0
  AND p.status = 'ENABLED'
  AND p.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

CREATE TABLE funding_application (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Funding application ID',
    store_id BIGINT NOT NULL COMMENT 'Store ID',
    application_no VARCHAR(64) NOT NULL COMMENT 'Application number',
    customer_name VARCHAR(64) NOT NULL COMMENT 'Customer name',
    phone VARCHAR(32) NOT NULL COMMENT 'Customer phone',
    id_card_no VARCHAR(32) NOT NULL COMMENT 'ID card number',
    vehicle_model VARCHAR(128) NOT NULL COMMENT 'Vehicle model',
    pickup_date DATE NOT NULL COMMENT 'Pickup date',
    payment_type VARCHAR(32) NOT NULL COMMENT 'FULL / INSTALLMENT',
    purchase_cost DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Purchase cost',
    incentive_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Incentive amount',
    upstream_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Upstream amount',
    total_cost DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Total cost',
    retail_price DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Retail price',
    receivable_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Total receivable',
    down_payment DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Down payment plan',
    installment_count INT NOT NULL DEFAULT 0 COMMENT 'Installment count',
    installment_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Per installment amount',
    first_due_date DATE NULL COMMENT 'First installment due date',
    group_leader VARCHAR(64) NOT NULL COMMENT 'Group leader',
    handler_name VARCHAR(64) NULL COMMENT 'Handler name',
    add_on_remark VARCHAR(512) NULL COMMENT 'Add-on remark',
    status VARCHAR(32) NOT NULL COMMENT 'DRAFT / PENDING_AUDIT / APPROVED / REJECTED / CONTRACT_PENDING / CONTRACT_CONFIRMED / LEDGER_CREATED / VOIDED',
    audit_remark VARCHAR(512) NULL COMMENT 'Audit remark',
    audited_by BIGINT NULL COMMENT 'Audited by',
    audited_at DATETIME NULL COMMENT 'Audited at',
    submitted_by BIGINT NULL COMMENT 'Submitted by',
    submitted_at DATETIME NULL COMMENT 'Submitted at',
    remark VARCHAR(512) NULL COMMENT 'Remark',
    created_by BIGINT NULL COMMENT 'Created by',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_by BIGINT NULL COMMENT 'Updated by',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Soft delete flag',
    PRIMARY KEY (id),
    UNIQUE KEY uk_funding_application_no (application_no),
    KEY idx_funding_app_store_status (store_id, status),
    KEY idx_funding_app_customer (store_id, customer_name),
    KEY idx_funding_app_phone (store_id, phone),
    KEY idx_funding_app_pickup_date (store_id, pickup_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Funding material application';

CREATE TABLE funding_contract (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Funding contract ID',
    store_id BIGINT NOT NULL COMMENT 'Store ID',
    application_id BIGINT NOT NULL COMMENT 'Funding application ID',
    contract_no VARCHAR(64) NOT NULL COMMENT 'Contract number',
    contract_type VARCHAR(32) NOT NULL COMMENT 'FULL / INSTALLMENT / SUPPLEMENT / OTHER',
    signed_date DATE NULL COMMENT 'Offline signed date',
    status VARCHAR(32) NOT NULL COMMENT 'PENDING_UPLOAD / UPLOADED / CONFIRMED / VOIDED',
    confirmed_by BIGINT NULL COMMENT 'Confirmed by',
    confirmed_at DATETIME NULL COMMENT 'Confirmed at',
    void_reason VARCHAR(255) NULL COMMENT 'Void reason',
    remark VARCHAR(512) NULL COMMENT 'Remark',
    created_by BIGINT NULL COMMENT 'Created by',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_by BIGINT NULL COMMENT 'Updated by',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Soft delete flag',
    PRIMARY KEY (id),
    UNIQUE KEY uk_funding_contract_no (contract_no),
    KEY idx_funding_contract_app (application_id),
    KEY idx_funding_contract_store_status (store_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Funding offline contract';

CREATE TABLE funding_ledger (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Funding ledger ID',
    store_id BIGINT NOT NULL COMMENT 'Store ID',
    application_id BIGINT NOT NULL COMMENT 'Funding application ID',
    contract_id BIGINT NOT NULL COMMENT 'Funding contract ID',
    ledger_no VARCHAR(64) NOT NULL COMMENT 'Ledger number',
    customer_name VARCHAR(64) NOT NULL COMMENT 'Customer name snapshot',
    phone VARCHAR(32) NOT NULL COMMENT 'Phone snapshot',
    id_card_no VARCHAR(32) NOT NULL COMMENT 'ID card number snapshot',
    vehicle_model VARCHAR(128) NOT NULL COMMENT 'Vehicle model snapshot',
    pickup_date DATE NOT NULL COMMENT 'Pickup date snapshot',
    payment_type VARCHAR(32) NOT NULL COMMENT 'Payment type snapshot',
    purchase_cost DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Purchase cost',
    incentive_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Incentive amount',
    upstream_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Upstream amount',
    total_cost DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Total cost',
    retail_price DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Retail price',
    receivable_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Total receivable',
    received_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Received total',
    outstanding_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Outstanding amount',
    group_leader VARCHAR(64) NOT NULL COMMENT 'Group leader snapshot',
    handler_name VARCHAR(64) NULL COMMENT 'Handler name snapshot',
    status VARCHAR(32) NOT NULL COMMENT 'NORMAL / PARTIAL_PAID / SETTLED / OVERDUE / ABNORMAL / VOIDED',
    remark VARCHAR(512) NULL COMMENT 'Remark',
    created_by BIGINT NULL COMMENT 'Created by',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_by BIGINT NULL COMMENT 'Updated by',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Soft delete flag',
    PRIMARY KEY (id),
    UNIQUE KEY uk_funding_ledger_no (ledger_no),
    UNIQUE KEY uk_funding_ledger_contract (contract_id),
    KEY idx_funding_ledger_app (application_id),
    KEY idx_funding_ledger_store_status (store_id, status),
    KEY idx_funding_ledger_customer (store_id, customer_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Funding receivable ledger';

CREATE TABLE funding_installment_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Installment plan ID',
    store_id BIGINT NOT NULL COMMENT 'Store ID',
    ledger_id BIGINT NOT NULL COMMENT 'Funding ledger ID',
    phase_no INT NOT NULL COMMENT '0 means down payment; 1+ means installment phase',
    phase_name VARCHAR(64) NOT NULL COMMENT 'Phase name',
    due_date DATE NULL COMMENT 'Due date',
    receivable_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Receivable amount',
    received_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Received amount',
    status VARCHAR(32) NOT NULL COMMENT 'PENDING / PARTIAL_PAID / PAID / OVERDUE',
    remark VARCHAR(512) NULL COMMENT 'Remark',
    created_by BIGINT NULL COMMENT 'Created by',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_by BIGINT NULL COMMENT 'Updated by',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Soft delete flag',
    PRIMARY KEY (id),
    UNIQUE KEY uk_funding_plan_phase (ledger_id, phase_no),
    KEY idx_funding_plan_store_due (store_id, due_date),
    KEY idx_funding_plan_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Funding installment plan';

CREATE TABLE funding_payment_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Funding payment record ID',
    store_id BIGINT NOT NULL COMMENT 'Store ID',
    ledger_id BIGINT NOT NULL COMMENT 'Funding ledger ID',
    installment_plan_id BIGINT NULL COMMENT 'Installment plan ID',
    payment_no VARCHAR(64) NOT NULL COMMENT 'Payment number',
    amount DECIMAL(18,2) NOT NULL COMMENT 'Payment amount',
    payment_method VARCHAR(32) NOT NULL COMMENT 'Payment method',
    paid_at DATETIME NOT NULL COMMENT 'Paid at',
    operator_id BIGINT NOT NULL COMMENT 'Operator ID',
    remark VARCHAR(512) NULL COMMENT 'Remark',
    created_by BIGINT NULL COMMENT 'Created by',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_by BIGINT NULL COMMENT 'Updated by',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Soft delete flag',
    PRIMARY KEY (id),
    UNIQUE KEY uk_funding_payment_no (payment_no),
    KEY idx_funding_payment_ledger (ledger_id),
    KEY idx_funding_payment_store_paid (store_id, paid_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Funding ledger payment record';

CREATE TABLE funding_attachment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Funding attachment ID',
    store_id BIGINT NOT NULL COMMENT 'Store ID',
    owner_type VARCHAR(32) NOT NULL COMMENT 'APPLICATION / CONTRACT / PAYMENT',
    owner_id BIGINT NOT NULL COMMENT 'Owner ID',
    attachment_type VARCHAR(32) NOT NULL COMMENT 'ID_CARD / CONTRACT / PAYMENT_VOUCHER / VEHICLE / OTHER',
    original_filename VARCHAR(255) NOT NULL COMMENT 'Original filename',
    stored_filename VARCHAR(255) NOT NULL COMMENT 'Stored filename',
    storage_path VARCHAR(512) NOT NULL COMMENT 'Storage path',
    content_type VARCHAR(128) NULL COMMENT 'Content type',
    file_size BIGINT NOT NULL COMMENT 'File size',
    remark VARCHAR(512) NULL COMMENT 'Remark',
    created_by BIGINT NULL COMMENT 'Created by',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_by BIGINT NULL COMMENT 'Updated by',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Soft delete flag',
    PRIMARY KEY (id),
    KEY idx_funding_attachment_owner (owner_type, owner_id),
    KEY idx_funding_attachment_store (store_id, attachment_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Funding attachment metadata';

CREATE TABLE funding_ledger_change_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Funding ledger change log ID',
    store_id BIGINT NOT NULL COMMENT 'Store ID',
    ledger_id BIGINT NOT NULL COMMENT 'Funding ledger ID',
    field_name VARCHAR(64) NOT NULL COMMENT 'Changed field',
    old_value VARCHAR(512) NULL COMMENT 'Old value',
    new_value VARCHAR(512) NULL COMMENT 'New value',
    operator_id BIGINT NOT NULL COMMENT 'Operator ID',
    operated_at DATETIME NOT NULL COMMENT 'Operated at',
    remark VARCHAR(512) NULL COMMENT 'Remark',
    created_by BIGINT NULL COMMENT 'Created by',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (id),
    KEY idx_funding_log_ledger (ledger_id),
    KEY idx_funding_log_store_operated (store_id, operated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Funding ledger edit log';

CREATE TABLE funding_import_batch (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Funding import batch ID',
    store_id BIGINT NOT NULL COMMENT 'Store ID',
    batch_no VARCHAR(64) NOT NULL COMMENT 'Import batch number',
    original_filename VARCHAR(255) NOT NULL COMMENT 'Original import filename',
    status VARCHAR(32) NOT NULL COMMENT 'PENDING / IMPORTED / FAILED',
    total_rows INT NOT NULL DEFAULT 0 COMMENT 'Total source rows',
    success_rows INT NOT NULL DEFAULT 0 COMMENT 'Success rows',
    failed_rows INT NOT NULL DEFAULT 0 COMMENT 'Failed rows',
    error_summary VARCHAR(1024) NULL COMMENT 'Error summary',
    created_by BIGINT NULL COMMENT 'Created by',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_by BIGINT NULL COMMENT 'Updated by',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Soft delete flag',
    PRIMARY KEY (id),
    UNIQUE KEY uk_funding_import_batch_no (batch_no),
    KEY idx_funding_import_store_status (store_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Funding Excel import batch';
