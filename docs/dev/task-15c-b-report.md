# Task 15C-B — Inventory API 契约补充与边界确认报告

## 1. Commit

```
(见下方 git log)
```

## 2. 修改文件列表

| 文件 | 说明 |
| --- | --- |
| `docs/api/admin-api-v0.2-delta.md` | 新增 §7 Inventory API 契约（响应字段、查询参数、unitCost 语义、adjust 边界） |
| `docs/dev/admin-api-examples.http` | 新增 flows partId/flowType 组合查询、adjust 跨零/零值错误示例 |
| `docs/dev/admin-api-integration.md` | §6.3 补充 flows 查询参数表、响应字段表、unitCost 语义、adjust 边界说明 |
| `docs/dev/task-15c-b-report.md` | 本报告 |

**不修改任何 Java 代码。**

## 3. 是否新增/修改 Response 字段

**否。** 无需新增或修改任何字段。

发现：前端反馈 "beforeQty / afterQty / businessType 缺失" 与代码不符。`InventoryFlowQueryResponse` 已包含 `actualBefore`、`actualAfter`、`availableBefore`、`availableAfter`、`reservedBefore`、`reservedAfter`、`businessType` 字段，且 `toFlowQueryResponse()` 已正确映射。

前端可能因未看到这些字段的文档说明而误以为缺失，建议重新检查 `GET /api/admin/inventory/flows` 响应 JSON。

## 4. InventoryFlowQueryResponse 最终字段清单

实际代码字段（`InventoryFlowQueryResponse.java` + `toFlowQueryResponse()`）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 流水 ID |
| storeId | Long | 门店 ID |
| partId | Long | 配件 ID |
| partCode | String | 配件编码（从 Part 表关联） |
| partName | String | 配件名称（从 Part 表关联） |
| flowType | String | INBOUND / RESERVE / RELEASE / CONSUME / ADJUST |
| quantityDelta | Integer | 数量变化 |
| actualBefore | Integer | 变化前实际库存 |
| actualAfter | Integer | 变化后实际库存 |
| availableBefore | Integer | 变化前可用库存 |
| availableAfter | Integer | 变化后可用库存 |
| reservedBefore | Integer | 变化前预占库存 |
| reservedAfter | Integer | 变化后预占库存 |
| businessType | String | MANUAL_INBOUND / MANUAL_ADJUST |
| businessId | Long | 业务 ID（当前 null） |
| operatorId | Long | 操作人 ID |
| operatedAt | LocalDateTime | 操作时间 |
| reason | String | 原因 |
| remark | String | 备注 |
| unitCost | BigDecimal | 入库单价（仅 INBOUND） |

## 5. beforeQty / afterQty 是否返回

**是，已返回。** 数据库 `inventory_flow` 表有 `actual_before`/`actual_after`/`available_before`/`available_after`/`reserved_before`/`reserved_after` 六个字段，Entity 和 Response DTO 均已包含，`toFlowQueryResponse()` 已映射。

前端无需移除对应列。

## 6. businessSource / flowNo / operatorName 是否返回

| 字段 | 状态 | 说明 |
| --- | --- | --- |
| businessType | 已返回 | MANUAL_INBOUND / MANUAL_ADJUST |
| businessId | 已返回 | 当前 null，工单场景后续填充 |
| flowNo | 不存在 | 数据库无此字段，不新增 |
| operatorName | 不存在 | 无用户系统，不做联表查询 |

## 7. unitCost 最终语义

入库单价，业务规则：

1. 可选字段，允许为 null。
2. 持久化到 `inventory_flow.unit_cost`。
3. 入库时若提供，同步更新 `part.reference_cost_price`（直接覆盖，非加权平均）。
4. 调整（ADJUST）不写入 `unitCost`。
5. 不引入 FIFO / 加权平均库存成本算法。
6. 值不能为负数，否则返回 `INBOUND_UNIT_COST_NEGATIVE`。

## 8. quantityDelta 跨零行为和错误码

| 场景 | 行为 | 错误码 |
| --- | --- | --- |
| quantityDelta = 0 | 拒绝 | `INVENTORY_ADJUST_ZERO` |
| 调整后 availableQty < 0 | 拒绝 | `INVENTORY_ADJUST_WOULD_NEGATIVE` |
| 调整后 actualQty < 0 | 拒绝 | `INVENTORY_ADJUST_ACTUAL_NEGATIVE` |
| 无库存记录 | 拒绝 | `PART_STOCK_NOT_FOUND` |
| reason 为空 | 拒绝 | `INVENTORY_ADJUST_REASON_REQUIRED` |

检查顺序：先检 `availableAfter < 0`，再检 `actualAfter < 0`。

## 9. H2 测试结果

```
Tests run: 328, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

代码未修改，测试无回归。

## 10. MySQL smoke 或手工验证结果

待手工验证。smoke 脚本已覆盖 inbound → RESERVE → CONSUME → RELEASE 完整链路（Step 2–18）。

## 11. 是否触碰 forbidden scope

**否。** 本次仅更新文档，不修改任何 Java 代码或数据库 schema。

- 不改库存三数量核心规则
- 不改 INBOUND / RESERVE / RELEASE / CONSUME 规则
- 不改工单状态机
- 不改支付 / 退款 / 结算逻辑
- 不新增 Auth / User / Role / Permission
- 不新增 Reimbursement / Finance / Export
- 不接真实支付网关
- 不新增 payment_order
- 不引入 Redis / MQ / 微服务

## 12. 遗留问题

无代码遗留问题。

**前端建议：** 请重新检查 `GET /api/admin/inventory/flows` 响应 JSON，确认 `actualBefore`/`actualAfter`/`availableBefore`/`availableAfter`/`reservedBefore`/`reservedAfter`/`businessType` 字段是否存在。如确认缺失，请提供具体响应示例以便定位。
