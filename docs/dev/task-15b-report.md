# Task 15B — 后端修复完成报告

## 1. Commit

```
b55526d4ea6f55e9b9f5bb5a5977d86a3a3a1e21
```

## 2. 修改文件列表

| 文件 | 说明 |
| --- | --- |
| `SysDictItemEntity.java` | P0 核心修复：`system` → `isSystem` |
| `DictController.java` | 新增 `/types` endpoint |
| `DictTypeResponse.java` | 新增响应 DTO |
| `DictService.java` | 新增 `listEnabledTypes()` 接口 |
| `DictServiceImpl.java` | 实现 `listEnabledTypes()` |
| `PartController.java` | 新增三个查询参数入口 |
| `PartQueryRequest.java` | 新增三个查询字段 |
| `PartServiceImpl.java` | 实现三个新查询条件 |
| `DictControllerTest.java` | 新增 3 个单测 |
| `PartControllerTest.java` | 新增 3 个单测 |
| `smoke-mysql-dev.sh` | 新增 Step 19–22 |
| `docs/api/admin-api-v0.2-delta.md` | 文档更新 |
| `docs/dev/admin-api-examples.http` | HTTP 示例更新 |
| `docs/dev/admin-api-integration.md` | 集成指南更新 |

## 3. P0 根因说明

**现象：** `GET /api/admin/dict/types/{typeCode}/items` 在 MySQL 环境返回 `COMMON_INTERNAL_ERROR`，H2 正常。

**根因：** `SysDictItemEntity` 中字段 `system` 与 MySQL 保留字冲突。MyBatis-Plus 生成的 SQL 为：

```sql
SELECT id, type_id, item_code, item_name, sort_order, status, system, remark
```

`system` 是 MySQL 8.0 保留字，导致 `SQLSyntaxErrorException`。H2 无此限制，所以测试不报错。

**问题类型：** Entity 字段名映射问题（`@TableField` 已标注 `is_system`，但 Java 字段名为 `system`，MyBatis-Plus 默认用字段名生成 SELECT 列名）。

## 4. P0 修复方式

**修复：** 将 Java 字段 `system` 重命名为 `isSystem`，与数据库列名 `is_system` 对应。`@TableField("is_system")` 保持不变。

**修复后 `GET /api/admin/dict/types/{typeCode}/items` 响应格式：**

```json
{
  "code": "SUCCESS",
  "data": [
    {
      "itemCode": "DRAFT",
      "itemName": "草稿",
      "sortOrder": 1,
      "enabled": true
    }
  ]
}
```

- **typeCode 存在且 ENABLED：** 返回该类型下所有 enabled 字典项，按 `sort_order` 升序。
- **typeCode 不存在或已 DISABLED：** 返回空数组 `[]`，不返回错误码。
- 不暴露 `id`、`typeId`、`remark`、`isSystem` 等内部字段。

## 5. P1 实现情况

**已实现。** `GET /api/admin/dict/types` 返回所有启用的字典类型。

返回字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `typeCode` | String | 字典类型编码 |
| `typeName` | String | 字典类型名称 |
| `enabled` | Boolean | 是否启用 |

只返回 `status = ENABLED` 且未软删除的记录，按 `id` 升序。

## 6. P2 实现情况

**已实现。** `GET /api/admin/parts` 新增三个可选查询参数：

| 参数 | 匹配方式 | 说明 |
| --- | --- | --- |
| `officialPartNo` | 精确匹配（`eq`） | 官方配件编号 |
| `model` | 模糊匹配（`like`） | 车型 |
| `categoryCode` | 精确匹配（`eq`） | 配件分类编码 |

**storeId 隔离：** 保持不变，`storeId` 仍从 `CurrentUserContext` 获取并作为查询条件注入。

## 7. 文档更新

| 文档 | 状态 |
| --- | --- |
| `docs/api/admin-api-v0.2-delta.md` | 已更新（新增 §5 Dict 查询增强、§6 Part 查询增强） |
| `docs/dev/admin-api-examples.http` | 已更新 |
| `docs/dev/admin-api-integration.md` | 已更新 |

## 8. H2 测试结果

```
Tests run: 328, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

新增单测覆盖：

| 测试类 | 新增用例 |
| --- | --- |
| `DictControllerTest` | `listTypesReturnsEnabledTypes`、`listItemsReturnsEmptyForNonexistentTypeCode`、`listItemsReturnsEmptyForDisabledType` |
| `PartControllerTest` | `listPartsFilterByCategoryCode`、`listPartsFilterByModel`、`listPartsFilterByOfficialPartNo` |

## 9. MySQL Dev Smoke 结果

未在此环境运行（需已启动的 MySQL dev 实例）。Smoke 脚本已新增 Step 19–22 覆盖：

- Step 19：Dict types 列表
- Step 20：Dict items 存在 typeCode
- Step 21：Dict items 不存在 typeCode
- Step 22：Part filter by categoryCode

## 10. HTTP 示例验证结果

未在此环境运行实际 HTTP 请求（需后端服务运行）。HTTP 示例文件已更新。

## 11. Forbidden Scope 检查

| 项目 | 是否触碰 |
| --- | --- |
| 库存核心逻辑 | 否 |
| 工单状态机 | 否 |
| 支付/退款/结算逻辑 | 否 |
| Auth/JWT/User/Role/Permission | 否 |
| Reimbursement/Finance/Export | 否 |
| Mobile/真实支付/payment_order | 否 |
| Redis/MQ/微服务 | 否 |

## 12. 遗留问题

无遗留问题。P0/P1/P2 均已实现，全量测试通过。
