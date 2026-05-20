-- M19: Account type & multi-store minimal boundary

-- 1. Add account_type to sys_user
ALTER TABLE sys_user ADD COLUMN account_type VARCHAR(20) NOT NULL DEFAULT 'STORE';

-- 2. Make store_id nullable on sys_user
ALTER TABLE sys_user MODIFY COLUMN store_id BIGINT NULL;

-- 3. Make store_id nullable on sys_role (for platform-level roles)
ALTER TABLE sys_role MODIFY COLUMN store_id BIGINT NULL;

-- 4. Migrate SUPER_ADMIN role to platform level (store_id = NULL)
UPDATE sys_role SET store_id = NULL
WHERE role_code = 'SUPER_ADMIN' AND deleted = 0;

-- 5. Migrate SUPER_ADMIN users to PLATFORM account type (store_id = NULL)
UPDATE sys_user SET account_type = 'PLATFORM', store_id = NULL
WHERE id IN (
    SELECT sur.user_id FROM sys_user_role sur
    JOIN sys_role sr ON sur.role_id = sr.id
    WHERE sr.role_code = 'SUPER_ADMIN' AND sr.deleted = 0
) AND deleted = 0;

-- 6. Add PLATFORM_MANAGE permission
INSERT INTO sys_permission (
    id, permission_code, permission_name, module_code, status, sort_order,
    remark, created_by, created_at, updated_by, updated_at, deleted
) VALUES (
    1030, 'PLATFORM_MANAGE', '平台管理', 'PLATFORM', 'ENABLED', 30,
    'M19 platform management', NULL, NOW(), NULL, NOW(), 0
);

-- 7. Assign PLATFORM_MANAGE to SUPER_ADMIN role (by role_code, not hardcoded id)
INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT sr.id, 1030, NULL, NOW()
FROM sys_role sr
WHERE sr.role_code = 'SUPER_ADMIN' AND sr.deleted = 0
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = sr.id AND rp.permission_id = 1030
);
