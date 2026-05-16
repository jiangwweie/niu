# M16B 试运行 Smoke Report

> 日期：2026-05-16  
> 分支：feature/m15c-admin-auth-token  
> 测试账号：trial_staff（角色：TECHNICIAN_FRONT_DESK）

---

## 一、主业务闭环 — 全部通过 ✅

| # | 步骤 | 结果 |
|---|------|------|
| 1 | trial_staff 登录 | ✅ |
| 2 | 查询库存 | ✅ |
| 3 | 配件入库 | ✅ |
| 4 | 创建 DRAFT 工单 | ✅ |
| 5 | 添加 PART / LABOR / OTHER 费用 | ✅ |
| 6 | 提交工单（库存 RESERVE） | ✅ |
| 7 | 记录支付 | ✅ |
| 8 | 结算工单（库存 CONSUME） | ✅ |
| 9 | 提交报销 | ✅ |
| 10 | admin-web 查询工单 / 库存 | ✅ |
| 11 | 官方结算 | ✅ |
| 12 | 报销确认 | ✅ |
| 13 | 财务报表 | ✅ |
| 14 | Excel 导出 | ✅ |

---

## 二、发现问题记录

### 问题 1：登录接口携带旧 Authorization token

**现象：**  
小程序 `request.ts` 对所有请求统一注入 `Authorization: Bearer {token}`，包括 `POST /api/auth/login/password`。当本地存在过期 token 时，登录请求会携带该 token，后端 Spring Security 过滤链可能因 token 无效提前拦截并返回 401，导致无法重新登录。

**修复：**  
- 在 `RequestOptions` 增加 `skipAuth?: boolean` 选项
- `loginWithPassword` 调用时传入 `skipAuth: true`，登录接口不注入任何 token
- `getMe` / `logout` / 所有业务接口行为不变，仍正常携带 Authorization
- 401 / 403 处理逻辑不变

**修改文件：**
- `mini-program/src/utils/request.ts`：增加 `skipAuth` 判断
- `mini-program/src/api/auth.ts`：`loginWithPassword` 加 `skipAuth: true`

**验证要点：**
- 本地有旧 token → 登录页提交 → 请求头无 Authorization → 登录成功
- 登录后请求 `/api/auth/me` → 有 Authorization → 正常返回用户信息
- 正常业务接口携带 Authorization 不变

---

### 问题 2：trial_staff 调用退款接口返回 403 ⚠️ 等待 Owner 决策

**现象：**  
`trial_staff`（角色：`TECHNICIAN_FRONT_DESK`）调用退款接口（`POST /api/staff/work-orders/{id}/refund`）返回 HTTP 403。

**根因：**  
`trial_staff` 的权限集中**不包含 `REFUND_RECORD`**。后端 `@PreAuthorize("hasAuthority('REFUND_RECORD')")` 正确生效，拒绝访问。

**这不是系统异常，是权限模型正确运行的结果。**

---

### 需要 Owner 决策：退款权限归属

| 方案 | 描述 | 影响 |
|------|------|------|
| **方案 A** | 给 `TECHNICIAN_FRONT_DESK` 增加 `REFUND_RECORD` | 普通员工可在小程序现场退款，操作更便捷但权限更宽松 |
| **方案 B** | 保留现状，退款仅允许店长/财务处理 | 权限收紧，需用具备 `REFUND_RECORD` 的账号验证退款闭环 |

**当前建议（待 Owner 确认）：**

> **采用方案 B，保留现状。**  
> 退款属于涉及资金的敏感操作，建议由门店管理员（`STORE_ADMIN`）或财务（`FINANCE`）角色处理，而非所有前台员工。  
> 若业务场景确实需要员工现场退款，Owner 可在 RBAC 初始化脚本中为 `TECHNICIAN_FRONT_DESK` 补充 `REFUND_RECORD`，后端无需改动。

**小程序 UI 当前行为：**  
工单详情页的"记录退款"按钮已通过 `hasPermission('REFUND_RECORD')` 控制显隐，无权限时按钮不展示，体验层已正确处理。

**本次不改动：**
- 不修改 RBAC 初始化脚本
- 不修改 trial_staff 权限分配
- 不修改后端 `@PreAuthorize`
- 不新增权限点

---

## 三、本次修复文件

| 文件 | 修改内容 |
|------|---------|
| `mini-program/src/utils/request.ts` | 增加 `skipAuth?: boolean` 选项，登录接口跳过 token 注入 |
| `mini-program/src/api/auth.ts` | `loginWithPassword` 加 `skipAuth: true` |
| `docs/dev/m16-trial-smoke-report.md` | 新增本文档 |

**未修改：**
- `utils/config.ts` ❌
- `stores/auth.ts` ❌
- 后端任何代码 ❌
- RBAC 权限模型 ❌
- admin-web ❌
- 任何业务逻辑 ❌

---

## 四、下一步建议

1. **Owner 确认退款权限归属**（方案 A 或方案 B）
2. **本地手工 smoke 验证**：使用过期 token 重新登录，确认无 401 阻断
3. **用 STORE_ADMIN 账号 smoke 退款闭环**，确认方案 B 路径通
4. 如 Owner 决定方案 A，在 RBAC 初始化 SQL 中补充 `TECHNICIAN_FRONT_DESK` → `REFUND_RECORD` 映射，属于 **HIGH** 风险变更，需单独任务卡
