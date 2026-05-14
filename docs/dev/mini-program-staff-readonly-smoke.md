# 小牛售后小程序 Staff 只读接口联调 Smoke Test 记录 (Phase M2.2b)

## 1. 联调环境配置
- **后端服务**: Spring Boot 启动于 `dev` profile (`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`)，数据库为 `xiaoniu_aftermarket_dev`。
- **API_MODE**: `real` (联调测试完毕后已恢复为 `mock`)
- **BASE_URL**: `http://localhost:8080`
- **X-User-Id**: `11` (对应 dev seed 用户：赵维修)
- **X-Store-Id**: `1` (对应默认门店)
- **微信开发者工具配置**: 已开启“不校验合法域名、web-view（业务域名）、TLS版本以及HTTPS证书”选项。

## 2. 验证页面与测试结果

| 页面 | 请求接口 | 期望行为验证 | 测试结果 |
|---|---|---|---|
| **配件查询** | `GET /api/staff/parts` | 有真实数据（共 16 条）；DISABLED（下架）配件不在列表中；没有 `referenceCostPrice` 暴露；前端字段正确解析无 `undefined`。 | **PASS** |
| **配件详情** | `GET /api/staff/parts/100` | 测试 `partId=100`。页面展示正常，分类 `BATTERY` 正确回显；同样没有任何成本敏感字段。 | **PASS** |
| **库存查询** | `GET /api/staff/inventory/stocks` | 真实库存数据加载成功（共 15 条）；`actualQty` / `availableQty` / `reservedQty` 数值与后台逻辑对应；`lastChangedAt` 正常渲染防白屏。 | **PASS** |
| **库存详情** | `GET /api/staff/inventory/stocks/100` | 测试 `partId=100`。真实库存详情无 `undefined` 字段错乱，页面渲染正常，且未提供修改库存等写操作入口。 | **PASS** |
| **工单列表** | `GET /api/staff/work-orders` | 工单真实数据（共 14 条）；客户名及车型（`customerNameSnapshot` / `vehicleModelSnapshot`）正确；金额字段（`receivableAmount` / `receivedAmount`）映射无误。且未出现 `officialAfterSales` 等未规定字段强行显示情况。 | **PASS** |
| **工单详情** | `GET /api/staff/work-orders/405` | 测试 `workOrderId=405`。所有基础信息、明细 `chargeItems` 中 `PART`/`LABOR`/`OTHER` 类目的费用与计算完全渲染成功且正确（例如包含48V电池配件和工时费）。完全隔离所有 `cost` 相关快照。无门店/客户敏感ID展示。 | **PASS** |
| **字典查询** | `GET /api/staff/dict/types/PART_CATEGORY/items` | 返回了 `BATTERY`, `MOTOR`, `CHARGER`, `CONTROLLER`, `TIRE` 分类并能成功使用。无报错。 | **PASS** |

## 3. 联调检查项结论
- **每个接口实际返回是否匹配页面字段**：**是**。在此前 M2.2 阶段修改后的 Adapter 及前端接口完全能够接收并渲染最新的 JSON 结构。
- **是否发现字段差异**：**否**。在启动真实后端后，JSON 结构表现出的所有 Key 都已经在我们 M2.2 阶段被修复，未再发现任何遗漏差异。
- **是否需要后端调整**：**否**。后端 DTO 已完全脱敏，并且结构合理。
- **是否需要小程序 adapter 调整**：**否**。目前已完美匹配。
- **是否确认没有敏感字段展示**：**已确认**。
- **是否确认没有写操作**：**已确认**。没有任何通过 HTTP POST/PUT 的行为和 UI 入口。
