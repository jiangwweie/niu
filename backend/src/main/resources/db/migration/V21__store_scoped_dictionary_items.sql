-- M30: store-scoped dictionary items for high-frequency form options.
-- Dict types remain global. Dict items are split into SYSTEM defaults and STORE maintained options.

ALTER TABLE sys_dict_type
    ADD COLUMN edit_mode VARCHAR(32) NOT NULL DEFAULT 'SYSTEM_ONLY' COMMENT 'SYSTEM_ONLY / STORE_EXTENDABLE' AFTER status;

ALTER TABLE sys_dict_item
    ADD COLUMN store_id BIGINT NULL COMMENT 'Store ID, null means system item' AFTER type_id,
    ADD COLUMN scope VARCHAR(16) NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM / STORE' AFTER store_id;

ALTER TABLE sys_dict_item
    DROP INDEX uk_dict_item_type_code,
    ADD UNIQUE KEY uk_dict_item_type_scope_store_code (type_id, scope, store_id, item_code),
    ADD KEY idx_dict_item_type_scope_store_order (type_id, scope, store_id, sort_order);

UPDATE sys_dict_type
SET edit_mode = 'SYSTEM_ONLY'
WHERE edit_mode IS NULL OR edit_mode = '';

UPDATE sys_dict_item
SET scope = 'SYSTEM',
    store_id = NULL,
    is_system = 1
WHERE scope IS NULL OR scope = '';

UPDATE sys_dict_type
SET type_name = '配件来源',
    updated_at = NOW()
WHERE type_code = 'PART_SOURCE'
  AND deleted = 0;

INSERT INTO sys_dict_type (type_code, type_name, status, edit_mode, remark, created_by, created_at, updated_by, updated_at, deleted)
SELECT 'VEHICLE_MODEL', '车型', 'ENABLED', 'STORE_EXTENDABLE', '常用车型，门店可补充', NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type_code = 'VEHICLE_MODEL' AND deleted = 0);

INSERT INTO sys_dict_type (type_code, type_name, status, edit_mode, remark, created_by, created_at, updated_by, updated_at, deleted)
SELECT 'REPAIR_ITEM', '维修项目', 'ENABLED', 'STORE_EXTENDABLE', '常用维修项目，门店可补充', NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type_code = 'REPAIR_ITEM' AND deleted = 0);

INSERT INTO sys_dict_type (type_code, type_name, status, edit_mode, remark, created_by, created_at, updated_by, updated_at, deleted)
SELECT 'UNIT', '单位', 'ENABLED', 'STORE_EXTENDABLE', '费用和配件常用单位，门店可补充', NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type_code = 'UNIT' AND deleted = 0);

INSERT INTO sys_dict_type (type_code, type_name, status, edit_mode, remark, created_by, created_at, updated_by, updated_at, deleted)
SELECT 'NO_CHARGE_REASON', '无需收款原因', 'ENABLED', 'STORE_EXTENDABLE', '无需收款常用原因，门店可补充', NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type_code = 'NO_CHARGE_REASON' AND deleted = 0);

INSERT INTO sys_dict_type (type_code, type_name, status, edit_mode, remark, created_by, created_at, updated_by, updated_at, deleted)
SELECT 'REFUND_REASON', '退款原因', 'ENABLED', 'STORE_EXTENDABLE', '退款常用原因，门店可补充', NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type_code = 'REFUND_REASON' AND deleted = 0);

INSERT INTO sys_dict_type (type_code, type_name, status, edit_mode, remark, created_by, created_at, updated_by, updated_at, deleted)
SELECT 'WORK_ORDER_CANCEL_REASON', '工单取消原因', 'ENABLED', 'STORE_EXTENDABLE', '工单取消常用原因，门店可补充', NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type_code = 'WORK_ORDER_CANCEL_REASON' AND deleted = 0);

INSERT INTO sys_dict_type (type_code, type_name, status, edit_mode, remark, created_by, created_at, updated_by, updated_at, deleted)
SELECT 'INBOUND_REASON', '入库原因', 'ENABLED', 'STORE_EXTENDABLE', '配件入库常用原因，门店可补充', NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type_code = 'INBOUND_REASON' AND deleted = 0);

INSERT INTO sys_dict_type (type_code, type_name, status, edit_mode, remark, created_by, created_at, updated_by, updated_at, deleted)
SELECT 'PART_CATEGORY', '配件分类', 'ENABLED', 'STORE_EXTENDABLE', '配件分类，门店可补充', NULL, NOW(), NULL, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE type_code = 'PART_CATEGORY' AND deleted = 0);

INSERT INTO sys_dict_item (type_id, store_id, scope, item_code, item_name, sort_order, status, is_system, remark, created_by, created_at, updated_by, updated_at, deleted)
SELECT t.id, NULL, 'SYSTEM', seed.item_code, seed.item_name, seed.sort_order, 'ENABLED', 1, 'M30 system seed', NULL, NOW(), NULL, NOW(), 0
FROM sys_dict_type t
JOIN (
    SELECT 'VEHICLE_MODEL' type_code, 'NQI' item_code, 'NQi' item_name, 10 sort_order UNION ALL
    SELECT 'VEHICLE_MODEL', 'MQI', 'MQi', 20 UNION ALL
    SELECT 'VEHICLE_MODEL', 'UQI', 'UQi', 30 UNION ALL
    SELECT 'VEHICLE_MODEL', 'GOVA', 'GOVA', 40 UNION ALL
    SELECT 'VEHICLE_MODEL', 'FQI', 'FQi', 50 UNION ALL
    SELECT 'VEHICLE_MODEL', 'SQI', 'SQi', 60 UNION ALL
    SELECT 'VEHICLE_MODEL', 'KQI', 'KQi', 70 UNION ALL
    SELECT 'VEHICLE_MODEL', 'BQI', 'BQi', 80 UNION ALL
    SELECT 'VEHICLE_MODEL', 'RQI', 'RQi', 90 UNION ALL
    SELECT 'VEHICLE_MODEL', 'XQI', 'XQi', 100 UNION ALL
    SELECT 'VEHICLE_MODEL', 'OTHER', '其他', 999 UNION ALL
    SELECT 'REPAIR_ITEM', 'INSPECTION', '整车检测', 10 UNION ALL
    SELECT 'REPAIR_ITEM', 'BATTERY_CHECK', '电池检测', 20 UNION ALL
    SELECT 'REPAIR_ITEM', 'MOTOR_CHECK', '电机检测', 30 UNION ALL
    SELECT 'REPAIR_ITEM', 'CONTROLLER_CHECK', '控制器检测', 40 UNION ALL
    SELECT 'REPAIR_ITEM', 'BRAKE_REPAIR', '刹车维修', 50 UNION ALL
    SELECT 'REPAIR_ITEM', 'TIRE_REPLACE', '轮胎更换', 60 UNION ALL
    SELECT 'REPAIR_ITEM', 'LIGHT_REPAIR', '灯光维修', 70 UNION ALL
    SELECT 'REPAIR_ITEM', 'WIRING_REPAIR', '线路检修', 80 UNION ALL
    SELECT 'REPAIR_ITEM', 'SHELL_REPLACE', '外壳更换', 90 UNION ALL
    SELECT 'REPAIR_ITEM', 'MAINTENANCE', '常规保养', 100 UNION ALL
    SELECT 'REPAIR_ITEM', 'OTHER', '其他', 999 UNION ALL
    SELECT 'UNIT', 'PIECE', '件', 10 UNION ALL
    SELECT 'UNIT', 'EACH', '个', 20 UNION ALL
    SELECT 'UNIT', 'SET', '套', 30 UNION ALL
    SELECT 'UNIT', 'TIME', '次', 40 UNION ALL
    SELECT 'UNIT', 'PAIR', '副', 50 UNION ALL
    SELECT 'UNIT', 'GROUP', '组', 60 UNION ALL
    SELECT 'UNIT', 'METER', '米', 70 UNION ALL
    SELECT 'UNIT', 'BOTTLE', '瓶', 80 UNION ALL
    SELECT 'NO_CHARGE_REASON', 'OFFICIAL_AFTER_SALES', '官方售后', 10 UNION ALL
    SELECT 'NO_CHARGE_REASON', 'WARRANTY', '质保处理', 20 UNION ALL
    SELECT 'NO_CHARGE_REASON', 'FREE_INSPECTION', '免费检测', 30 UNION ALL
    SELECT 'NO_CHARGE_REASON', 'MANAGER_WAIVE', '老板免单', 40 UNION ALL
    SELECT 'NO_CHARGE_REASON', 'PROMOTION_GIFT', '活动赠送', 50 UNION ALL
    SELECT 'NO_CHARGE_REASON', 'OTHER', '其他', 999 UNION ALL
    SELECT 'REFUND_REASON', 'CUSTOMER_CANCEL', '客户取消', 10 UNION ALL
    SELECT 'REFUND_REASON', 'DUPLICATE_PAYMENT', '重复收款', 20 UNION ALL
    SELECT 'REFUND_REASON', 'AMOUNT_ERROR', '金额录错', 30 UNION ALL
    SELECT 'REFUND_REASON', 'REPAIR_CANCEL', '维修取消', 40 UNION ALL
    SELECT 'REFUND_REASON', 'AFTER_SALES_NEGOTIATION', '售后协商', 50 UNION ALL
    SELECT 'REFUND_REASON', 'OTHER', '其他', 999 UNION ALL
    SELECT 'WORK_ORDER_CANCEL_REASON', 'CUSTOMER_CANCEL', '客户取消', 10 UNION ALL
    SELECT 'WORK_ORDER_CANCEL_REASON', 'CUSTOMER_UNREACHABLE', '无法联系客户', 20 UNION ALL
    SELECT 'WORK_ORDER_CANCEL_REASON', 'PART_UNAVAILABLE', '配件缺货', 30 UNION ALL
    SELECT 'WORK_ORDER_CANCEL_REASON', 'DUPLICATE_ORDER', '重复开单', 40 UNION ALL
    SELECT 'WORK_ORDER_CANCEL_REASON', 'INPUT_ERROR', '录入错误', 50 UNION ALL
    SELECT 'WORK_ORDER_CANCEL_REASON', 'OTHER', '其他', 999 UNION ALL
    SELECT 'INBOUND_REASON', 'PURCHASE_INBOUND', '采购入库', 10 UNION ALL
    SELECT 'INBOUND_REASON', 'INITIAL_STOCK', '初始入库', 20 UNION ALL
    SELECT 'INBOUND_REASON', 'SCAN_NEW_PART', '扫码新增配件入库', 30 UNION ALL
    SELECT 'INBOUND_REASON', 'RETURN_INBOUND', '退件入库', 40 UNION ALL
    SELECT 'INBOUND_REASON', 'STOCK_REPLENISH', '补库存', 50 UNION ALL
    SELECT 'INBOUND_REASON', 'OTHER', '其他', 999 UNION ALL
    SELECT 'PART_CATEGORY', 'BATTERY', '电池类', 10 UNION ALL
    SELECT 'PART_CATEGORY', 'MOTOR', '电机类', 20 UNION ALL
    SELECT 'PART_CATEGORY', 'CONTROLLER', '控制器类', 30 UNION ALL
    SELECT 'PART_CATEGORY', 'TIRE', '轮胎类', 40 UNION ALL
    SELECT 'PART_CATEGORY', 'BRAKE', '刹车类', 50 UNION ALL
    SELECT 'PART_CATEGORY', 'LIGHT', '灯具类', 60 UNION ALL
    SELECT 'PART_CATEGORY', 'SHELL', '外壳类', 70 UNION ALL
    SELECT 'PART_CATEGORY', 'WIRING', '线束类', 80 UNION ALL
    SELECT 'PART_CATEGORY', 'HARDWARE', '五金类', 90 UNION ALL
    SELECT 'PART_CATEGORY', 'OTHER', '其他', 999
) seed ON seed.type_code = t.type_code
WHERE t.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_dict_item i
      WHERE i.type_id = t.id
        AND i.scope = 'SYSTEM'
        AND i.item_code = seed.item_code
        AND i.deleted = 0
  );
