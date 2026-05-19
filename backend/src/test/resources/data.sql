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

MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, status, password_must_change, deleted) KEY (phone) VALUES (1, 1, 'admin01', '{noop}dev123', '张三', '13800000001', 'ENABLED', FALSE, 0);
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, status, password_must_change, deleted) KEY (phone) VALUES (2, 1, 'tech01', '{noop}dev123', '李四', '13800000002', 'ENABLED', FALSE, 0);
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, status, password_must_change, deleted) KEY (phone) VALUES (3, 1, 'disabled01', '{noop}dev123', '停用员工', '13800000003', 'DISABLED', FALSE, 0);
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, status, password_must_change, deleted) KEY (phone) VALUES (4, 1, 'deleted01', '{noop}dev123', '删除员工', '13800000004', 'ENABLED', FALSE, 1);

-- roles
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (1, 1, 'ADMIN', '管理员', 'ENABLED');
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (2, 1, 'TECHNICIAN', '技术员', 'ENABLED');
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (3, 1, 'DISCOUNT_STAFF', '折扣员', 'DISABLED');
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (4, 1, 'SUPER_ADMIN', '超级管理员', 'ENABLED');

-- permissions
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
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1021, 'WORK_ORDER_UPDATE', '工单编辑', 'WORK_ORDER', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1022, 'USER_MANAGE', '用户管理', 'USER', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1023, 'ROLE_MANAGE', '角色权限管理', 'USER', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1024, 'STORE_MANAGE', '门店配置管理', 'STORE', 'ENABLED');

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
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1021, 1, 1021);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1019, 1, 1019);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1022, 1, 1022);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1023, 1, 1023);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1024, 1, 1024);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1025, 4, 1022);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1026, 4, 1023);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1027, 4, 1024);

-- M17A hardening: cross-store test data
MERGE INTO store (id, store_code, store_name, contact_name, contact_phone, address, status, deleted) KEY (id) VALUES (2, 'STORE2', '第二门店', '李店长', '13800000010', '第二门店地址', 'ENABLED', 0);
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (5, 2, 'STORE_ADMIN', '门店管理员', 'ENABLED');
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, status, password_must_change, deleted) KEY (phone) VALUES (10, 2, 'store_admin01', '{noop}dev123', '王二', '13800000010', 'ENABLED', FALSE, 0);
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, status, password_must_change, deleted) KEY (phone) VALUES (11, 2, 'test02', '{noop}dev123', '测试用户2', '13800000011', 'ENABLED', FALSE, 0);
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (201, 10, 5);
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (202, 11, 5);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (2001, 5, 1022);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (2002, 5, 1023);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (2003, 5, 1024);
