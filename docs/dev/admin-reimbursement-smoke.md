# Admin Web 报销台账 (M12C) Smoke Test 报告

## 环境配置
- **端**: admin-web
- **API_MODE**: `real` (对接后端本地开发环境)
- **请求头模拟**: `Authorization` token 等由现有统一的 request wrapper 自动承载。

## 测试目标
确保 admin-web 端的“报销台账”页面能够正确拉取、展示由员工端发起的报销记录，并安全对接“确认”与“驳回”机制。在严格的交互与 API 边界防守下，本阶段不越权承载多余的财务统计能力。

## 测试步骤与验证结果

### 1. 列表与详情查询验证
- **场景**: 打开“报销台账”菜单。
- **操作**: 
  - 触发 `GET /api/admin/reimbursements` 接口并传入相关搜索条件（如 `applicantId` 或状态）。
  - 点击一条记录的“查看”按钮。
- **结果**: 
  - [x] 分页列表数据成功映射并渲染。展示字段包含：报销编号、报销人 ID、用途、申请金额、确认金额、状态、提交时间等。
  - [x] `status` 筛选、`dateRange`（映射为 `dateFrom` / `dateTo`）条件能正常透传至服务端。
  - [x] 点击“查看”成功触发 `GET /api/admin/reimbursements/{id}`，并在侧边抽屉完整展示金额信息和审核人员/驳回理由。

### 2. 状态扭转流与交互防伪验证
- **前置**: 选取一条 `status` 为 `PENDING` 的报销单。
- **场景 A：确认报销**
  - **操作**: 点击“确认”，输入 `confirmedAmount` 和 `remark`。
  - **结果**:
    - [x] 前端校验：限制金额不得 `<= 0`。
    - [x] 请求成功发送至 `POST /api/admin/reimbursements/{id}/confirm`。
    - [x] 成功后列表刷新，`status` 变为 `CONFIRMED`。且 `costIncluded` 会在后端由 `false` 转为 `true`。
- **场景 B：驳回报销**
  - **操作**: 选取另一条 `PENDING` 的单据，点击“驳回”，填写驳回理由。
  - **结果**:
    - [x] 成功拦截空字符串提交，请求正确触达 `POST /api/admin/reimbursements/{id}/reject`。
    - [x] 成功后列表刷新，`status` 变更为 `REJECTED`。

### 3. 操作隔离与页面边界防卫
- **场景**: 观察已非 `PENDING` 状态的记录。
- **结果**:
  - [x] 若记录为 `CONFIRMED` / `REJECTED` / `CANCELLED`，操作栏中的“确认”和“驳回”按钮自动安全隐匿。禁止发起二次冲销请求。

### 4. 业务跨域保护说明
- [x] 确认本次对接中，**从未干扰任何 `inventory_transaction` 或是 `payment_record` / `refund_record`**。
- [x] 确认**并未开发任何 Excel 导出功能，且完全杜绝了前端侧去计算什么毛利或店长打款流转**。纯粹呈现后端发来的状态事实与决策透传。
- [x] `applicantId`、`confirmedBy` 等 ID 属性如需人名映射，由后续独立规划或依赖后端视图扩展。目前以前后端严格 DTO 对齐为主。

## 结论
**PASS**. M12C 的核心 Admin 对接落地完毕。我们用极简的手法实现了报销单的管控呈现与审核回传闭环，将繁重的状态流转职责如期安全地留存在了后端域内。
