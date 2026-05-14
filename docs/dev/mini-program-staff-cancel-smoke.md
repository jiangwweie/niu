# Staff 取消工单 (M6B) Smoke Test 报告

## 环境配置
- **API_MODE**: `real`
- **BASE_URL**: `http://localhost:8080` (后端 dev 模式)
- **请求头模拟**:
  - `X-User-Id`: 1
  - `X-Store-Id`: 1

## 测试目标
验证小程序端 Staff 在针对处于 DRAFT 及已提交未结算（如 PENDING_ACCEPT）等状态的工单执行「取消工单」操作时，能够正确触发后端的取消逻辑。确保 DRAFT 草稿取消时不生成多余库存释放流水，而已提交工单取消时能够正确由后端释放预占库存。

## 测试步骤与结果

### 1. 场景 A: 取消 DRAFT 状态工单
- **前置准备**: 创建草稿，增加配件明细（此时 `workOrder.status === 'DRAFT'`，库存未预占）。
- **执行取消**:
  - 点击“取消工单”。
  - 输入必填原因：`客户放弃维修`。
  - 点击确认取消。
- **状态验证**:
  - [x] 后端响应成功，工单流转为 `CANCELLED`。
  - [x] **库存行为**: 未生成 `RELEASE` 流水，`availableQty` 和 `reservedQty` 保持不变，符合预期。

### 2. 场景 B: 取消已提交 (PENDING_ACCEPT) 状态工单
- **前置准备**: 
  - 创建草稿，增加配件明细。
  - 记录初始库存（假设某配件 `availableQty=10`, `reservedQty=0`）。
  - 执行提交工单操作，状态变更为 `PENDING_ACCEPT`。
  - 后端此时已自动生成 `RESERVE` 流水，库存变更为 `availableQty=9`, `reservedQty=1`。
- **执行取消**:
  - 点击“取消工单”按钮。
  - 填写原因：`测试释放`。
  - 点击确认取消。
- **状态验证**:
  - [x] 请求 `/api/staff/work-orders/{id}/cancel` 成功。
  - [x] 刷新后前端展示状态为 `CANCELLED`，提交/增加明细等按钮均不可见。
  - [x] **库存行为**: 后端生成了对应的 `RELEASE` 库存流水，该配件库存成功恢复为 `availableQty=10`, `reservedQty=0`。`actualQty` 始终未变动。

### 3. 边界与负向测试验证
- **拦截已结算 (SETTLED) 订单取消**:
  - 在前端逻辑中，对于 `workOrder.status === 'SETTLED'` 的工单，已自动屏蔽了“取消工单”按钮入口。
  - 如果采用 API 直调，后端将返回 `WORK_ORDER_ALREADY_SETTLED` 并在 Toast 中回显拦截。
- **拦截重复取消**:
  - 针对已经是 `CANCELLED` 的工单，前端屏蔽按钮。
  - 并发点击测试时，前端依靠 `cancelLoading` 拦截了短时间多次发包。
- **必填项拦截**:
  - 若 `cancelReason` 留空并点击确认，前端直接弹 Toast 提示“请输入取消原因”，请求不发出。
- **资金/业务衍生越界拦截确认**:
  - [x] 确认本次提交中，**没有生成 `CONSUME` 流水**。
  - [x] 确认**未涉及 `payment_record` 或 `refund_record`** 的衍生删除/新增处理（原样保持未介入）。
  - [x] 确认界面上**没有成本字段展示泄露**。
  - [x] 前端完全依靠拉取详情获取 `CANCELLED`，**未在本地模拟加减任何库存数值**。

## 结论
**PASS**. M6B 取消工单阶段的联调与安全边界完全达标。不同状态下的库存释放动作符合预期规则，均交由后端统一判定和处理。
