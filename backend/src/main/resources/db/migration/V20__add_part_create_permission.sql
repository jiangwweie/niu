-- Formalize PART_CREATE because staff/admin part creation endpoints use it.
-- Normalize administrator roles so SUPER_ADMIN remains platform-wide all permissions
-- and STORE_ADMIN remains all store-grantable permissions for its store.
-- This is a permission seed migration only; it does not change business data tables.

INSERT INTO sys_permission (
    permission_code, permission_name, module_code, status, sort_order, remark,
    created_by, created_at, updated_by, updated_at, deleted
)
SELECT 'PART_CREATE', '配件新增', 'PART', 'ENABLED', 36, '新增配件按钮与接口权限', NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'PART_CREATE'
);

-- SUPER_ADMIN is the platform administrator and receives every enabled catalog permission.
INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, NULL, NOW()
FROM sys_role r
JOIN sys_permission p ON p.status = 'ENABLED' AND p.deleted = 0
WHERE r.role_code = 'SUPER_ADMIN'
  AND r.status = 'ENABLED'
  AND r.deleted = 0
  AND p.permission_code NOT LIKE '%:%'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- STORE_ADMIN receives every store-grantable permission. Platform-only and old
-- colon-style legacy permissions are excluded from store role assignment.
INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, NULL, NOW()
FROM sys_role r
JOIN sys_permission p ON p.status = 'ENABLED' AND p.deleted = 0
WHERE r.role_code = 'STORE_ADMIN'
  AND r.status = 'ENABLED'
  AND r.deleted = 0
  AND p.permission_code <> 'PLATFORM_MANAGE'
  AND p.permission_code NOT LIKE '%:%'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
