-- ============================================================================
-- M16 试运行初始化账号脚本
-- ============================================================================
--
-- 用途：首次部署 / 试运行环境初始化可登录账号
--
-- 安全要求：
--   1. 本脚本仅用于试运行 / 首次部署初始化，不是长期默认账号机制
--   2. 所有密码使用 bcrypt hash，禁止使用 {noop}
--   3. 首次登录后必须立即修改密码
--   4. 正式交付前应设计受控的 bootstrap admin 流程替代本脚本
--
-- 使用方式：
--   1. 确认 store 表已有 id=1 的门店记录
--   2. 确认 sys_role 表已有 SUPER_ADMIN / STORE_ADMIN / FINANCE / TECHNICIAN_FRONT_DESK 角色
--   3. 确认 sys_permission 表已有全部权限码
--   4. 手动执行本脚本（不要放入 Flyway migration）
--   5. 使用对应账号登录后立即修改密码
--
-- 多门店 / SaaS 演进说明：
--   当前为单门店 MVP，系统超管账号暂绑定 store_id=1
--   该账号业务语义是"软件商 / 系统拥有者超管"，不是普通门店员工
--   后续如演进为多门店平台或 SaaS，应补 account_type / user_type 或 platform_admin 概念
--   本次不新增 account_type / user_type migration
--
-- ============================================================================

-- 密码说明：
--   示例密码均为 Trial@2026!
--   bcrypt hash 由 Spring Security DelegatingPasswordEncoder 格式生成
--   前缀 {bcrypt} 表示使用 bcrypt 算法
--   首次登录后必须修改密码

-- ============================================================================
-- 1. 系统超管 / 软件商管理员
-- ============================================================================
-- 用途：系统拥有者初始超管账号，用于初始化系统、首次登录、创建/管理门店账号、排障和交付验收
-- 角色：SUPER_ADMIN（通过角色继承全部权限）
-- 语义：软件商 / 系统拥有者，不是普通门店员工
-- 兼容：当前单门店 MVP 下绑定 store_id=1，后续演进时应解耦

INSERT INTO sys_user (id, store_id, username, password_hash, real_name, phone, wechat_openid, wechat_unionid, status, last_login_at, remark, created_by, created_at, updated_by, updated_at, deleted)
VALUES (1, 1, 'system_admin',
        '{bcrypt}$2a$10$EqKcp1WFKVQISheBxnFOheYMKMuiEPOBGYFBEglVPOKJlFHNyxiMK',
        '系统管理员', '13800000001', NULL, NULL, 'ENABLED', NULL, 'Trial init: 系统超管 / 软件商管理员', NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE username = username;

INSERT INTO sys_user_role (id, user_id, role_id, created_by, created_at)
VALUES (1, 1, 1, NULL, NOW())
ON DUPLICATE KEY UPDATE role_id = role_id;

-- ============================================================================
-- 2. 门店管理员（试运行）
-- ============================================================================
-- 用途：门店管理员试运行账号
-- 角色：STORE_ADMIN

INSERT INTO sys_user (id, store_id, username, password_hash, real_name, phone, wechat_openid, wechat_unionid, status, last_login_at, remark, created_by, created_at, updated_by, updated_at, deleted)
VALUES (2, 1, 'trial_store_admin',
        '{bcrypt}$2a$10$EqKcp1WFKVQISheBxnFOheYMKMuiEPOBGYFBEglVPOKJlFHNyxiMK',
        '门店管理员', '13800000002', NULL, NULL, 'ENABLED', NULL, 'Trial init: 门店管理员', NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE username = username;

INSERT INTO sys_user_role (id, user_id, role_id, created_by, created_at)
VALUES (2, 2, 2, NULL, NOW())
ON DUPLICATE KEY UPDATE role_id = role_id;

-- ============================================================================
-- 3. 财务人员（试运行）
-- ============================================================================
-- 用途：财务报表 / 导出权限验证
-- 角色：FINANCE

INSERT INTO sys_user (id, store_id, username, password_hash, real_name, phone, wechat_openid, wechat_unionid, status, last_login_at, remark, created_by, created_at, updated_by, updated_at, deleted)
VALUES (3, 1, 'trial_finance',
        '{bcrypt}$2a$10$EqKcp1WFKVQISheBxnFOheYMKMuiEPOBGYFBEglVPOKJlFHNyxiMK',
        '财务人员', '13800000003', NULL, NULL, 'ENABLED', NULL, 'Trial init: 财务人员', NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE username = username;

INSERT INTO sys_user_role (id, user_id, role_id, created_by, created_at)
VALUES (3, 3, 3, NULL, NOW())
ON DUPLICATE KEY UPDATE role_id = role_id;

-- ============================================================================
-- 4. 员工 / 技师前台（试运行）
-- ============================================================================
-- 用途：员工小程序主链路验证
-- 角色：TECHNICIAN_FRONT_DESK

INSERT INTO sys_user (id, store_id, username, password_hash, real_name, phone, wechat_openid, wechat_unionid, status, last_login_at, remark, created_by, created_at, updated_by, updated_at, deleted)
VALUES (4, 1, 'trial_staff',
        '{bcrypt}$2a$10$EqKcp1WFKVQISheBxnFOheYMKMuiEPOBGYFBEglVPOKJlFHNyxiMK',
        '员工', '13800000004', NULL, NULL, 'ENABLED', NULL, 'Trial init: 员工/技师前台', NULL, NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE username = username;

INSERT INTO sys_user_role (id, user_id, role_id, created_by, created_at)
VALUES (4, 4, 4, NULL, NOW())
ON DUPLICATE KEY UPDATE role_id = role_id;

-- ============================================================================
-- 注意事项
-- ============================================================================
-- 1. sys_user.id 使用固定值，确保与 dev-migration V5 的 id 不冲突
--    V5 使用 id 10/11/12，本脚本使用 id 1/2/3/4
--    如需同时使用 dev 种子，请确认 id 不冲突
-- 2. ON DUPLICATE KEY UPDATE 确保脚本可重复执行
-- 3. 所有账号使用相同示例密码，首次登录后必须修改
-- 4. system_admin 是软件商超管，不是门店员工
--    当前绑定 store_id=1 是单门店 MVP 技术兼容
--    后续多门店/SaaS 演进时应引入 platform_admin 概念
-- 5. 本脚本不放入 Flyway migration，由部署人员手动执行
