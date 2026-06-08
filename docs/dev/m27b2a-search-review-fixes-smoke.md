# M27B-2A Search Review Fixes Smoke Test

> 本次修复针对 Code Review 发现的 Critical / Important / Minor 问题。
> 分支：feature/m18-delivery-readiness
> 基线提交：c324b67

---

## 1. Code Review 发现的问题

| 编号 | 严重级别 | 问题 | 状态 |
|------|----------|------|------|
| 1 | Critical | 客户/车辆管理分页损坏：current-change 绑定到 handleSearch 导致 pageNo 重置 | 已修复 |
| 2A | Important | 车辆档案缺少客户姓名筛选 | 已修复 |
| 2B | Important | 工单管理缺少车型筛选 | 已修复 |
| 2C | Important | 配件管理缺少条码筛选 | 已修复 |
| 2D | Important | 用户管理缺少手机号筛选 | 已修复 |
| 3 | Important | 小程序配件搜索缺少 300ms 防抖 | 已修复 |
| 4 | Important | 小程序配件搜索遗漏 locationRemark | 已修复 |
| 5 | Minor | WorkOrderServiceImpl 中 customerName 未 trim（与其他字段不一致） | 已修复 |

---

## 2. 客户/车辆分页修复说明

**问题**：`el-pagination` 的 `@current-change` 和 `@size-change` 都绑定到 `handleSearch`，而 `handleSearch` 会重置 `pageNo = 1`，导致翻页后永远跳回第一页。

**修复**：
- `customer/index.vue` 和 `vehicle/index.vue` 均增加 `handlePageChange()` 和 `handleSizeChange()`。
- `handlePageChange()` 直接调用 `fetchData()`，不重置 pageNo（因为 `v-model:current-page` 已同步更新）。
- `handleSizeChange()` 重置 `pageNo = 1` 后查询（切换单页条数应回到首页）。

**验证**：
- [ ] 客户管理点击第 2 页不跳回第 1 页
- [ ] 车辆档案点击第 2 页不跳回第 1 页
- [ ] 搜索后翻页保留搜索条件
- [ ] 重置后回到第 1 页

---

## 3. 遗漏搜索字段补齐说明

### 3A. 车辆档案 - 客户姓名

- **前端**：`vehicle/index.vue` 增加「客户姓名」筛选框，传参 `customerName`。
- **后端**：`VehiclePageQuery` 增加 `customerName` 字段；`VehicleServiceImpl.pageQuery()` 增加客户表子查询 LIKE 过滤。
- **API 类型**：`VehicleQuery` 增加 `customerName`。
- **验证**：[ ] 车辆档案可通过客户姓名模糊筛选

### 3B. 工单管理 - 车型

- **前端**：`work-order/index.vue` 增加「车型」筛选框，传参 `scooterModel`。
- **后端**：`WorkOrderQueryRequest` 增加 `scooterModel` 字段；`WorkOrderServiceImpl.pageQuery()` 增加 `vehicle_model_snapshot` LIKE 过滤。
- **API 类型**：`WorkOrderQuery` 增加 `scooterModel`；`getWorkOrderList` 映射到 `scooterModel`。
- **验证**：[ ] 工单管理可通过车型模糊筛选

### 3C. 配件管理 - 条码

- **前端**：`parts/index.vue` 增加「条码」筛选框，传参 `barcode`。
- **后端**：`PartQueryRequest` 增加 `barcode` 字段；`PartServiceImpl.buildPartQueryWrapper()` 增加 `default_barcode` LIKE 过滤。
- **API 类型**：`PartViewQuery` 和 `PartListParams` 增加 `barcode`；`getPartsList` 映射到 `barcode`。
- **仓位说明**：仓位属于库存维度，配件管理本身没有仓位字段（仅有 locationRemark 即"库存位置备注"），不做硬加。
- **验证**：[ ] 配件管理可通过条码模糊筛选

### 3D. 用户管理 - 手机号

- **前端**：`user/index.vue` 增加「手机号」筛选框，传参 `phone`。
- **后端**：`AdminUserController.listUsers()` 增加 `phone` 参数；`AdminUserService` 接口和 `AdminUserServiceImpl` 增加 `phone` LIKE 过滤。
- **API 类型**：`UserQuery` 增加 `phone`。
- **验证**：[ ] 用户管理可通过手机号模糊筛选

---

## 4. 小程序配件搜索防抖修复说明

**问题**：`onSearchPart` 每次输入字符立即触发 `filter`，在大列表下可能卡顿。

**修复**：
- `create-work-order-placeholder/index.ts` 和 `inbound-placeholder/index.ts` 均增加 `partSearchTimer` 变量。
- `onSearchPart` 使用 `clearTimeout + setTimeout 300ms` 防抖模式，与已有的客户/车辆搜索防抖保持一致。
- 不引入额外依赖。

**验证**：
- [ ] 输入配件关键词时不会每个字符立即触发 filter
- [ ] 停止输入约 300ms 后刷新搜索结果
- [ ] 清空搜索恢复列表

---

## 5. locationRemark 搜索补齐说明

**修复**：
- `mini-program/src/types/parts.ts` 的 `Part` 接口增加 `locationRemark?: string`。
- `create-work-order-placeholder/index.ts` 和 `inbound-placeholder/index.ts` 的 `onSearchPart` filter 链增加 `(p.locationRemark || '').toLowerCase().includes(keyword)`。
- 安全兼容空值（使用 `|| ''`）。
- 不改变配件选择结果结构。

**后端说明**：`StaffPartListItem`（小程序端配件列表响应 DTO）目前不返回 `defaultBarcode` 和 `locationRemark`——这是 `StaffControllerTest.staffPartListDoesNotExposeSensitiveFields` 明确断言的现有设计决策。本次不做改变。当后端未来开放这两个字段时，前端搜索代码已就绪可直接生效。

**验证**：
- [ ] 当 locationRemark 有值时可搜索到（需后端先开放字段）
- [ ] locationRemark 为空时搜索不报错

---

## 6. trim 一致性修复说明

**文件**：`WorkOrderServiceImpl.java`，`pageQuery()` 方法。

**修复**：`customerName` 查询条件增加 `.trim()`，与 `workOrderNo`、`customerPhone`、`vehicleFrameNo` 保持一致。

**不做**：不做全站 LIKE escape（M27B-3）。

---

## 7. cashierStatus 未处理原因

本次不处理 `cashierStatus` 后端支持。前端已有收银状态筛选 UI 和前端参数传递，后端 `pageQuery()` 中是否支持 cashierStatus 取决于状态机设计，不在本次 review fix 范围内。

---

## 8. 未做 M27B-3 LIKE 全站治理说明

LIKE 通配符（`%` 和 `_`）转义是独立任务，需逐表逐字段评估，风险较高，暂停不处理。

---

## 9. 未部署、未 push、未改数据库说明

- 未执行 `scripts/deploy-prod-release.sh`。
- 未 `git push`。
- 未改数据库 migration / schema。
- 未删除历史数据。
- 未改核心业务规则。

---

## 10. 验证命令

```bash
# Backend
cd backend && JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH mvn test

# Admin-web
cd admin-web && npx vue-tsc --noEmit
cd admin-web && npm run build

# Mini-program
cd mini-program && npx tsc --noEmit

# 通用
git diff --check
git status --short
```

---

## 11. 验证结果

| 验证项 | 结果 |
|--------|------|
| backend tests | 716 tests passed |
| admin-web vue-tsc | 通过 |
| admin-web build | 通过 |
| mini-program tsc | 历史 tdesign-miniprogram 类型问题，本次修改文件无新增错误 |
| git diff --check | 通过 |
| git status | 仅本次修改文件，无意外变更 |
