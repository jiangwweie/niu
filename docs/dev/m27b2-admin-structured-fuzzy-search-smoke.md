# M27B-2 管理端结构化模糊筛选增强 - 冒烟测试

## 1. 本轮背景

M27B-1 已完成小程序模糊搜索增强。本轮聚焦管理端（admin-web），保留多条件结构化筛选，但将文本类条件从精确匹配改为模糊匹配，使管理员能通过部分关键词快速定位记录。

管理端不采用"一个 keyword 全包"的设计，而是保留独立字段筛选（客户姓名、手机号、车架号等），与状态、来源、日期等结构化条件共存。这样管理员可灵活组合条件，也能精确控制筛选范围。

## 2. 修改文件

### Backend (7 files)
- `CustomerServiceImpl.java` — phone: eq → like
- `WorkOrderServiceImpl.java` — workOrderNo: eq → like
- `PartServiceImpl.java` — partCode: eq → like
- `InventoryServiceImpl.java` — findPartIdsByFilters partCode: eq → like
- `InventoryController.java` — 新增 keyword 参数，调用 keyword 重载方法
- `AdminReimbursementController.java` — 新增 applicantName 参数
- `ReimbursementQueryRequest.java` — 新增 applicantName 字段
- `ReimbursementServiceImpl.java` — applicantName 子查询支持

### Admin-web (8 files)
- `views/customer/index.vue` — 手机号 placeholder 更新
- `views/vehicle/index.vue` — 车架号/手机号 placeholder 更新
- `views/work-order/index.vue` — 工单号/手机号/车架号 placeholder 更新
- `views/parts/index.vue` — 配件编码/官方品号 placeholder 更新
- `views/inventory/index.vue` — 新增统一搜索框（keyword）
- `views/reimbursement/index.vue` — 报销人：applicantId → applicantName（模糊）
- `api/inventory.ts` — 新增 keyword 透传
- `types/inventory.ts` — 新增 keyword 字段
- `types/reimbursement.ts` — 新增 applicantName 字段

## 3. 客户管理搜索增强

| 字段 | 改造前 | 改造后 |
|------|--------|--------|
| 客户姓名 | like | like（不变） |
| 手机号 | **eq（精确）** | **like（模糊，支持后4位）** |

- 前端改动：手机号 placeholder 更新为"请输入（支持后4位）"

## 4. 车辆档案搜索增强

| 字段 | 改造前 | 改造后 |
|------|--------|--------|
| 车架号 | like | like（不变） |
| 车型 | like | like（不变） |
| 客户手机号 | like（子查询） | like（子查询，不变） |

- 前端改动：车架号 placeholder 更新为"请输入（支持后6位）"；客户手机号 placeholder 更新为"请输入（支持后4位）"

## 5. 工单管理搜索增强

| 字段 | 改造前 | 改造后 |
|------|--------|--------|
| 工单号 | **eq（精确）** | **like（模糊）** |
| 客户姓名 | like | like（不变） |
| 客户手机号 | like | like（不变） |
| 车架号 | like | like（不变） |

- 前端改动：工单号/手机号/车架号 placeholder 更新

## 6. 配件管理搜索增强

| 字段 | 改造前 | 改造后 |
|------|--------|--------|
| 配件编码 | **eq（精确）** | **like（模糊）** |
| 配件名称 | like | like（不变） |
| 官方品号 | eq（精确） | eq（精确，不变） |
| 型号 | like | like（不变） |
| 来源/状态 | eq | eq（不变） |

- 前端改动：配件编码 placeholder 更新为"支持模糊匹配"
- 官方品号保持精确匹配（如需模糊，可使用 keyword 参数）

## 7. 库存管理搜索增强

| 字段 | 改造前 | 改造后 |
|------|--------|--------|
| 统一搜索框 | **不存在** | **新增 keyword（6字段模糊）** |
| 配件编码 | **eq（精确）** | **like（模糊）** |
| 配件名称 | like | like（不变） |
| 来源 | eq | eq（不变） |

- keyword 覆盖：partCode、partName、officialPartNo、defaultBarcode、model、locationRemark
- 前端改动：新增统一搜索框，placeholder "配件编码、名称、官方品号、条码"

## 8. 报销管理搜索增强

| 字段 | 改造前 | 改造后 |
|------|--------|--------|
| 报销编号 | like | like（不变） |
| 报销人 | **applicantId（精确ID）** | **applicantName（模糊姓名）** |
| 报销状态 | eq | eq（不变） |
| 日期范围 | 精确 | 精确（不变） |

- 后端：新增 applicantName 子查询，通过 real_name LIKE 查询 sys_user 表获取 applicant_id 集合
- 前端：报销人搜索改为姓名模糊输入（移除 ID 输入）

## 9. 财务报表处理说明

财务报表页面（/finance）无文本搜索条件，仅有日期/报表类型筛选。本轮不改动。

## 10. 用户管理处理说明

用户管理页面（/user）已有模糊搜索能力：
- 员工账号：已支持输入
- 姓名：已支持输入
- 角色/状态：精确下拉

本轮不改动用户管理页面。

## 11. cashierStatus 结论

工单管理页面已有收银状态下拉（cashierStatus），后端 `WorkOrderQueryRequest` 不包含 `cashierStatus` 字段，前端 `WorkOrderQuery` 中的 `cashierStatus` 目前未传递至后端。本轮不改动收银状态筛选，待后端独立支持后再接入。

## 12. LIKE 安全处理说明

本轮新增的 like 条件均使用 MyBatis-Plus `.like()` 方法，keyword/文本字段已在 Controller/Service 层做 `.trim()` 处理。
LIKE 通配符转义（`%`/`_`）未在本轮做全站治理，将作为后续 M27B-3 独立治理项。

## 13. 未改数据库说明

未修改数据库表结构，未新增 migration。

## 14. 未删除历史数据说明

未删除任何历史数据。

## 15. 未改核心业务规则说明

仅修改查询条件（eq → like），不改库存/工单状态机/支付/结算/财务规则。

## 16. 验证命令

### Backend
```bash
cd backend && JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH mvn test
# 结果：716 tests, 0 failures
```

### Admin-web TypeScript
```bash
cd admin-web && npx vue-tsc --noEmit
# 结果：通过
```

### Admin-web Build
```bash
cd admin-web && npm run build
# 结果：成功
```

### Git
```bash
git diff --check
# 结果：通过
```

## 17. 管理端验证步骤

1. 客户管理 → 输入手机号后4位 → 应能找到客户
2. 车辆档案 → 输入车架号后6位 → 应能找到车辆
3. 车辆档案 → 输入客户手机号后4位 → 应能找到该客户的车辆
4. 工单管理 → 输入工单号部分字符 → 应能模糊匹配
5. 工单管理 → 输入手机号后4位 → 应能找到相关工单
6. 配件管理 → 输入配件编码部分字符 → 应能模糊匹配
7. 库存管理 → 使用统一搜索框输入官方品号 → 应能搜到对应库存
8. 库存管理 → 使用统一搜索框输入条码 → 应能搜到对应库存
9. 库存管理 → 组合使用统一搜索框 + 来源下拉 → 两个条件同时生效
10. 报销管理 → 输入报销人姓名 → 应能按姓名模糊搜索
11. 报销管理 → 导出按钮沿用当前筛选条件
12. 各页面 placeholder 应显示新文案
13. 搜索按钮重置 pageNo=1
14. 翻页保留当前筛选条件

## 18. 后续部署建议

1. 后端部署：`scripts/deploy-prod-release.sh --restart`
2. 管理端部署：admin-web build 后部署静态资源
3. 本轮与 M27B-1 可合并部署
