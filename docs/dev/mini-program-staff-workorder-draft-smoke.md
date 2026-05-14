# 小牛售后小程序 Staff 工单草稿与费用明细联调 Smoke Test 记录 (Phase M4B)

## 1. 联调环境配置
- **后端服务**: Spring Boot 启动于 `dev` profile，数据库为 `xiaoniu_aftermarket_dev`。
- **API_MODE**: `real` (在本地联调时使用)
- **BASE_URL**: `http://localhost:8080`
- **X-User-Id**: `11` (对应 dev seed 用户：赵维修)
- **X-Store-Id**: `1` (对应默认门店)
- **微信开发者工具配置**: 开启“不校验合法域名、web-view（业务域名）”选项。

## 2. 真实草稿与明细 API 验证结果

| 测试步骤 | 请求行为 | 期望与实际表现 | 测试结果 |
|---|---|---|---|
| **创建工单草稿** | `POST /api/staff/work-orders/drafts` | 传入 `customerNameSnapshot`="联调张三" 及必填项。成功返回 ID: 406，状态为 `DRAFT`。金额均为 0。 | **PASS** |
| **添加 PART 费用** | `POST /api/staff/work-orders/{id}/charge-items` | 传入 `chargeType="PART"`, `partId=104`, `quantity=2`, `unitPrice=80`。成功追加。 | **PASS** |
| **添加 LABOR 费用** | `POST /api/staff/work-orders/{id}/charge-items` | 传入 `chargeType="LABOR"`, `quantity=1`, `unitPrice=50`。成功追加。 | **PASS** |
| **添加 OTHER 费用** | `POST /api/staff/work-orders/{id}/charge-items` | 传入 `chargeType="OTHER"`, `quantity=1`, `unitPrice=30`。成功追加。 | **PASS** |
| **明细总计计算** | `GET /api/staff/work-orders/{id}` | 读取详情，`receivableAmount` (应收总额) 由后端自动准确计算为 160+50+30 = **240.00**。 | **PASS** |
| **删除 OTHER 费用** | `DELETE /api/staff/work-orders/{id}/charge-items/{itemId}` | 成功删除该笔 OTHER 费用。 | **PASS** |
| **校验删除后总额** | `GET /api/staff/work-orders/{id}` | 重新拉取后，`receivableAmount` 减至 160+50 = **210.00**。明细数组刷新正常。 | **PASS** |

## 3. 业务边界安全确认

- [x] **是否确认未生成库存流水**：**是**。仅维护 `chargeItems`，不涉及 `inbound`/`outbound` 表。
- [x] **是否确认未触发库存预占**：**是**。工单状态仍为 `DRAFT`，按设计规则，仅在提交（转为 `PENDING_ACCEPT` 等状态）时才会真正扣减 `availableQty` 并增加 `reservedQty`。
- [x] **是否确认无敏感字段展示**：**是**。前后端通讯中完全没有 `referenceCostPrice`, `costPriceSnapshot`, `lineCostAmount` 等财务成本数据。

## 4. 前端交互能力总结
1. **分布式的填写体验**：采用 Step1(基本信息) -> Step2(添加明细) 的串联结构，完全摒弃之前不可用的占位页。
2. **多模态费用弹窗**：支持通过 Action Bar 分别唤起 PART / LABOR / OTHER 的录入模板。其中 PART 附带了从本地查询后端接口的配件选择弹窗（TDesign Popup + Search）。
3. **实时金额刷新**：前端不自行处理总额计算，所有的 Add/Edit/Delete 动作在 `SUCCESS` 后均自动 `GET` 一次最新工单视图，保证前端视图永远与 DB `receivableAmount` 强一致。
4. **强体验校验**：空项目名、0数量、负数价格均在发起 Request 前被拦截于组件层。
