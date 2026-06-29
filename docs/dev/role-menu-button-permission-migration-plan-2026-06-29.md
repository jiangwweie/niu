# 角色-菜单-按钮权限存量迁移方案

## 迁移目标

### 已知客观事实

1. 系统已进入多门店数据隔离阶段，`SUPER_ADMIN` 是平台超管，`STORE_ADMIN` 是门店管理员。
2. `SUPER_ADMIN` 应拥有全部当前权限点。
3. `STORE_ADMIN` 应拥有全部门店内能力，但不能被门店管理员修改。
4. 普通门店角色通过权限点控制菜单入口与按钮操作。
5. 当前重构不以兼容存量脏数据为前置条件，但上线后需要迁移存量角色权限。

### 迁移结论

1. 存量用户不需要直接改账号主体，优先迁移角色与角色权限。
2. 门店管理员角色统一补齐全部门店可授权权限。
3. 平台超管角色统一补齐全部 catalog 权限。
4. 普通角色保守迁移，不自动扩权，只按现有角色语义补缺。
5. 旧式冒号权限如 `work_order:create` 不进入新角色权限页面，保留历史查询兼容，后续单独清理。

## 迁移步骤

### 1. 迁移前盘点

执行只读盘点 SQL，导出现状快照：

```sql
SELECT r.id AS role_id, r.store_id, r.role_code, r.role_name, r.status,
       GROUP_CONCAT(p.permission_code ORDER BY p.permission_code) AS permission_codes
FROM sys_role r
LEFT JOIN sys_role_permission rp ON rp.role_id = r.id
LEFT JOIN sys_permission p ON p.id = rp.permission_id
WHERE r.deleted = 0
GROUP BY r.id, r.store_id, r.role_code, r.role_name, r.status
ORDER BY r.store_id, r.role_code;
```

### 2. 备份角色权限关系

```sql
CREATE TABLE IF NOT EXISTS bak_sys_role_permission_20260629 AS
SELECT * FROM sys_role_permission;
```

### 3. 补齐管理员权限

`V20__add_part_create_permission.sql` 已内置以下迁移逻辑：

1. 创建缺失的 `PART_CREATE` 权限点。
2. `SUPER_ADMIN` 补齐全部启用且非旧式冒号权限。
3. `STORE_ADMIN` 补齐全部启用、非平台、非旧式冒号权限。

### 4. 普通角色迁移建议

| 存量角色 | 建议权限 | 原则 |
|---|---|---|
| 前台/维修 | `WORK_ORDER_VIEW`、`WORK_ORDER_CREATE`、`WORK_ORDER_UPDATE`、`WORK_ORDER_SUBMIT`、`PART_VIEW`、`INVENTORY_VIEW`、`CUSTOMER_VIEW`、`PAYMENT_RECORD` | 覆盖接车、维修、收款基础链路 |
| 库存员 | `PART_VIEW`、`PART_CREATE`、`INVENTORY_VIEW`、`INVENTORY_INBOUND`、可选 `INVENTORY_ADJUST` | 新增配件与入库分离，调整库存单独授权 |
| 收银员 | `WORK_ORDER_VIEW`、`PAYMENT_RECORD`、`REFUND_RECORD` | 收款与退款留痕，不授予工单编辑 |
| 财务 | `FINANCE_VIEW`、`PAYMENT_RECORD`、`REFUND_RECORD`、`OFFICIAL_SETTLEMENT_MANAGE`、`REIMBURSEMENT_CONFIRM`、`EXCEL_EXPORT` | 财务统计、官方结算、报销确认 |
| 只读员工 | `WORK_ORDER_VIEW`、`PART_VIEW`、`INVENTORY_VIEW`、`CUSTOMER_VIEW` | 只看不改 |

### 5. 验收 SQL

```sql
-- STORE_ADMIN 不应缺少任何门店权限
SELECT r.id, r.store_id, r.role_code, p.permission_code
FROM sys_role r
CROSS JOIN sys_permission p
LEFT JOIN sys_role_permission rp ON rp.role_id = r.id AND rp.permission_id = p.id
WHERE r.role_code = 'STORE_ADMIN'
  AND r.status = 'ENABLED'
  AND r.deleted = 0
  AND p.status = 'ENABLED'
  AND p.deleted = 0
  AND p.permission_code <> 'PLATFORM_MANAGE'
  AND p.permission_code NOT LIKE '%:%'
  AND rp.id IS NULL;

-- STORE_ADMIN 不应获得平台权限
SELECT r.id, r.store_id, p.permission_code
FROM sys_role r
JOIN sys_role_permission rp ON rp.role_id = r.id
JOIN sys_permission p ON p.id = rp.permission_id
WHERE r.role_code = 'STORE_ADMIN'
  AND p.permission_code = 'PLATFORM_MANAGE';
```

## 回滚方案

### 1. 回滚角色权限关系

```sql
DELETE FROM sys_role_permission;

INSERT INTO sys_role_permission (id, role_id, permission_id, created_by, created_at)
SELECT id, role_id, permission_id, created_by, created_at
FROM bak_sys_role_permission_20260629;
```

### 2. 回滚注意事项

1. 不建议删除 `PART_CREATE` 权限点，删除会导致新版本菜单与接口鉴权无法表达“仅可新增配件”。
2. 如必须回滚应用版本，应同时回滚到旧前端静态包和旧后端服务。

## 上线后验收

### 角色验收

1. `SUPER_ADMIN`：能进入平台门店管理、员工与权限，能查看全部权限 catalog。
2. `STORE_ADMIN`：能进入本门店全部业务菜单，能管理本门店员工和普通角色权限，不能修改 `STORE_ADMIN`。
3. 普通门店角色：只能看到已授予的菜单，只能点击已授予的按钮。
4. `PART_CREATE` 角色：能看到配件管理并新增配件，不能编辑、停用、删除配件。

### 接口验收

1. `POST /api/admin/parts/official`：`PART_CREATE` 或 `PART_MANAGE` 可访问。
2. `PUT /api/admin/parts/{id}`：仍仅 `PART_MANAGE` 可访问。
3. `PUT /api/admin/roles/{id}/permissions`：仅 `SUPER_ADMIN` 或 `STORE_ADMIN` 写入，普通角色即使有 `ROLE_MANAGE` 也不能写。
4. `STORE_ADMIN` 修改 `STORE_ADMIN` 角色权限应返回业务拒绝。
