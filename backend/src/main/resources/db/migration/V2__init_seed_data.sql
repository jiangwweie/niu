INSERT INTO store (
    id, store_code, store_name, contact_phone, address, status, remark, created_by, created_at, updated_by, updated_at, deleted
) VALUES (
    1, 'DEFAULT', '默认门店', NULL, NULL, 'ENABLED', 'Default single-store MVP seed', NULL, NOW(), NULL, NOW(), 0
);

INSERT INTO sys_role (
    id, store_id, role_code, role_name, status, sort_order, remark, created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (1, 1, 'SUPER_ADMIN', '超级管理员', 'ENABLED', 1, 'Seed role', NULL, NOW(), NULL, NOW(), 0),
    (2, 1, 'STORE_ADMIN', '门店管理员', 'ENABLED', 2, 'Seed role', NULL, NOW(), NULL, NOW(), 0),
    (3, 1, 'FINANCE', '财务', 'ENABLED', 3, 'Seed role', NULL, NOW(), NULL, NOW(), 0),
    (4, 1, 'TECHNICIAN_FRONT_DESK', '维修员/前台', 'ENABLED', 4, 'Seed role', NULL, NOW(), NULL, NOW(), 0);

INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order, remark, created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (1001, 'USER_MANAGE', '用户管理', 'USER', 'ENABLED', 1, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1002, 'ROLE_MANAGE', '角色权限管理', 'USER', 'ENABLED', 2, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1003, 'DICT_MANAGE', '字典配置', 'DICT', 'ENABLED', 3, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1004, 'PART_MANAGE', '配件管理', 'PART', 'ENABLED', 4, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1005, 'INVENTORY_VIEW', '库存查看', 'INVENTORY', 'ENABLED', 5, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1006, 'INVENTORY_INBOUND', '库存入库', 'INVENTORY', 'ENABLED', 6, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1007, 'INVENTORY_ADJUST', '库存调整', 'INVENTORY', 'ENABLED', 7, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1008, 'WORK_ORDER_CREATE', '工单创建', 'WORK_ORDER', 'ENABLED', 8, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1009, 'WORK_ORDER_UPDATE', '工单更新', 'WORK_ORDER', 'ENABLED', 9, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1010, 'WORK_ORDER_SUBMIT', '工单提交', 'WORK_ORDER', 'ENABLED', 10, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1011, 'WORK_ORDER_CANCEL', '工单取消', 'WORK_ORDER', 'ENABLED', 11, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1012, 'WORK_ORDER_SETTLE', '工单结算', 'WORK_ORDER', 'ENABLED', 12, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1013, 'PAYMENT_RECORD', '支付记录', 'PAYMENT', 'ENABLED', 13, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1014, 'REFUND_RECORD', '退款记录', 'PAYMENT', 'ENABLED', 14, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1015, 'OFFICIAL_SETTLEMENT_MANAGE', '官方结算管理', 'OFFICIAL', 'ENABLED', 15, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1016, 'REIMBURSEMENT_SUBMIT', '报销提交', 'REIMBURSEMENT', 'ENABLED', 16, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1017, 'REIMBURSEMENT_CONFIRM', '报销确认', 'REIMBURSEMENT', 'ENABLED', 17, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1018, 'FINANCE_VIEW', '财务查看', 'FINANCE', 'ENABLED', 18, 'Seed permission', NULL, NOW(), NULL, NOW(), 0),
    (1019, 'EXCEL_EXPORT', 'Excel导出', 'EXPORT', 'ENABLED', 19, 'Seed permission', NULL, NOW(), NULL, NOW(), 0);

INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT 1, id, NULL, NOW()
FROM sys_permission;

INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at) VALUES
    (2, 1004, NULL, NOW()),
    (2, 1005, NULL, NOW()),
    (2, 1006, NULL, NOW()),
    (2, 1007, NULL, NOW()),
    (2, 1008, NULL, NOW()),
    (2, 1009, NULL, NOW()),
    (2, 1010, NULL, NOW()),
    (2, 1011, NULL, NOW()),
    (2, 1012, NULL, NOW()),
    (2, 1013, NULL, NOW()),
    (2, 1014, NULL, NOW()),
    (2, 1015, NULL, NOW()),
    (2, 1016, NULL, NOW()),
    (2, 1017, NULL, NOW()),
    (2, 1018, NULL, NOW()),
    (2, 1019, NULL, NOW()),
    (3, 1013, NULL, NOW()),
    (3, 1014, NULL, NOW()),
    (3, 1015, NULL, NOW()),
    (3, 1017, NULL, NOW()),
    (3, 1018, NULL, NOW()),
    (3, 1019, NULL, NOW()),
    (4, 1005, NULL, NOW()),
    (4, 1006, NULL, NOW()),
    (4, 1008, NULL, NOW()),
    (4, 1009, NULL, NOW()),
    (4, 1010, NULL, NOW()),
    (4, 1011, NULL, NOW()),
    (4, 1012, NULL, NOW()),
    (4, 1013, NULL, NOW()),
    (4, 1016, NULL, NOW());

INSERT INTO sys_dict_type (
    id, type_code, type_name, status, remark, created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (2001, 'WORK_ORDER_STATUS', '工单状态', 'ENABLED', 'Seed dictionary type', NULL, NOW(), NULL, NOW(), 0),
    (2002, 'PAYMENT_METHOD', '支付方式', 'ENABLED', 'Seed dictionary type', NULL, NOW(), NULL, NOW(), 0),
    (2003, 'PART_SOURCE', '配件来源', 'ENABLED', 'Seed dictionary type', NULL, NOW(), NULL, NOW(), 0),
    (2004, 'INVENTORY_FLOW_TYPE', '库存流水类型', 'ENABLED', 'Seed dictionary type', NULL, NOW(), NULL, NOW(), 0),
    (2005, 'OFFICIAL_SETTLEMENT_STATUS', '官方结算状态', 'ENABLED', 'Seed dictionary type', NULL, NOW(), NULL, NOW(), 0),
    (2006, 'REIMBURSEMENT_STATUS', '报销状态', 'ENABLED', 'Seed dictionary type', NULL, NOW(), NULL, NOW(), 0),
    (2007, 'CHARGE_ITEM_TYPE', '费用项目类型', 'ENABLED', 'Seed dictionary type', NULL, NOW(), NULL, NOW(), 0);

INSERT INTO sys_dict_item (
    type_id, item_code, item_name, sort_order, status, is_system, remark, created_by, created_at, updated_by, updated_at, deleted
) VALUES
    (2001, 'DRAFT', '草稿', 1, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2001, 'PENDING_ACCEPT', '待接单', 2, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2001, 'ACCEPTED', '已接单', 3, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2001, 'PART_ORDERED', '已定件', 4, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2001, 'PART_ARRIVED', '已到件', 5, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2001, 'SETTLED', '已结算', 6, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2001, 'CANCELLED', '已取消', 7, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2002, 'WECHAT', '微信', 1, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2002, 'ALIPAY', '支付宝', 2, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2002, 'UNIONPAY', '银联', 3, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2002, 'CASH', '现金', 4, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2002, 'OTHER', '其他', 5, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2003, 'OFFICIAL', '官方', 1, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2003, 'THIRD_PARTY', '第三方', 2, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2004, 'INBOUND', '入库', 1, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2004, 'RESERVE', '预占', 2, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2004, 'RELEASE', '释放', 3, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2004, 'CONSUME', '消耗', 4, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2004, 'ADJUST', '调整', 5, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2005, 'NOT_REQUIRED', '无需结算', 1, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2005, 'PENDING', '待结算', 2, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2005, 'SETTLED', '已结算', 3, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2006, 'PENDING', '待确认', 1, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2006, 'CONFIRMED', '已确认', 2, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2006, 'REJECTED_OR_CANCELLED', '已驳回/取消', 3, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2007, 'PART', '配件费', 1, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2007, 'LABOR', '工时费', 2, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0),
    (2007, 'OTHER', '其他费用', 3, 'ENABLED', 1, 'Seed dictionary item', NULL, NOW(), NULL, NOW(), 0);
