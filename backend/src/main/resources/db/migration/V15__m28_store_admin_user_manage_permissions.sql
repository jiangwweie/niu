-- M28: allow existing store administrators to use scoped user management.
-- No new permission codes or schema changes are introduced here.

INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, NULL, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('USER_MANAGE', 'ROLE_MANAGE')
WHERE r.role_code = 'STORE_ADMIN'
  AND r.status = 'ENABLED'
  AND r.deleted = 0
  AND p.status = 'ENABLED'
  AND p.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
