-- Idempotent test data for Task 5 (uses MERGE INTO for H2 re-runs)

-- dict types
MERGE INTO sys_dict_type (id, type_code, type_name, status) KEY (type_code) VALUES (1, 'PART_CATEGORY', '零件分类', 'ENABLED');
MERGE INTO sys_dict_type (id, type_code, type_name, status) KEY (type_code) VALUES (2, 'REPAIR_TYPE', '维修类型', 'DISABLED');
MERGE INTO sys_dict_type (id, type_code, type_name, status) KEY (type_code) VALUES (3, 'PRIORITY', '优先级', 'ENABLED');
UPDATE sys_dict_type SET deleted = 1 WHERE type_code = 'PRIORITY';

-- dict items
MERGE INTO sys_dict_item (id, type_id, item_code, item_name, sort_order, status, is_system) KEY (type_id, item_code) VALUES (1, 1, 'BATTERY', '电池', 1, 'ENABLED', 0);
MERGE INTO sys_dict_item (id, type_id, item_code, item_name, sort_order, status, is_system) KEY (type_id, item_code) VALUES (2, 1, 'MOTOR', '电机', 2, 'ENABLED', 0);
MERGE INTO sys_dict_item (id, type_id, item_code, item_name, sort_order, status, is_system) KEY (type_id, item_code) VALUES (3, 1, 'BRAKE', '刹车', 3, 'DISABLED', 0);

-- users
MERGE INTO store (id, store_code, store_name, contact_name, contact_phone, address, status, deleted) KEY (id) VALUES (1, 'DEFAULT', '默认门店', '王店长', '13800000000', '测试地址', 'ENABLED', 0);

MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, account_type, status, password_must_change, deleted) KEY (phone) VALUES (1, 1, 'admin01', '{noop}dev123', '张三', '13800000001', 'STORE', 'ENABLED', FALSE, 0);
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, account_type, status, password_must_change, deleted, wechat_openid, wechat_bound_at) KEY (phone) VALUES (2, 1, 'tech01', '{noop}dev123', '李四', '13800000002', 'STORE', 'ENABLED', FALSE, 0, 'test_bound_openid_001', TIMESTAMP '2026-05-20 10:00:00');
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, account_type, status, password_must_change, deleted) KEY (phone) VALUES (3, 1, 'disabled01', '{noop}dev123', '停用员工', '13800000003', 'STORE', 'DISABLED', FALSE, 0);
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, account_type, status, password_must_change, deleted) KEY (phone) VALUES (4, 1, 'deleted01', '{noop}dev123', '删除员工', '13800000004', 'STORE', 'ENABLED', FALSE, 1);
-- M19: platform admin test user
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, account_type, status, password_must_change, deleted) KEY (phone) VALUES (20, NULL, 'platform_admin', '{noop}dev123', '平台管理员', '13800000020', 'PLATFORM', 'ENABLED', FALSE, 0);

-- roles
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (1, 1, 'ADMIN', '管理员', 'ENABLED');
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (2, 1, 'TECHNICIAN', '技术员', 'ENABLED');
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (3, 1, 'DISCOUNT_STAFF', '折扣员', 'DISABLED');
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (4, 1, 'SUPER_ADMIN', '超级管理员', 'ENABLED');
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (100, 1, 'STORE_ADMIN', '门店管理员', 'ENABLED');
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (101, 1, 'FINANCE', '财务', 'ENABLED');
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (102, 1, 'TECHNICIAN_FRONT_DESK', '前台员工', 'ENABLED');

-- permissions (ids 1-5 are test-only; 1004+ match V2 seed; 1028-1029 are test-only extras not in V10)
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1, 'work_order:create', '创建工单', 'WORK_ORDER', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (2, 'work_order:settle', '结算工单', 'WORK_ORDER', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (3, 'inventory:manage', '库存管理', 'INVENTORY', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (4, 'payment:refund', '退款', 'PAYMENT', 'DISABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (5, 'report:view', '报表查看', 'REPORT', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1004, 'PART_MANAGE', '配件管理', 'PART', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1005, 'INVENTORY_VIEW', '库存查看', 'INVENTORY', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1006, 'INVENTORY_INBOUND', '库存入库', 'INVENTORY', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1007, 'INVENTORY_ADJUST', '库存调整', 'INVENTORY', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1010, 'WORK_ORDER_SUBMIT', '工单提交', 'WORK_ORDER', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1011, 'WORK_ORDER_CANCEL', '工单取消', 'WORK_ORDER', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1012, 'WORK_ORDER_SETTLE', '工单结算', 'WORK_ORDER', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1013, 'PAYMENT_RECORD', '支付记录', 'PAYMENT', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1014, 'REFUND_RECORD', '退款记录', 'PAYMENT', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1015, 'OFFICIAL_SETTLEMENT_MANAGE', '官方结算管理', 'OFFICIAL', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1016, 'REIMBURSEMENT_SUBMIT', '报销提交', 'REIMBURSEMENT', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1017, 'REIMBURSEMENT_CONFIRM', '报销确认', 'REIMBURSEMENT', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1018, 'FINANCE_VIEW', '财务查看', 'FINANCE', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1019, 'EXCEL_EXPORT', 'Excel导出', 'EXPORT', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1020, 'WORK_ORDER_CREATE', '工单创建', 'WORK_ORDER', 'ENABLED');
-- 1021/1022: defined here (Flyway disabled in test profile, so V10 does not run)
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1021, 'CUSTOMER_VIEW', '客户档案查看', 'CUSTOMER', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1022, 'CUSTOMER_MANAGE', '客户档案管理', 'CUSTOMER', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1023, 'ROLE_MANAGE', '角色权限管理', 'USER', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1024, 'STORE_MANAGE', '门店配置管理', 'STORE', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1028, 'WORK_ORDER_UPDATE', '工单编辑', 'WORK_ORDER', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1029, 'USER_MANAGE', '用户管理', 'USER', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1031, 'PLATFORM_MANAGE', '平台管理', 'PLATFORM', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1032, 'REFUND_AFTER_DELIVERY', '交付后退款', 'PAYMENT', 'ENABLED');

-- user-role relations
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (1, 1, 1);
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (2, 2, 2);
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (3, 1, 3);
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (7, 7, 1);
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (8, 8, 1);
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (101, 101, 1);
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (102, 1, 4);

-- role-permission relations
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1, 1, 1);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (2, 1, 2);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (3, 1, 3);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (4, 2, 1);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (5, 2, 4);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (6, 3, 5);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1004, 1, 1004);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1006, 1, 1006);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1007, 1, 1007);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1010, 1, 1010);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1011, 1, 1011);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1012, 1, 1012);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1013, 1, 1013);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1014, 1, 1014);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1015, 1, 1015);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1016, 1, 1016);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1017, 1, 1017);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1018, 1, 1018);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1005, 1, 1005);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1020, 1, 1020);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1028, 1, 1028);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1019, 1, 1019);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1029, 1, 1029);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1023, 1, 1023);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1024, 1, 1024);
-- 1021 (CUSTOMER_VIEW) and 1022 (CUSTOMER_MANAGE) are assigned to role 1 by V10 migration only for SUPER_ADMIN/STORE_ADMIN/TECHNICIAN_FRONT_DESK codes;
-- ADMIN (role 1) is a test-only code not in V10, so assign explicitly here.
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1030, 1, 1021);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1031, 1, 1022);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1025, 4, 1029);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1026, 4, 1023);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1027, 4, 1024);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1044, 4, 1031);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1045, 4, 1032);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1046, 100, 1032);

-- M19: platform admin role assignment
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (301, 20, 4);
MERGE INTO store (id, store_code, store_name, contact_name, contact_phone, address, status, deleted) KEY (id) VALUES (2, 'STORE2', '第二门店', '李店长', '13800000010', '第二门店地址', 'ENABLED', 0);
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (5, 2, 'STORE_ADMIN', '门店管理员', 'ENABLED');
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, account_type, status, password_must_change, deleted) KEY (phone) VALUES (10, 2, 'store_admin01', '{noop}dev123', '王二', '13800000010', 'STORE', 'ENABLED', FALSE, 0);
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, account_type, status, password_must_change, deleted) KEY (phone) VALUES (11, 2, 'test02', '{noop}dev123', '测试用户2', '13800000011', 'STORE', 'ENABLED', FALSE, 0);
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (201, 10, 5);
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (202, 11, 5);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (2001, 5, 1029);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (2002, 5, 1023);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (2003, 5, 1024);
-- M18 customer/vehicle permissions for store2 admin role (via V10 migration auto-assign)
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1042, 5, 1021);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1043, 5, 1022);
-- M19 template roles for store_id=1 (used by createBaseRolesForStore)
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1050, 100, 1004);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1051, 100, 1010);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1052, 100, 1021);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1053, 100, 1022);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1054, 101, 1024);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1055, 102, 1010);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1056, 102, 1023);
-- bindtest01: dedicated user for bindWechatThenAuthMeShowsWechatBoundTrue (no wechat binding)
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, account_type, status, password_must_change, deleted) KEY (phone) VALUES (30, 1, 'bindtest01', '{noop}dev123', '绑定测试员', '13800000030', 'STORE', 'ENABLED', FALSE, 0);
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (401, 30, 1);
