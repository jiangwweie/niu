# Claude Task 14A: PartController + InventoryController

## 1. 背景

项目：小牛电动官方授权店两轮车维修售后库存管理系统。

当前后端已完成 Service 层和 Admin API Contract v0.1：

- `docs/api/admin-api-v0.1.md`
- Task 14A-1 已由 Codex 建立 Controller 基础设施、dev-only `CurrentUserContext` Header 注入、`DictController` 和样例 MockMvc 测试。

本任务交给 Claude 实现低风险、重复性 Controller：`PartController` 和 `InventoryController`。

## 2. Claude 可修改文件范围

允许新增或修改：

- `backend/src/main/java/com/xiaoniu/aftermarket/part/controller/**`
- `backend/src/main/java/com/xiaoniu/aftermarket/inventory/controller/**`
- `backend/src/test/java/com/xiaoniu/aftermarket/part/controller/**`
- `backend/src/test/java/com/xiaoniu/aftermarket/inventory/controller/**`
- 必要时可新增纯 Controller 层 Request / Response DTO，位置限于上述 controller 包内

允许参考但不应修改：

- `docs/api/admin-api-v0.1.md`
- `backend/src/main/java/com/xiaoniu/aftermarket/part/dto/**`
- `backend/src/main/java/com/xiaoniu/aftermarket/inventory/dto/**`
- `backend/src/main/java/com/xiaoniu/aftermarket/common/context/**`
- `backend/src/main/java/com/xiaoniu/aftermarket/common/api/**`

## 3. Claude 禁止修改文件范围

禁止修改：

- `backend/src/main/java/com/xiaoniu/aftermarket/part/service/**`
- `backend/src/main/java/com/xiaoniu/aftermarket/inventory/service/**`
- `backend/src/main/java/com/xiaoniu/aftermarket/workorder/**`
- `backend/src/main/java/com/xiaoniu/aftermarket/payment/**`
- `backend/src/main/java/com/xiaoniu/aftermarket/official/**`
- `backend/src/main/java/com/xiaoniu/aftermarket/reimbursement/**`
- `backend/src/main/java/com/xiaoniu/aftermarket/finance/**`
- `backend/src/main/resources/db/migration/**`
- `backend/src/main/resources/schema.sql`
- `backend/src/main/resources/data.sql`
- `backend/pom.xml`

禁止实现：

- WorkOrderController
- PaymentController
- RefundController
- OfficialAfterSalesController
- ReimbursementController
- FinanceController
- ExportController
- 正式登录认证
- JWT
- 权限拦截器
- 任何业务规则变更

## 4. 共同实现规则

- 所有接口路径必须以 `/api/admin` 开头。
- 所有响应必须使用 `ApiResponse`。
- 分页响应直接使用当前 `PageResponse`：`records/pageNo/pageSize/total`。
- 不要添加 `success` 字段。
- 不要把 Entity 直接作为 Controller 响应。
- Controller 从 `CurrentUserContext` 获取 `storeId` / `operatorId` 并填入现有 Command / Query DTO。
- dev-only Header 已由 Codex 提供：`X-User-Id` / `X-Store-Id`。
- 前端传入的 `storeId` / `operatorId` 不可信，不作为最终来源。
- 不得修改 Service 层业务逻辑。
- 不得修改 migration。

## 5. PartController 接口清单

基础路径：

```text
/api/admin/parts
```

需要实现：

```text
GET  /api/admin/parts
GET  /api/admin/parts/{partId}
POST /api/admin/parts/official
POST /api/admin/parts/third-party
PUT  /api/admin/parts/{partId}
POST /api/admin/parts/{partId}/enable
POST /api/admin/parts/{partId}/disable
```

字段要求：

- 查询请求贴合 `PartQueryRequest`：`partCode/partName/source/status/pageNo/pageSize`
- Controller 可接受 `enabled` 并映射为 `status=ENABLED/DISABLED`，但不要改 Service DTO
- 创建请求贴合 `CreatePartCommand`，但 `storeId/operatorId` 由 `CurrentUserContext` 填充
- 修改请求贴合 `UpdatePartCommand`，但 `partId/storeId/operatorId` 由路径和 `CurrentUserContext` 填充
- 响应不要返回 Entity；详情可转为 Controller VO，字段参考 `PartQueryResponse`

## 6. InventoryController 接口清单

基础路径：

```text
/api/admin/inventory
```

需要实现：

```text
GET  /api/admin/inventory/stocks
GET  /api/admin/inventory/stocks/{partId}
POST /api/admin/inventory/inbound
POST /api/admin/inventory/adjust
GET  /api/admin/inventory/flows
```

字段要求：

- 库存列表调用 `InventoryService.pageQuery(storeId, partCode, partName, pageNo, pageSize)`
- `source` / `lowStockOnly` 暂不实现筛选，不要伪装支持
- 入库请求贴合 `InventoryInboundCommand`，但 `storeId/operatorId` 由 `CurrentUserContext` 填充
- 调整请求贴合 `InventoryAdjustCommand`，但 `storeId/operatorId` 由 `CurrentUserContext` 填充
- 流水查询请求贴合 `InventoryFlowQueryRequest`
- `startTime/endTime` 当前 DTO 未支持，不要伪装支持
- 响应不要返回 Entity；单配件库存详情应转 Controller VO 或复用查询 Response 形状

## 7. MockMvc 测试要求

至少新增：

- `PartControllerTest`
- `InventoryControllerTest`

测试要求：

- 使用 `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`
- 每个 Controller 至少覆盖一个成功查询接口
- 覆盖创建/入库/调整类接口时必须传 `X-User-Id` / `X-Store-Id`
- 校验响应结构为 `code/message/data/traceId`
- 校验没有 `success` 字段
- 校验 Controller 不直接暴露 Entity 内部字段，如 `deleted`
- 不要求为所有业务失败场景补齐测试，业务失败由 Service 既有测试覆盖

运行：

```bash
cd backend
mvn test
mvn package -DskipTests
```

## 8. Codex Review Checklist

Codex review 时重点检查：

- 是否只改了允许范围内的 Controller / Controller test 文件
- 是否没有修改 Service / Entity / Mapper / migration
- 是否所有接口使用 `ApiResponse`
- 是否没有新增 `success` 响应字段
- 是否没有直接返回 Entity
- 是否 `storeId/operatorId` 来自 `CurrentUserContext`
- 是否没有实现 WorkOrder / Payment / Refund / Official Controller
- 是否没有改变库存 RESERVE / RELEASE / CONSUME 逻辑
- 是否没有改变支付退款金额计算
- 是否没有改变官方结算规则
- `mvn test` 是否通过
- `mvn package -DskipTests` 是否通过
