# 小牛售后小程序 Staff 配件入库联调 Smoke Test 记录 (Phase M3)

## 1. 联调环境配置
- **后端服务**: Spring Boot 启动于 `dev` profile (`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`)，数据库为 `xiaoniu_aftermarket_dev`。
- **API_MODE**: `real` (完成测试后可在配置中随意切换回 `mock`)
- **BASE_URL**: `http://localhost:8080`
- **X-User-Id**: `11` (对应 dev seed 用户：赵维修)
- **X-Store-Id**: `1` (对应默认门店)
- **微信开发者工具配置**: 已开启“不校验合法域名、web-view（业务域名）、TLS版本以及HTTPS证书”选项。

## 2. 真实入库验证结果

| 验证项 | 测试行为 / 数据 | 测试结果 | 备注说明 |
|---|---|---|---|
| **测试入库 API** | `POST /api/staff/inventory/inbound` | **PASS** | |
| **测试用 partId** | `100` (48V20Ah电池) | **PASS** | 请求 JSON 中只传入了 partId 和 quantity，以及可选的 remark 等。 |
| **入库数量** | 增加 `10` 件 | **PASS** | |
| **请求结果响应** | `code: "SUCCESS"`, 返回了完整的更新后库存视图 | **PASS** | |
| **库存前后变化** | `actualQty` / `availableQty` 从 **45** 增加到了 **55**。`reservedQty` 为 0 不变。 | **PASS** | |
| **flowId 返回** | 成功返回真实流水号：`314` | **PASS** | |
| **无敏感字段返回** | 响应中无任何 `unitCost`, `referenceCostPrice` 或工单成本快照。 | **PASS** | 已全局核对，完全安全。 |

## 3. 小程序端页面能力检查
1. **页面位置**：已将 `src/pages/inbound-placeholder/*` 原地改造为“配件入库”页面。
2. **配件选择交互**：成功实现了从 `GET /api/staff/parts` 拉取可用配件列表，并支持无感知的前端 `Search` 搜索。选择后可折叠为选中卡片视图。
3. **入库校验能力**：成功实现了防无数量入库、防非法金额提交，及提交中的 Button Loading 防止重复发起。
4. **扫码模拟能力**：设计了模拟的扫码入口（使用 `wx.showModal` 拦截并给出明确业务提示），符合本阶段无实体硬件下的交互准则。
5. **入库成功结果页**：按照需求，在请求成功后原地更新了状态，高亮显示了“可用库存”，并展示了流水单号和后端返回时间。提供了“继续入库”的复位按钮。

## 4. 后续注意事项
- 本阶段成功引入了 **首个写操作**。
- 但仍然 **没有开放** 工单的提交、退款、核销与报销入口。
- 小程序仍维持无 JWT 及真实登录拦截的物理隔离形态，一切仍依赖 `M1` 时期的简单模拟账号拦截体系。
