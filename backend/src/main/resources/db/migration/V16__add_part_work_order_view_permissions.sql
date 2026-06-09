-- Add explicit read permissions used by admin-web route/menu guards.
-- Keep grants scoped to operational roles; finance remains limited to finance/payment surfaces.

INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order, remark, created_by, created_at, updated_by, updated_at, deleted
)
SELECT 1035, 'PART_VIEW', '配件查看', 'PART', 'ENABLED', 35, 'Read-only access to part catalog', NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'PART_VIEW'
);

INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order, remark, created_by, created_at, updated_by, updated_at, deleted
)
SELECT 1036, 'WORK_ORDER_VIEW', '工单查看', 'WORK_ORDER', 'ENABLED', 36, 'Read-only access to work orders', NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'WORK_ORDER_VIEW'
);

INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, NULL, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('PART_VIEW', 'WORK_ORDER_VIEW')
WHERE r.role_code IN ('SUPER_ADMIN', 'STORE_ADMIN', 'TECHNICIAN_FRONT_DESK')
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
