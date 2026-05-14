-- ============================================================
-- Dev-only seed data for Staff read-only API integration test
-- This migration only runs in dev profile (db/dev-migration)
-- NEVER runs in production
-- ============================================================

-- -----------------------------------------------------------
-- 1. sys_user — 3 staff users for X-User-Id header matching
-- -----------------------------------------------------------
INSERT INTO sys_user (
    id, store_id, username, password_hash, real_name, phone,
    wechat_openid, wechat_unionid, status, last_login_at, remark,
    created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (10, 1, 'admin01', '{noop}dev123', '王店长', '13900000001', NULL, NULL, 'ENABLED', NULL, 'Dev seed: 门店管理员', NULL, NOW(), NULL, NOW(), 0),
    (11, 1, 'tech01',  '{noop}dev123', '赵维修', '13900000002', NULL, NULL, 'ENABLED', NULL, 'Dev seed: 维修员',     NULL, NOW(), NULL, NOW(), 0),
    (12, 1, 'front01', '{noop}dev123', '钱前台', '13900000003', NULL, NULL, 'ENABLED', NULL, 'Dev seed: 前台',       NULL, NOW(), NULL, NOW(), 0);

-- user-role relations
INSERT INTO sys_user_role (user_id, role_id, created_by, created_at) VALUES
    (10, 2, NULL, NOW()),
    (11, 4, NULL, NOW()),
    (12, 4, NULL, NOW());

-- -----------------------------------------------------------
-- 2. part — 8 parts (7 ENABLED + 1 DISABLED)
-- -----------------------------------------------------------
INSERT INTO part (
    id, store_id, part_code, official_part_no, part_name, model,
    source, category_code, reference_cost_price, default_barcode,
    location_remark, create_source, status, remark,
    created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (100, 1, 'P-BAT-001', 'OF-BAT-48V20A',  '48V20Ah电池',       '48V20A',  'OFFICIAL',    'BATTERY',  420.0000, 'BC-BAT-001', 'A区货架1层', 'NORMAL', 'ENABLED', 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (101, 1, 'P-BAT-002', 'OF-BAT-60V20A',  '60V20Ah电池',       '60V20A',  'OFFICIAL',    'BATTERY',  560.0000, 'BC-BAT-002', 'A区货架1层', 'NORMAL', 'ENABLED', 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (102, 1, 'P-MOT-001', 'OF-MOT-800W',    '800W电机',          '800W',    'OFFICIAL',    'MOTOR',    380.0000, 'BC-MOT-001', 'B区货架2层', 'NORMAL', 'ENABLED', 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (103, 1, 'P-MOT-002', NULL,              '1000W第三方电机',    '1000W',   'THIRD_PARTY', 'MOTOR',    290.0000, 'BC-MOT-002', 'B区货架2层', 'NORMAL', 'ENABLED', 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (104, 1, 'P-CHR-001', 'OF-CHR-48V',     '48V充电器',         '48V',     'OFFICIAL',    'CHARGER',   85.0000, 'BC-CHR-001', 'C区货架',   'NORMAL', 'ENABLED', 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (105, 1, 'P-CTL-001', 'OF-CTL-60V',     '60V控制器',         '60V',     'OFFICIAL',    'CONTROLLER',150.0000, 'BC-CTL-001', 'C区货架',   'NORMAL', 'ENABLED', 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (106, 1, 'P-TIR-001', NULL,              '3.00-10真空胎',     '3.00-10', 'THIRD_PARTY', 'TIRE',       65.0000, 'BC-TIR-001', 'D区货架',   'NORMAL', 'ENABLED', 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (107, 1, 'P-BRK-001', NULL,              '鼓刹片(已停用)',    '通用',    'THIRD_PARTY', 'BRAKE',      25.0000, 'BC-BRK-001', 'D区货架',   'NORMAL', 'DISABLED','Dev seed: 停用配件，Staff列表不应返回', NULL, NOW(), NULL, NOW(), 0);

-- -----------------------------------------------------------
-- 3. inventory_stock — stock for ENABLED parts
-- -----------------------------------------------------------
INSERT INTO inventory_stock (
    id, store_id, part_id, actual_qty, available_qty, reserved_qty,
    last_flow_id, last_changed_at, remark,
    created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (200, 1, 100, 50, 40, 10, NULL, NOW(), 'Dev seed: 正常库存',    NULL, NOW(), NULL, NOW(), 0),
    (201, 1, 101, 30, 25,  5, NULL, NOW(), 'Dev seed: 正常库存',    NULL, NOW(), NULL, NOW(), 0),
    (202, 1, 102, 20, 18,  2, NULL, NOW(), 'Dev seed: 正常库存',    NULL, NOW(), NULL, NOW(), 0),
    (203, 1, 103, 15, 15,  0, NULL, NOW(), 'Dev seed: 无预占',      NULL, NOW(), NULL, NOW(), 0),
    (204, 1, 104, 60, 55,  5, NULL, NOW(), 'Dev seed: 正常库存',    NULL, NOW(), NULL, NOW(), 0),
    (205, 1, 105, 8,   3,  5, NULL, NOW(), 'Dev seed: 低可用高预占', NULL, NOW(), NULL, NOW(), 0),
    (206, 1, 106, 40, 40,  0, NULL, NOW(), 'Dev seed: 无预占',      NULL, NOW(), NULL, NOW(), 0);

-- -----------------------------------------------------------
-- 4. inventory_flow — sample flows covering all types
-- -----------------------------------------------------------
INSERT INTO inventory_flow (
    id, store_id, inventory_stock_id, part_id, flow_type,
    quantity_delta, actual_before, actual_after,
    available_before, available_after,
    reserved_before, reserved_after,
    business_type, business_id, work_order_id, work_order_charge_item_id,
    operator_id, operated_at, reason, remark, unit_cost,
    created_by, created_at, updated_by, updated_at
) VALUES
    (300, 1, 200, 100, 'INBOUND', 50, 0, 50, 0, 50, 0, 0, 'MANUAL_INBOUND', NULL, NULL, NULL, 10, '2026-05-01 09:00:00', 'Dev seed: 初始入库', NULL, 420.0000, NULL, NOW(), NULL, NOW()),
    (301, 1, 200, 100, 'RESERVE', 10, 50, 50, 50, 40, 0, 10, 'WORK_ORDER', NULL, 400, NULL, 11, '2026-05-05 10:00:00', 'Dev seed: 工单预占', NULL, NULL, NULL, NOW(), NULL, NOW()),
    (302, 1, 202, 102, 'INBOUND', 20, 0, 20, 0, 20, 0, 0, 'MANUAL_INBOUND', NULL, NULL, NULL, 10, '2026-05-02 09:00:00', 'Dev seed: 初始入库', NULL, 380.0000, NULL, NOW(), NULL, NOW()),
    (303, 1, 202, 102, 'RESERVE', 2, 20, 20, 20, 18, 0, 2, 'WORK_ORDER', NULL, 401, NULL, 11, '2026-05-06 11:00:00', 'Dev seed: 工单预占', NULL, NULL, NULL, NOW(), NULL, NOW()),
    (304, 1, 204, 104, 'INBOUND', 60, 0, 60, 0, 60, 0, 0, 'MANUAL_INBOUND', NULL, NULL, NULL, 10, '2026-05-03 09:00:00', 'Dev seed: 初始入库', NULL, 85.0000, NULL, NOW(), NULL, NOW()),
    (305, 1, 204, 104, 'RESERVE', 5, 60, 60, 60, 55, 0, 5, 'WORK_ORDER', NULL, 402, NULL, 11, '2026-05-07 14:00:00', 'Dev seed: 工单预占', NULL, NULL, NULL, NOW(), NULL, NOW()),
    (306, 1, 205, 105, 'INBOUND', 8, 0, 8, 0, 8, 0, 0, 'MANUAL_INBOUND', NULL, NULL, NULL, 10, '2026-05-04 09:00:00', 'Dev seed: 初始入库', NULL, 150.0000, NULL, NOW(), NULL, NOW()),
    (307, 1, 205, 105, 'RESERVE', 5, 8, 8, 8, 3, 0, 5, 'WORK_ORDER', NULL, 403, NULL, 11, '2026-05-08 10:00:00', 'Dev seed: 工单预占', NULL, NULL, NULL, NOW(), NULL, NOW()),
    (308, 1, 201, 101, 'INBOUND', 30, 0, 30, 0, 30, 0, 0, 'MANUAL_INBOUND', NULL, NULL, NULL, 10, '2026-05-02 10:00:00', 'Dev seed: 初始入库', NULL, 560.0000, NULL, NOW(), NULL, NOW()),
    (309, 1, 201, 101, 'RESERVE', 5, 30, 30, 30, 25, 0, 5, 'WORK_ORDER', NULL, 404, NULL, 11, '2026-05-09 09:00:00', 'Dev seed: 工单预占', NULL, NULL, NULL, NOW(), NULL, NOW()),
    (310, 1, 203, 103, 'INBOUND', 15, 0, 15, 0, 15, 0, 0, 'MANUAL_INBOUND', NULL, NULL, NULL, 10, '2026-05-03 10:00:00', 'Dev seed: 初始入库', NULL, 290.0000, NULL, NOW(), NULL, NOW()),
    (311, 1, 206, 106, 'INBOUND', 40, 0, 40, 0, 40, 0, 0, 'MANUAL_INBOUND', NULL, NULL, NULL, 10, '2026-05-04 10:00:00', 'Dev seed: 初始入库', NULL, 65.0000, NULL, NOW(), NULL, NOW()),
    (312, 1, 200, 100, 'CONSUME', 10, 50, 40, 40, 40, 10, 0, 'WORK_ORDER', 405, 405, 506, 11, '2026-05-10 16:00:00', 'Dev seed: 工单结算消耗', NULL, NULL, NULL, NOW(), NULL, NOW()),
    (313, 1, 200, 100, 'ADJUST', 5, 40, 45, 40, 45, 0, 0, 'MANUAL_ADJUST', NULL, NULL, NULL, 10, '2026-05-11 09:00:00', 'Dev seed: 盘点调整', NULL, NULL, NULL, NOW(), NULL, NOW());

-- Update inventory_stock to match flow state after CONSUME + ADJUST
UPDATE inventory_stock SET actual_qty = 45, available_qty = 45, reserved_qty = 0, last_changed_at = '2026-05-11 09:00:00' WHERE id = 200;

-- -----------------------------------------------------------
-- 5. customer + vehicle — 3 customers with vehicles
-- -----------------------------------------------------------
INSERT INTO customer (
    id, store_id, customer_name, phone, remark,
    created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (500, 1, '刘明', '13700000001', 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (501, 1, '陈红', '13700000002', 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (502, 1, '孙强', '13700000003', 'Dev seed', NULL, NOW(), NULL, NOW(), 0);

INSERT INTO vehicle (
    id, store_id, customer_id, model, frame_no, battery_no, remark,
    created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (600, 1, 500, '小牛N1S',    'LFV000001DEV00001', 'BAT-DEV-0001', 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (601, 1, 501, '小牛M+',     'LFV000002DEV00002', 'BAT-DEV-0002', 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (602, 1, 502, '九号C80',    'LFV000003DEV00003', NULL,            'Dev seed', NULL, NOW(), NULL, NOW(), 0);

-- -----------------------------------------------------------
-- 6. work_order — 6 orders covering key statuses
-- -----------------------------------------------------------
INSERT INTO work_order (
    id, store_id, work_order_no, customer_id, vehicle_id,
    customer_name_snapshot, customer_phone_snapshot,
    vehicle_model_snapshot, frame_no_snapshot, battery_no_snapshot,
    repair_item, status,
    receivable_amount, received_amount,
    submitted_by, submitted_at, settled_by, settled_at,
    cancelled_by, cancelled_at, cancel_reason,
    remark,
    created_by, created_at, updated_by, updated_at, deleted
) VALUES
    -- DRAFT
    (400, 1, 'WO-20260510-001', 500, 600,
     '刘明', '13700000001', '小牛N1S', 'LFV000001DEV00001', 'BAT-DEV-0001',
     '更换电池', 'DRAFT',
     420.00, 0.00,
     NULL, NULL, NULL, NULL, NULL, NULL, NULL,
     'Dev seed: 草稿工单',
     11, '2026-05-10 09:00:00', NULL, NOW(), 0),

    -- PENDING_ACCEPT
    (401, 1, 'WO-20260511-001', 501, 601,
     '陈红', '13700000002', '小牛M+', 'LFV000002DEV00002', 'BAT-DEV-0002',
     '电机异响', 'PENDING_ACCEPT',
     380.00, 0.00,
     12, '2026-05-11 10:00:00', NULL, NULL, NULL, NULL, NULL,
     'Dev seed: 待接单',
     12, '2026-05-11 09:30:00', NULL, NOW(), 0),

    -- ACCEPTED
    (402, 1, 'WO-20260512-001', 502, 602,
     '孙强', '13700000003', '九号C80', 'LFV000003DEV00003', NULL,
     '充电器故障', 'ACCEPTED',
     85.00, 0.00,
     12, '2026-05-12 09:00:00', NULL, NULL, NULL, NULL, NULL,
     'Dev seed: 已接单',
     12, '2026-05-12 08:30:00', NULL, NOW(), 0),

    -- PART_ORDERED
    (403, 1, 'WO-20260508-002', 500, 600,
     '刘明', '13700000001', '小牛N1S', 'LFV000001DEV00001', 'BAT-DEV-0001',
     '控制器损坏', 'PART_ORDERED',
     150.00, 0.00,
     12, '2026-05-08 14:00:00', NULL, NULL, NULL, NULL, NULL,
     'Dev seed: 已定件',
     12, '2026-05-08 13:00:00', NULL, NOW(), 0),

    -- PART_ARRIVED
    (404, 1, 'WO-20260509-001', 501, 601,
     '陈红', '13700000002', '小牛M+', 'LFV000002DEV00002', 'BAT-DEV-0002',
     '轮胎破损', 'PART_ARRIVED',
     65.00, 0.00,
     12, '2026-05-09 10:00:00', NULL, NULL, NULL, NULL, NULL,
     'Dev seed: 已到件',
     12, '2026-05-09 09:00:00', NULL, NOW(), 0),

    -- SETTLED (with payment)
    (405, 1, 'WO-20260505-001', 500, 600,
     '刘明', '13700000001', '小牛N1S', 'LFV000001DEV00001', 'BAT-DEV-0001',
     '更换电池+工时费', 'SETTLED',
     520.00, 520.00,
     12, '2026-05-05 10:00:00', 10, '2026-05-10 16:00:00', NULL, NULL, NULL,
     'Dev seed: 已结算',
     11, '2026-05-05 09:00:00', NULL, NOW(), 0);

-- -----------------------------------------------------------
-- 7. work_order_charge_item — charge items covering PART/LABOR/OTHER
-- -----------------------------------------------------------
INSERT INTO work_order_charge_item (
    id, store_id, work_order_id, charge_type, item_name,
    part_id, part_code_snapshot, part_name_snapshot, part_source_snapshot,
    quantity, unit, unit_price, line_amount,
    cost_price_snapshot, line_cost_amount,
    inventory_affecting, is_temp_part, status, remark,
    created_by, created_at, updated_by, updated_at, deleted
) VALUES
    -- WO-400 (DRAFT): 1 PART
    (500, 1, 400, 'PART', '48V20Ah电池',
     100, 'P-BAT-001', '48V20Ah电池', 'OFFICIAL',
     1, '个', 420.00, 420.00,
     420.0000, 420.00,
     1, 0, 'ACTIVE', 'Dev seed',
     11, NOW(), NULL, NOW(), 0),

    -- WO-401 (PENDING_ACCEPT): 1 PART + 1 LABOR
    (501, 1, 401, 'PART', '800W电机',
     102, 'P-MOT-001', '800W电机', 'OFFICIAL',
     1, '个', 380.00, 380.00,
     380.0000, 380.00,
     1, 0, 'ACTIVE', 'Dev seed',
     12, NOW(), NULL, NOW(), 0),
    (502, 1, 401, 'LABOR', '电机更换工时',
     NULL, NULL, NULL, NULL,
     1, '次', 0.00, 0.00,
     NULL, NULL,
     0, 0, 'ACTIVE', 'Dev seed: LABOR无partId',
     12, NOW(), NULL, NOW(), 0),

    -- WO-402 (ACCEPTED): 1 PART
    (503, 1, 402, 'PART', '48V充电器',
     104, 'P-CHR-001', '48V充电器', 'OFFICIAL',
     1, '个', 85.00, 85.00,
     85.0000, 85.00,
     1, 0, 'ACTIVE', 'Dev seed',
     12, NOW(), NULL, NOW(), 0),

    -- WO-403 (PART_ORDERED): 1 PART
    (504, 1, 403, 'PART', '60V控制器',
     105, 'P-CTL-001', '60V控制器', 'OFFICIAL',
     1, '个', 150.00, 150.00,
     150.0000, 150.00,
     1, 0, 'ACTIVE', 'Dev seed',
     12, NOW(), NULL, NOW(), 0),

    -- WO-404 (PART_ARRIVED): 1 PART
    (505, 1, 404, 'PART', '3.00-10真空胎',
     106, 'P-TIR-001', '3.00-10真空胎', 'THIRD_PARTY',
     1, '条', 65.00, 65.00,
     65.0000, 65.00,
     1, 0, 'ACTIVE', 'Dev seed',
     12, NOW(), NULL, NOW(), 0),

    -- WO-405 (SETTLED): 1 PART + 1 LABOR + 1 OTHER
    (506, 1, 405, 'PART', '48V20Ah电池',
     100, 'P-BAT-001', '48V20Ah电池', 'OFFICIAL',
     1, '个', 420.00, 420.00,
     420.0000, 420.00,
     1, 0, 'ACTIVE', 'Dev seed',
     11, NOW(), NULL, NOW(), 0),
    (507, 1, 405, 'LABOR', '电池更换工时',
     NULL, NULL, NULL, NULL,
     1, '次', 80.00, 80.00,
     NULL, NULL,
     0, 0, 'ACTIVE', 'Dev seed: LABOR无partId',
     11, NOW(), NULL, NOW(), 0),
    (508, 1, 405, 'OTHER', '上门服务费',
     NULL, NULL, NULL, NULL,
     1, '次', 20.00, 20.00,
     NULL, NULL,
     0, 0, 'ACTIVE', 'Dev seed: OTHER无partId',
     11, NOW(), NULL, NOW(), 0);

-- -----------------------------------------------------------
-- 8. payment_record / refund_record — minimal for SETTLED order
-- -----------------------------------------------------------
INSERT INTO payment_record (
    id, store_id, work_order_id, payment_no, amount,
    payment_method, paid_at, receiver_id, operator_id, remark,
    created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (700, 1, 405, 'PAY-20260510-001', 520.00,
     'WECHAT', '2026-05-10 15:30:00', 10, 12, 'Dev seed: 全额微信支付',
     12, NOW(), NULL, NOW(), 0);

-- No refund for now; add later if needed for admin testing

-- -----------------------------------------------------------
-- 9. work_order_status_log — status transitions for settled order
-- -----------------------------------------------------------
INSERT INTO work_order_status_log (
    id, store_id, work_order_id, from_status, to_status,
    action_type, operator_id, operated_at, reason, remark,
    created_by, created_at, updated_by, updated_at
) VALUES
    (800, 1, 405, NULL, 'DRAFT', 'CREATE', 11, '2026-05-05 09:00:00', NULL, 'Dev seed', NULL, NOW(), NULL, NOW()),
    (801, 1, 405, 'DRAFT', 'PENDING_ACCEPT', 'SUBMIT', 12, '2026-05-05 10:00:00', NULL, 'Dev seed', NULL, NOW(), NULL, NOW()),
    (802, 1, 405, 'PENDING_ACCEPT', 'ACCEPTED', 'UPDATE_STATUS', 11, '2026-05-06 09:00:00', NULL, 'Dev seed', NULL, NOW(), NULL, NOW()),
    (803, 1, 405, 'ACCEPTED', 'PART_ARRIVED', 'UPDATE_STATUS', 11, '2026-05-08 09:00:00', NULL, 'Dev seed', NULL, NOW(), NULL, NOW()),
    (804, 1, 405, 'PART_ARRIVED', 'SETTLED', 'SETTLE', 10, '2026-05-10 16:00:00', NULL, 'Dev seed', NULL, NOW(), NULL, NOW());

-- -----------------------------------------------------------
-- 10. official_after_sales — for settled order with official part
-- -----------------------------------------------------------
INSERT INTO official_after_sales (
    id, store_id, work_order_id, is_official_after_sales,
    official_order_no, official_settlement_amount,
    official_settlement_status, official_settlement_time,
    official_settlement_operator_id, official_settlement_remark,
    remark,
    created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (900, 1, 405, 1,
     'OF-20260505-001', 420.00,
     'SETTLED', '2026-05-12 10:00:00',
     10, 'Dev seed: 官方结算完成',
     'Dev seed',
     NULL, NOW(), NULL, NOW(), 0);

-- -----------------------------------------------------------
-- 11. Add PART_CATEGORY dict type + items (missing from V2 seed)
-- -----------------------------------------------------------
INSERT INTO sys_dict_type (
    id, type_code, type_name, status, remark,
    created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (2008, 'PART_CATEGORY', '配件分类', 'ENABLED', 'Dev seed: 配件分类字典',
     NULL, NOW(), NULL, NOW(), 0);

INSERT INTO sys_dict_item (
    type_id, item_code, item_name, sort_order, status, is_system, remark,
    created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (2008, 'BATTERY',    '电池',   1, 'ENABLED',  0, 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (2008, 'MOTOR',      '电机',   2, 'ENABLED',  0, 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (2008, 'CHARGER',    '充电器', 3, 'ENABLED',  0, 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (2008, 'CONTROLLER', '控制器', 4, 'ENABLED',  0, 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (2008, 'TIRE',       '轮胎',   5, 'ENABLED',  0, 'Dev seed', NULL, NOW(), NULL, NOW(), 0),
    (2008, 'BRAKE',      '刹车',   6, 'DISABLED', 0, 'Dev seed', NULL, NOW(), NULL, NOW(), 0);
