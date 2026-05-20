-- M18: Customer & Vehicle archive management permissions

INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order,
    remark, created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (1021, 'CUSTOMER_VIEW', '客户档案查看', 'CUSTOMER', 'ENABLED', 21,
     'M18 customer & vehicle archive view', NULL, NOW(), NULL, NOW(), 0),
    (1022, 'CUSTOMER_MANAGE', '客户档案管理', 'CUSTOMER', 'ENABLED', 22,
     'M18 customer & vehicle archive manage', NULL, NOW(), NULL, NOW(), 0);

-- SUPER_ADMIN gets all permissions automatically via existing V2 trigger;
-- explicitly assign to STORE_ADMIN and TECHNICIAN_FRONT_DESK
INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, NULL, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('CUSTOMER_VIEW', 'CUSTOMER_MANAGE')
WHERE r.role_code IN ('SUPER_ADMIN', 'STORE_ADMIN')
  AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- TECHNICIAN_FRONT_DESK: view only
INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, NULL, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'CUSTOMER_VIEW'
WHERE r.role_code = 'TECHNICIAN_FRONT_DESK'
  AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
