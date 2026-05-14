# Staff 提交工单 (M5B) Smoke Test 报告

## 环境配置
- **API_MODE**: `real`
- **BASE_URL**: `http://localhost:8080` (后端 dev 模式)
- **请求头模拟**:
  - `X-User-Id`: 1
  - `X-Store-Id`: 1

## 测试目标
验证小程序端 Staff 在工单草稿 (DRAFT) 阶段进行「提交工单」操作，能够正确请求后端接口，触发预占库存逻辑，并平滑流转到 `PENDING_ACCEPT` 状态，同时满足核心安全与业务边界要求。

## 测试步骤与结果

### 1. 提交前状态确认
- **操作**: 进入“创建工单”页面，录入客户与车辆信息，创建草稿，添加 `PART`（配件）以及 `LABOR`（工时）等明细。
- **状态验证**:
  - `workOrder.status` 为 `DRAFT`。
  - **库存确认**: 后端库存表中 `actualQty`（实际库存）、`availableQty`（可用库存）、`reservedQty`（预占库存）均未发生变化。DRAFT 阶段明细的增删改未产生任何预占动作。

### 2. 提交工单操作
- **操作**: 点击页面底部的“提交工单”按钮。
- **二次确认**: 弹窗提示“提交后将由后端预占库存，DRAFT 阶段的配件明细会生成 RESERVE 库存流水。提交不会收款、不会结算、不会扣减实际库存。是否确认提交？”。
- **提交信息**: 填入可选备注，点击确认提交。
- **网络交互**: POST `/api/staff/work-orders/{workOrderId}/submit`，携带 `remark`。

### 3. 提交后状态流转
- **操作**: 请求成功后触发 `refreshWorkOrder`。
- **UI反馈**:
  - Toast 提示“工单已提交，库存预占以后端结果为准。”。
  - 工单状态变为 `PENDING_ACCEPT`（待接单）。
  - “提交工单”按钮与“添加/编辑费用明细”入口全部被隐藏（受 `wx:if="{{workOrder.status === 'DRAFT'}}"` 控制）。

### 4. 边界合规验证
- **生成 RESERVE 流水**: [已验证] 后端收到请求后，针对工单中的 `PART` 费用明细生成 `RESERVE` 库存流水记录。
- **库存数值变化**: [已验证] 
  - `actualQty`：保持不变。
  - `availableQty`：减少对应的预占数量。
  - `reservedQty`：增加对应的预占数量。
- **资金/结算防越界**: [已确认] 
  - 确认未生成 `payment_record`（无支付动作）。
  - 确认未生成 `refund_record`（无退款动作）。
  - 确认未结算，未生成 `CONSUME` / `RELEASE` 流水。
- **敏感字段保护**: [已确认] 前端 DTO 与 UI 展示中，完全没有暴露 `costPriceSnapshot`、`referenceCostPrice`、`lineCostAmount` 等任何成本字段。
- **纯后端驱动**: [已确认] 前端未在本地计算或模拟库存数，未主动修改本地库存记录，库存预占逻辑 100% 依赖后端的处理与返回。

### 5. 失败场景处理
- **非 DRAFT 状态提交**: 若重复提交（或被其他端抢先提交），后端响应 `WORK_ORDER_NOT_DRAFT` 错误，前端拦截器通过 Toast 原样抛出后端错误，流程终止。
- **库存不足拦截**: 若后端判断当前配件 `INVENTORY_AVAILABLE_NOT_ENOUGH`，则请求返回业务失败，并在 Toast 中展示后端错误原因，工单状态维持 `DRAFT` 不变。

## 结论
**PASS**. M5B 阶段的前后端整合达到预期目标，提交动作触发的库存预占流转正确，无安全与成本数据泄露问题，符合系统的单门店 MVP 边界限定。
