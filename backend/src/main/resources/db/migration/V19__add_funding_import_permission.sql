-- M31: funding ledger Excel import permission.

CREATE TABLE IF NOT EXISTS funding_import_batch (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Funding import batch ID',
    store_id BIGINT NOT NULL COMMENT 'Store ID',
    batch_no VARCHAR(64) NOT NULL COMMENT 'Import batch number',
    original_filename VARCHAR(255) NOT NULL COMMENT 'Original Excel filename',
    status VARCHAR(32) NOT NULL COMMENT 'PENDING / IMPORTED / FAILED',
    total_rows INT NOT NULL DEFAULT 0 COMMENT 'Imported data rows',
    success_rows INT NOT NULL DEFAULT 0 COMMENT 'Successful rows',
    failed_rows INT NOT NULL DEFAULT 0 COMMENT 'Failed rows',
    error_summary VARCHAR(1024) NULL COMMENT 'Import error summary',
    created_by BIGINT NULL COMMENT 'Created by',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_by BIGINT NULL COMMENT 'Updated by',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Soft delete flag',
    PRIMARY KEY (id),
    UNIQUE KEY uk_funding_import_batch_no (batch_no),
    KEY idx_funding_import_batch_store_created (store_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Funding Excel import batch';

INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order, remark,
    created_by, created_at, updated_by, updated_at, deleted
)
SELECT 1048, 'FUNDING_IMPORT', '资方台账导入', 'FUNDING', 'ENABLED', 48, '资方台账模块：从线下 Excel 导入正式台账',
       NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'FUNDING_IMPORT');

INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, NULL, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'FUNDING_IMPORT'
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
