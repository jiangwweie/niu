ALTER TABLE sys_user
    ADD COLUMN password_must_change TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether user must change password on next login',
    ADD COLUMN password_changed_at DATETIME NULL COMMENT 'Password last changed at';

ALTER TABLE store
    ADD COLUMN contact_name VARCHAR(64) NULL COMMENT 'Store contact name' AFTER store_name;

INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order, remark, created_by, created_at, updated_by, updated_at, deleted
) VALUES (
    1020, 'STORE_MANAGE', '门店配置管理', 'STORE', 'ENABLED', 20, 'M17A store management permission', NULL, NOW(), NULL, NOW(), 0
);

INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, NULL, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'STORE_MANAGE'
WHERE r.role_code IN ('SUPER_ADMIN', 'STORE_ADMIN')
  AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
