# Staff 提交报销 (M12B) Smoke Test 报告

## 环境配置
- **API_MODE**: `real`
- **BASE_URL**: `http://localhost:8080` (后端 dev 模式)
- **请求头模拟**: 
  - `X-User-Id`: 1
  - `X-Store-Id`: 1

## 测试目标
验证小程序端 Staff 发起的「提交报销」请求能否精准抛送至后端 `POST /api/staff/reimbursements`，并在传参层面保持克制，不在前端夹带超出权限的状态或成本计入标识。

## 测试步骤与结果

### 1. 场景 A: 正常提交报销
- **操作**: 
  - 进入“我的”页面 -> 点击“提交报销”进入填报页。
  - 输入用途（`purpose`）：“门店水管维修垫付”。
  - 输入金额（`amount`）：`150.5`。
  - 输入备注（`remark`）：“附言测试”。
  - 点击“提交报销”。
- **结果**: 
  - [x] 后端返回 `200 OK` 并下发 `StaffReimbursementResponse`。
  - [x] 成功后的 `status` 严格锁定在 `PENDING`，且 `costIncluded: false`（一切以服务端为准，前端只取回显，不涉逻辑）。
  - [x] 前端出 Toast 提示“提交成功，请等待确认”，清理表单并退回上一页。

### 2. 异常与边界拦截
- **空值与边界拦截**:
  - `amount <= 0` 时，前端 Toast 阻止提交：“请输入大于0的报销金额”。
  - `purpose` 为空时，前端 Toast 阻止提交：“请输入报销用途”。
- **越权与越轨防守**:
  - 请求 Payload 中 **坚决未传递 `storeId`, `applicantId`, `status`, `costIncluded` 等一切越权信息**，仅提供 `purpose`, `amount`, `remark` 三元素。身份数据悉数交由后端的 `CurrentUserContext` 从 Header 中解析挂载。

### 3. 业务防渗透验证
- [x] **与库存隔离**: 报销纯属账本动作，没有触碰也不影响任何预占和正式扣减逻辑。
- [x] **与订单收银隔离**: 未动用 `receivedAmount` 或支付 / 退款 (`payment_record` / `refund_record`) 表。
- [x] **未开启超纲流转**: 没有列表、没有详情页、没有撤销操作，且无视管理员权限确认动作，严格贴合 M12A 给定的框架能力与边界。

## 结论
**PASS**. M12B Staff 报销提交入口上线完成，轻快而克制地支持了账目发起的请求落地，不产生一丝副作用。
