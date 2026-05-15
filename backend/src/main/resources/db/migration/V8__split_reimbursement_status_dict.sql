-- Split REJECTED_OR_CANCELLED into separate REJECTED and CANCELLED dict items.
-- Disable the old combined item to avoid confusion with the actual enum values.

UPDATE sys_dict_item
SET status = 'DISABLED', remark = 'Migrated to REJECTED + CANCELLED (V8)'
WHERE type_id = 2006 AND item_code = 'REJECTED_OR_CANCELLED';

INSERT INTO sys_dict_item (
    type_id, item_code, item_name, sort_order, status, is_system, remark, created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (2006, 'REJECTED', '已驳回', 3, 'ENABLED', 1, 'Split from REJECTED_OR_CANCELLED in V8', NULL, NOW(), NULL, NOW(), 0),
    (2006, 'CANCELLED', '已取消', 4, 'ENABLED', 1, 'Split from REJECTED_OR_CANCELLED in V8', NULL, NOW(), NULL, NOW(), 0);