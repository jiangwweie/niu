-- M24B: clean-start work-order / cashier / inventory state machine.
-- This migration only changes schema, dictionaries, and permissions.
-- It must NOT delete work orders, payments, refunds, inventory flows, or other business data.

ALTER TABLE work_order
    ADD COLUMN repair_done_by BIGINT NULL COMMENT 'Repair done by' AFTER settled_at,
    ADD COLUMN repair_done_at DATETIME NULL COMMENT 'Repair done at' AFTER repair_done_by,
    ADD COLUMN delivered_by BIGINT NULL COMMENT 'Delivered by' AFTER repair_done_at,
    ADD COLUMN delivered_at DATETIME NULL COMMENT 'Delivered at' AFTER delivered_by,
    ADD COLUMN no_charge_reason VARCHAR(64) NULL COMMENT 'No-charge reason' AFTER delivered_at,
    ADD COLUMN no_charge_remark VARCHAR(512) NULL COMMENT 'No-charge remark' AFTER no_charge_reason,
    ADD KEY idx_work_order_repair_done_at (repair_done_at),
    ADD KEY idx_work_order_delivered_at (delivered_at);

UPDATE sys_dict_item
SET status = 'DISABLED', remark = 'Replaced by M24B clean-start repair state machine'
WHERE type_id = 2001
  AND item_code IN ('PENDING_ACCEPT', 'ACCEPTED', 'PART_ORDERED', 'PART_ARRIVED', 'SETTLED');

INSERT INTO sys_dict_item (
    type_id, item_code, item_name, sort_order, status, is_system, remark,
    created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (2001, 'REPAIRING', '维修中', 2, 'ENABLED', 1, 'M24B clean-start state', NULL, NOW(), NULL, NOW(), 0),
    (2001, 'REPAIR_DONE', '维修完成', 3, 'ENABLED', 1, 'M24B clean-start state', NULL, NOW(), NULL, NOW(), 0),
    (2001, 'DELIVERED', '已交付', 4, 'ENABLED', 1, 'M24B clean-start state', NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
    item_name = VALUES(item_name),
    sort_order = VALUES(sort_order),
    status = 'ENABLED',
    remark = VALUES(remark),
    updated_at = NOW();

INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order,
    remark, created_by, created_at, updated_by, updated_at, deleted
) VALUES (
    1032, 'REFUND_AFTER_DELIVERY', '交付后退款', 'PAYMENT', 'ENABLED', 32,
    'M24B admin-web only after-delivery refund permission', NULL, NOW(), NULL, NOW(), 0
)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    module_code = VALUES(module_code),
    status = 'ENABLED',
    sort_order = VALUES(sort_order),
    remark = VALUES(remark),
    updated_at = NOW();

INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT r.id, p.id, NULL, NOW()
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'REFUND_AFTER_DELIVERY'
WHERE r.role_code IN ('SUPER_ADMIN', 'STORE_ADMIN')
  AND r.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
