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
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, status, deleted) KEY (phone) VALUES (1, 1, 'admin01', '{noop}dev123', '张三', '13800000001', 'ENABLED', 0);
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, status, deleted) KEY (phone) VALUES (2, 1, 'tech01', '{noop}dev123', '李四', '13800000002', 'ENABLED', 0);
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, status, deleted) KEY (phone) VALUES (3, 1, 'disabled01', '{noop}dev123', '停用员工', '13800000003', 'DISABLED', 0);
MERGE INTO sys_user (id, store_id, username, password_hash, real_name, phone, status, deleted) KEY (phone) VALUES (4, 1, 'deleted01', '{noop}dev123', '删除员工', '13800000004', 'ENABLED', 1);

-- roles
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (1, 1, 'ADMIN', '管理员', 'ENABLED');
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (2, 1, 'TECHNICIAN', '技术员', 'ENABLED');
MERGE INTO sys_role (id, store_id, role_code, role_name, status) KEY (store_id, role_code) VALUES (3, 1, 'DISCOUNT_STAFF', '折扣员', 'DISABLED');

-- permissions
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (1, 'work_order:create', '创建工单', 'WORK_ORDER', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (2, 'work_order:settle', '结算工单', 'WORK_ORDER', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (3, 'inventory:manage', '库存管理', 'INVENTORY', 'ENABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (4, 'payment:refund', '退款', 'PAYMENT', 'DISABLED');
MERGE INTO sys_permission (id, permission_code, permission_name, module_code, status) KEY (permission_code) VALUES (5, 'report:view', '报表查看', 'REPORT', 'ENABLED');

-- user-role relations
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (1, 1, 1);
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (2, 2, 2);
MERGE INTO sys_user_role (id, user_id, role_id) KEY (user_id, role_id) VALUES (3, 1, 3);

-- role-permission relations
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (1, 1, 1);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (2, 1, 2);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (3, 1, 3);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (4, 2, 1);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (5, 2, 4);
MERGE INTO sys_role_permission (id, role_id, permission_id) KEY (role_id, permission_id) VALUES (6, 3, 5);
