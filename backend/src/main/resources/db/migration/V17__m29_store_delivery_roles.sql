-- M29: preset store delivery roles for acceptance and daily operation.
-- These roles are store-scoped and do not change the permission model.

UPDATE sys_dict_type
SET type_name = '配件分类',
    updated_at = NOW()
WHERE type_code = 'PART_SOURCE'
  AND type_name = '配件来源'
  AND deleted = 0;

INSERT INTO sys_role (
    store_id, role_code, role_name, status, sort_order, remark, created_by, created_at, updated_by, updated_at, deleted
)
SELECT s.id, 'INVENTORY_CLERK', '库存员', 'ENABLED', 5, '交付预置角色：配件库存入库与查询', NULL, NOW(), NULL, NOW(), 0
FROM store s
WHERE s.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role r
      WHERE r.store_id = s.id
        AND r.role_code = 'INVENTORY_CLERK'
        AND r.deleted = 0
  );

INSERT INTO sys_role (
    store_id, role_code, role_name, status, sort_order, remark, created_by, created_at, updated_by, updated_at, deleted
)
SELECT s.id, 'CASHIER', '收银员', 'ENABLED', 6, '交付预置角色：工单查看、收款、退款', NULL, NOW(), NULL, NOW(), 0
FROM store s
WHERE s.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role r
      WHERE r.store_id = s.id
        AND r.role_code = 'CASHIER'
        AND r.deleted = 0
  );

INSERT INTO sys_role (
    store_id, role_code, role_name, status, sort_order, remark, created_by, created_at, updated_by, updated_at, deleted
)
SELECT s.id, 'READONLY_STAFF', '只读员工', 'ENABLED', 7, '交付预置角色：工单、客户、配件、库存只读', NULL, NOW(), NULL, NOW(), 0
FROM store s
WHERE s.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role r
      WHERE r.store_id = s.id
        AND r.role_code = 'READONLY_STAFF'
        AND r.deleted = 0
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, NULL, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('PART_VIEW', 'INVENTORY_VIEW', 'INVENTORY_INBOUND')
WHERE r.role_code = 'INVENTORY_CLERK'
  AND r.status = 'ENABLED'
  AND r.deleted = 0
  AND p.status = 'ENABLED'
  AND p.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, NULL, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('WORK_ORDER_VIEW', 'PAYMENT_RECORD', 'REFUND_RECORD')
WHERE r.role_code = 'CASHIER'
  AND r.status = 'ENABLED'
  AND r.deleted = 0
  AND p.status = 'ENABLED'
  AND p.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, NULL, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('WORK_ORDER_VIEW', 'PART_VIEW', 'INVENTORY_VIEW', 'CUSTOMER_VIEW')
WHERE r.role_code = 'READONLY_STAFF'
  AND r.status = 'ENABLED'
  AND r.deleted = 0
  AND p.status = 'ENABLED'
  AND p.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
