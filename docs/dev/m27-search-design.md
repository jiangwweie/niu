# M27 搜索方案与字段矩阵设计

## 1. 背景

客户反馈"搜索条件不好用，希望尽可能支持模糊搜索"。
M27A 阶段已完成搜索能力全量审计（详见 `docs/dev/m27a-search-capability-audit.md`）。
本阶段在审计结论基础上，按 Owner 最新决策细化搜索方案和字段矩阵。
本阶段不改代码、不提交、不部署、不 push。

设计日期：2026-06-08

---

## 2. Owner 最新决策

1. 小程序现场使用场景优先简单，一个搜索框为主；
2. 管理端不适合一个输入框全包，管理端应保留多条件筛选；
3. 管理端的文本条件应支持模糊搜索；
4. 状态、来源、日期、金额区间等结构化条件仍保持精确筛选；
5. 客户数据量不大，第一版不需要过度考虑性能问题；
6. 但仍不允许破坏分页、状态筛选、导出范围和权限隔离。

---

## 3. 小程序搜索设计原则

| 原则 | 说明 |
|------|------|
| 一个 keyword | 不要求员工选择"按姓名/按手机号/按车架号"，一个输入框匹配多个高频字段 |
| 后端匹配 | keyword 由后端匹配多个字段，不在前端过滤当前页数据 |
| 模糊优先 | 所有文本字段使用 LIKE %keyword%，支持部分输入（手机号后4位、车架号后6位等） |
| 状态共存 | keyword 与状态筛选同时生效，互不覆盖 |
| 分页共存 | keyword 不破坏分页，搜索重置 pageNo=1，翻页保留 keyword 和 status |
| 空 keyword | 空 keyword 不传查询条件，返回默认列表 |
| 触发方式 | 列表页：提交触发（Enter/搜索按钮）；弹窗内搜索：300ms 防抖实时触发 |
| 空结果 | 搜索无结果显示友好空状态，与搜索失败区分 |

---

## 4. 管理端搜索设计原则

| 原则 | 说明 |
|------|------|
| 多条件筛选 | 保留多个查询条件输入框，不改成一个 keyword 全包 |
| 文本模糊 | 每个文本条件支持 LIKE %value%，支持部分输入 |
| 结构化精确 | 状态、来源、日期范围、金额范围保持精确/区间筛选 |
| 分页联动 | 搜索按钮重置 pageNo=1；翻页保留当前搜索条件 |
| 导出联动 | 导出按当前筛选条件导出 |
| 重置按钮 | 清空所有条件并刷新 |
| 权限隔离 | 所有查询必须保持 store_id 隔离 |

---

## 5. 小程序搜索字段矩阵

基于实体字段实际存在情况设计（标注"当前不存在"的字段表示数据库中无此列，需要后续评估是否新增）。

### 5.1 客户搜索（创建工单页）

| keyword 匹配字段 | 数据库字段 | 当前后端是否支持 | 改造说明 |
|-----------------|-----------|----------------|---------|
| 客户姓名 | customer.customer_name | 支持（LIKE %keyword%） | 已满足 |
| 手机号 | customer.phone | 支持（LIKE %keyword%） | 已满足 |
| 手机号后4位 | customer.phone | 支持（LIKE %keyword% 天然匹配后缀） | 已满足 |
| 备注/别名 | customer.remark | **不支持** | 需后端 keyword 查询增加 remark 字段 |

**提示文案建议**：`输入姓名、手机号或备注搜索`

**接口**：`GET /api/staff/customers/search?keyword=xxx`

**当前差距**：后端 keyword 查询仅匹配 customerName + phone，未包含 remark。

---

### 5.2 车辆搜索（创建工单页）

| keyword 匹配字段 | 数据库字段 | 当前后端是否支持 | 改造说明 |
|-----------------|-----------|----------------|---------|
| 车架号 | vehicle.frame_no | 支持（LIKE %keyword%） | 已满足 |
| 车架号后6位 | vehicle.frame_no | 支持（LIKE %keyword% 天然匹配后缀） | 已满足 |
| 车型 | vehicle.model | 支持（LIKE %keyword%） | 已满足 |
| 电池号 | vehicle.battery_no | 支持（LIKE %keyword%） | 已满足 |
| 客户姓名 | customer.customer_name | **不支持** | 需增加 customer 表 JOIN 查询 |
| 客户手机号 | customer.phone | 支持（通过二次查询合并） | 已满足（staff controller 二次查询机制） |
| 客户手机号后4位 | customer.phone | 支持（LIKE %keyword% 天然匹配后缀） | 已满足 |
| 车牌号 | 不存在 | — | vehicle 表无 licensePlate 字段 |

**提示文案建议**：`输入车架号、车型或客户手机号搜索`

**接口**：`GET /api/staff/vehicles/search?keyword=xxx&customerId=xxx`

**当前差距**：
- 后端未匹配客户姓名（staff controller 二次查询只查 customer.phone，不查 customer.customerName）
- 无车牌号字段（vehicle 表无此列，暂不纳入）

---

### 5.3 配件搜索（创建工单页/入库页弹窗）

**注意**：当前为前端本地 includes() 筛改，不走后端接口。需改为后端 keyword 搜索或扩展本地过滤字段。

| keyword 匹配字段 | 数据库字段 | 当前本地过滤是否支持 | 改造说明 |
|-----------------|-----------|-------------------|---------|
| 配件名称 | part.part_name | 支持（includes） | 已满足 |
| 配件编码 | part.part_code | 支持（includes） | 已满足 |
| 官方品号 | part.official_part_no | **不支持** | 需增加到本地过滤条件 |
| 默认条码 | part.default_barcode | **不支持** | 需增加到本地过滤条件 |
| 型号 | part.model | **不支持** | 需增加到本地过滤条件 |
| 规格 | 不存在 | — | part 表无 specification 字段 |
| 仓位 | part.location_remark | **不支持** | 需增加到本地过滤条件 |

**提示文案建议**：`搜索配件编码、名称、官方品号`

**改造方案**：
- **方案A（推荐）**：扩展前端本地过滤，增加 officialPartNo、defaultBarcode、model、locationRemark 字段匹配，同时添加 300ms 防抖
- **方案B**：改为后端 keyword 搜索接口，但会增加网络请求，需评估体验

**改造内容（方案A）**：
```
当前：p.partName.toLowerCase().includes(kw) || p.partCode.toLowerCase().includes(kw)
改为：p.partName.toLowerCase().includes(kw)
   || p.partCode.toLowerCase().includes(kw)
   || (p.officialPartNo || '').toLowerCase().includes(kw)
   || (p.defaultBarcode || '').toLowerCase().includes(kw)
   || (p.model || '').toLowerCase().includes(kw)
   || (p.locationRemark || '').toLowerCase().includes(kw)
```

---

### 5.4 工单列表搜索

| keyword 匹配字段 | 数据库字段 | 当前后端是否支持 | 改造说明 |
|-----------------|-----------|----------------|---------|
| 工单号 | work_order.work_order_no | 支持（LIKE %keyword%） | 已满足 |
| 客户姓名 | work_order.customer_name_snapshot | 支持（LIKE %keyword%） | 已满足 |
| 客户手机号 | work_order.customer_phone_snapshot | 支持（LIKE %keyword%） | 已满足 |
| 客户手机号后4位 | work_order.customer_phone_snapshot | 支持（LIKE %keyword% 天然匹配后缀） | 已满足 |
| 车架号 | work_order.frame_no_snapshot | 支持（LIKE %keyword%） | 已满足 |
| 车架号后6位 | work_order.frame_no_snapshot | 支持（LIKE %keyword% 天然匹配后缀） | 已满足 |
| 车型 | work_order.vehicle_model_snapshot | 支持（LIKE %keyword%） | 已满足 |
| 电池号 | work_order.battery_no_snapshot | **不支持** | 需后端增加 battery_no_snapshot 到 keyword OR 条件 |
| 车牌号 | 不存在 | — | work_order 表无 licensePlateSnapshot |

**提示文案建议**：`搜索工单号、客户名、手机号、车架号`

**同时保留**：
- 状态筛选（keyword + status 同时生效）
- 分页（无限滚动 loadMore）
- 下拉刷新

**当前差距**：后端 keyword 已覆盖 5 个字段 + official_after_sales 子查询，基本满足。
可选增强：增加 battery_no_snapshot 到 keyword 匹配。

---

### 5.5 库存查询搜索

| keyword 匹配字段 | 数据库字段 | 当前后端是否支持 | 改造说明 |
|-----------------|-----------|----------------|---------|
| 配件名称 | part.part_name | 支持（LIKE %keyword%） | 已满足 |
| 配件编码 | part.part_code | **不支持（精确匹配）** | 需改为 LIKE %keyword% |
| 官方品号 | part.official_part_no | **不支持** | 需增加到查询条件 |
| 默认条码 | part.default_barcode | **不支持** | 需增加到查询条件 |
| 型号 | part.model | **不支持** | 需增加到查询条件 |
| 仓位 | part.location_remark | **不支持** | 需增加到查询条件 |

**提示文案建议**：`搜索配件编码、名称、官方品号`

**当前差距**：后端 inventory 查询先查 part 表（partCode 精确 + partName LIKE），再查库存。
需改为：partCode LIKE + 增加 officialPartNo、defaultBarcode、model、locationRemark 到 OR 条件。

---

### 5.6 配件列表搜索

| keyword 匹配字段 | 数据库字段 | 当前后端是否支持 | 改造说明 |
|-----------------|-----------|----------------|---------|
| 配件名称 | part.part_name | 支持（LIKE %keyword%） | 已满足 |
| 配件编码 | part.part_code | **不支持（精确匹配）** | 需改为 LIKE %keyword% |
| 官方品号 | part.official_part_no | **不支持（精确匹配）** | 需改为 LIKE %keyword% |
| 默认条码 | part.default_barcode | **不支持** | 需增加到查询条件 |
| 型号 | part.model | 支持（LIKE %keyword%） | 已满足 |
| 仓位 | part.location_remark | **不支持** | 需增加到查询条件 |

**提示文案建议**：`搜索配件编码、名称、官方品号`

**当前差距**：后端使用独立字段过滤（partCode eq + partName like），无统一 keyword 查询。
需改为统一 keyword 查询，覆盖 partCode、partName、officialPartNo、defaultBarcode、model、locationRemark。

---

## 6. 管理端搜索字段矩阵

### 6.1 客户管理

| 筛选条件 | 前端字段 | 后端匹配方式 | 目标匹配方式 | 改造说明 |
|---------|---------|------------|------------|---------|
| 客户姓名 | customerName | LIKE %value% | LIKE %value% | 已满足 |
| 手机号 | phone | 精确匹配(eq) | **LIKE %value%** | 需改为模糊匹配 |
| 创建时间 | createdAt | — | 日期范围 | 当前无此筛选，可选增加 |

**提示文案建议**：
- 客户姓名：`请输入客户姓名`（当前为 `请输入`，过于模糊）
- 手机号：`请输入手机号（支持后4位）`

**修复项**：
- 分页 BUG：`@current-change` 应绑定 `fetchData` 而非 `handleSearch`（当前翻页永远回到第1页）

---

### 6.2 车辆档案

| 筛选条件 | 前端字段 | 后端匹配方式 | 目标匹配方式 | 改造说明 |
|---------|---------|------------|------------|---------|
| 车架号 | vin | LIKE %value% | LIKE %value% | 已满足 |
| 车牌号 | 不存在 | — | — | vehicle 表无此字段 |
| 车型 | model | LIKE %value% | LIKE %value% | 已满足 |
| 客户姓名 | 无前端字段 | — | **LIKE %value%** | 需增加前端输入框和后端查询 |
| 客户手机号 | customerPhone | LIKE %value% | LIKE %value% | 已满足 |

**提示文案建议**：
- 车架号：`请输入车架号（支持后6位）`
- 车型：`请输入车型`
- 客户手机号：`请输入手机号（支持后4位）`

**修复项**：
- 分页 BUG：同客户管理

---

### 6.3 工单管理

| 筛选条件 | 前端字段 | 后端匹配方式 | 目标匹配方式 | 改造说明 |
|---------|---------|------------|------------|---------|
| 工单号 | workOrderNo | 精确匹配(eq) | **LIKE %value%** | 需改为模糊匹配 |
| 客户姓名 | customerName | LIKE %value% | LIKE %value% | 已满足 |
| 客户手机号 | customerPhone | LIKE %value% | LIKE %value% | 已满足 |
| 车架号 | vehicleFrameNo | LIKE %value% | LIKE %value% | 已满足 |
| 车型 | 无前端字段 | — | **LIKE %value%** | 需增加前端输入框和后端查询 |
| 工单状态 | status | 精确 | 精确 | 已满足，保持精确 |
| 收款状态 | cashierStatus | **未传递** | 精确 | 需修复 API 映射 + 后端支持 |
| 是否官方 | officialOnly | 精确 | 精确 | 已满足 |
| 创建时间 | startTime/endTime | 日期范围 | 日期范围 | 已满足 |

**提示文案建议**：
- 工单号：`请输入工单号`（当前已正确）
- 客户姓名：`请输入客户姓名`（当前已正确）
- 客户手机号：`请输入手机号`（当前已正确）
- 车架号：`请输入车架号`

**修复项**：
- cashierStatus 筛选器 API 映射缺失
- 工单号改为模糊匹配

---

### 6.4 配件管理

| 筛选条件 | 前端字段 | 后端匹配方式 | 目标匹配方式 | 改造说明 |
|---------|---------|------------|------------|---------|
| 配件名称 | partName | LIKE %value% | LIKE %value% | 已满足 |
| 配件编码 | partCode | 精确匹配(eq) | **LIKE %value%** | 需改为模糊匹配 |
| 官方品号 | officialPartNo | 精确匹配(eq) | **LIKE %value%** | 需改为模糊匹配 |
| 默认条码 | 无前端字段 | — | **LIKE %value%** | 需增加前端输入框和后端查询 |
| 型号 | model | LIKE %value% | LIKE %value% | 已满足 |
| 仓位 | 无前端字段 | — | **LIKE %value%** | 需增加前端输入框（对应 part.locationRemark） |
| 配件来源 | source | 精确 | 精确 | 已满足 |
| 启用状态 | enabled | 精确 | 精确 | 已满足 |

**提示文案建议**：
- 配件编码：`请输入配件编码`
- 配件名称：`请输入配件名称`
- 官方品号：`请输入官方品号`
- 默认条码：`请输入条码`（新增）

---

### 6.5 库存管理

| 筛选条件 | 前端字段 | 后端匹配方式 | 目标匹配方式 | 改造说明 |
|---------|---------|------------|------------|---------|
| 配件名称 | partName | LIKE %value% | LIKE %value% | 已满足 |
| 配件编码 | partCode | 精确匹配(eq) | **LIKE %value%** | 需改为模糊匹配 |
| 官方品号 | 无前端字段 | — | **LIKE %value%** | 需增加前端输入框和后端查询 |
| 默认条码 | 无前端字段 | — | **LIKE %value%** | 需增加前端输入框和后端查询 |
| 仓位 | 无前端字段 | — | **LIKE %value%** | 需增加前端输入框（对应 part.locationRemark） |
| 配件来源 | source | 精确 | 精确 | 已满足 |
| 库存视图 | view | 精确 | 精确 | 已满足 |

---

### 6.6 收款记录

| 筛选条件 | 前端字段 | 后端匹配方式 | 目标匹配方式 | 改造说明 |
|---------|---------|------------|------------|---------|
| 工单号 | workOrderNo | LIKE %value% | LIKE %value% | 已满足 |
| 客户姓名 | customerName | LIKE %value% | LIKE %value% | 已满足 |
| 支付方式 | paymentMethod | 精确 | 精确 | 已满足 |
| 收款时间 | startTime/endTime | 日期范围 | 日期范围 | 已满足 |

**提示文案建议**：当前已正确，无需修改。

---

### 6.7 退款记录

| 筛选条件 | 前端字段 | 后端匹配方式 | 目标匹配方式 | 改造说明 |
|---------|---------|------------|------------|---------|
| 工单号 | workOrderNo | LIKE %value% | LIKE %value% | 已满足 |
| 客户姓名 | customerName | LIKE %value% | LIKE %value% | 已满足 |
| 退款方式 | refundMethod | 精确 | 精确 | 已满足 |
| 退款时间 | startTime/endTime | 日期范围 | 日期范围 | 已满足 |

---

### 6.8 报销管理

| 筛选条件 | 前端字段 | 后端匹配方式 | 目标匹配方式 | 改造说明 |
|---------|---------|------------|------------|---------|
| 报销编号 | reimbursementNo | LIKE %value% | LIKE %value% | 已满足 |
| 报销人 | applicantId | 精确(数字ID) | **模糊（改为员工姓名搜索）** | 需后端增加按员工姓名搜索 |
| 报销状态 | status | 精确 | 精确 | 已满足 |
| 提交时间 | dateFrom/dateTo | 日期范围 | 日期范围 | 已满足，参数名建议统一 |

**提示文案建议**：
- 报销人：`请输入报销人姓名`（当前为 `请输入报销人ID`，不友好）

**改造说明**：当前按 applicantId（数字）搜索不友好，建议增加按员工姓名模糊搜索，后端 JOIN sys_user.real_name。

---

### 6.9 官方结算

| 筛选条件 | 前端字段 | 后端匹配方式 | 目标匹配方式 | 改造说明 |
|---------|---------|------------|------------|---------|
| 工单号 | workOrderNo | LIKE %value% | LIKE %value% | 已满足 |
| 官方订单号 | officialOrderNo | LIKE %value% | LIKE %value% | 已满足 |
| 结算状态 | settlementStatus | 精确 | 精确 | 已满足 |
| 结算时间 | startTime/endTime | 日期范围 | 日期范围 | 已满足 |

---

### 6.10 员工与权限

| 筛选条件 | 前端字段 | 后端匹配方式 | 目标匹配方式 | 改造说明 |
|---------|---------|------------|------------|---------|
| 员工账号 | username | LIKE %value% | LIKE %value% | 已满足 |
| 姓名 | realName | LIKE %value% | LIKE %value% | 已满足 |
| 手机号 | 无前端字段 | — | **LIKE %value%** | 需增加前端输入框和后端查询 |
| 角色 | roleCode | 精确 | 精确 | 已满足 |
| 状态 | enabled | 精确 | 精确 | 已满足 |

**修复项**：pageSize 切换不重置 pageNo 的问题。

---

### 6.11 财务报表 / 收银日报

当前无文本搜索，仅日期选择查询报表。不在本批次搜索改造范围内。

---

## 7. 当前代码能力差距汇总

### 7.1 后端查询差距

| 编号 | 接口 | 差距 | 改造类型 |
|------|------|------|---------|
| B1 | GET /api/staff/customers/search | keyword 不匹配 remark | 增加 OR 条件 |
| B2 | GET /api/staff/vehicles/search | 不匹配 customer.customer_name | 增加子查询 |
| B3 | GET /api/staff/parts | partCode 精确→需模糊；不匹配 officialPartNo、defaultBarcode、locationRemark | 改为统一 keyword LIKE |
| B4 | GET /api/staff/inventory/stocks | partCode 精确→需模糊；不匹配 officialPartNo、defaultBarcode、model、locationRemark | 改为统一 keyword LIKE |
| B5 | GET /api/staff/work-orders | 不匹配 battery_no_snapshot | 增加 OR 条件 |
| B6 | GET /api/admin/customers | phone 精确→需模糊 | 改 eq 为 like |
| B7 | GET /api/admin/vehicles | 不支持 customerName 筛选 | 增加字段 |
| B8 | GET /api/admin/work-orders | workOrderNo 精确→需模糊；cashierStatus 未实现 | 改 eq 为 like + 增加字段 |
| B9 | GET /api/admin/parts | partCode/officialPartNo 精确→需模糊；缺 defaultBarcode、locationRemark | 扩展模糊字段 |
| B10 | GET /api/admin/inventory/stocks | partCode 精确→需模糊；缺 officialPartNo、defaultBarcode、locationRemark | 扩展模糊字段 |
| B11 | GET /api/admin/reimbursements | 报销人仅支持 ID 精确查，不支持姓名模糊 | 增加姓名 LIKE |
| B12 | GET /api/admin/users | 不支持手机号搜索 | 增加字段 |

### 7.2 前端差距

| 编号 | 页面 | 差距 | 改造类型 |
|------|------|------|---------|
| F1 | 小程序创建工单-配件搜索 | 本地过滤不匹配 officialPartNo、defaultBarcode、model、locationRemark | 扩展 includes 条件 |
| F2 | 小程序入库-配件搜索 | 同 F1 | 扩展 includes 条件 |
| F3 | 小程序创建工单-配件搜索 | 无防抖 | 添加 300ms 防抖 |
| F4 | 管理端客户管理 | 分页 BUG（翻页永远回到第1页） | 修复事件绑定 |
| F5 | 管理端车辆档案 | 分页 BUG（同上） | 修复事件绑定 |
| F6 | 管理端工单管理 | cashierStatus 筛选器无效 | 修复 API 映射 |
| F7 | 管理端客户/车辆管理 | 提示文案仅为"请输入" | 补充具体文案 |
| F8 | 管理端配件管理 | 缺条码/仓位搜索框 | 增加输入框 |
| F9 | 管理端库存管理 | 缺官方品号/条码/仓位搜索框 | 增加输入框 |
| F10 | 管理端车辆档案 | 缺客户姓名搜索框 | 增加输入框 |
| F11 | 管理端工单管理 | 缺车型搜索框 | 增加输入框 |
| F12 | 管理端员工管理 | 缺手机号搜索框 | 增加输入框 |
| F13 | 管理端报销管理 | 报销人按 ID 搜索不友好 | 改为按姓名搜索 |

---

## 8. 后端查询设计建议

### 8.1 改造原则

1. 只改查询条件（QueryWrapper / LambdaQueryWrapper 的 WHERE 子句），不改数据结构
2. 不做全文索引，不引入 ES / Redis / MQ
3. LIKE 查询先满足小数据量场景
4. keyword 和文本字段统一 trim
5. 权限和门店隔离必须保持（store_id 条件不动）
6. 不改变接口业务语义和返回结构

### 8.2 keyword 统一匹配模式

小程序接口统一使用 keyword 参数，后端用 OR 连接多个字段：

```
WHERE store_id = ? AND deleted = 0
  AND (
    field1 LIKE CONCAT('%', ESCAPE_LIKE(keyword), '%')
    OR field2 LIKE CONCAT('%', ESCAPE_LIKE(keyword), '%')
    OR ...
  )
```

### 8.3 管理端独立字段模式

管理端保留多条件表单，每个文本字段独立 LIKE：

```
WHERE store_id = ? AND deleted = 0
  AND (field1 IS NULL OR field1 LIKE CONCAT('%', ESCAPE_LIKE(:field1), '%'))
  AND (field2 IS NULL OR field2 LIKE CONCAT('%', ESCAPE_LIKE(:field2), '%'))
  AND status = :status   -- 结构化条件精确匹配
```

### 8.4 配件/库存 keyword 改造

当前配件查询使用独立字段 eq + like 混合模式，需改为统一 keyword 查询。

**当前**（PartServiceImpl.buildPartQueryWrapper）：
```
partCode = ? (eq)     -- 精确
partName LIKE %?%     -- 模糊
officialPartNo = ?    -- 精确
model LIKE %?%        -- 模糊
```

**目标**（小程序 keyword 模式）：
```
(partCode LIKE %keyword%
 OR partName LIKE %keyword%
 OR officialPartNo LIKE %keyword%
 OR defaultBarcode LIKE %keyword%
 OR model LIKE %keyword%
 OR locationRemark LIKE %keyword%)
```

**目标**（管理端独立字段模式）：
```
partCode LIKE %?%       -- 精确→模糊
partName LIKE %?%       -- 已模糊
officialPartNo LIKE %?% -- 精确→模糊
defaultBarcode LIKE %?% -- 新增
model LIKE %?%          -- 已模糊
locationRemark LIKE %?% -- 新增
source = ?              -- 保持精确
status = ?              -- 保持精确
```

### 8.5 库存查询改造

**当前**（InventoryServiceImpl.findPartIdsByFilters）：
```
先查 part: partCode = ? (eq) AND partName LIKE %?%
再查 inventory_stock: part_id IN (matched_ids)
```

**目标**：
```
先查 part: (partCode LIKE %keyword% OR partName LIKE %keyword%
            OR officialPartNo LIKE %keyword% OR defaultBarcode LIKE %keyword%
            OR model LIKE %keyword% OR locationRemark LIKE %keyword%)
再查 inventory_stock: part_id IN (matched_ids)
```

### 8.6 LIKE 安全治理

建议作为独立治理项，在所有 LIKE 查询处统一转义：

```java
/**
 * 转义 LIKE 通配符，防止 % 和 _ 被当作通配符
 */
public static String escapeLike(String value) {
    if (value == null) return null;
    return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
}
```

在 MyBatis-Plus 的 `.like()` 调用前对参数执行此转义。

---

## 9. 分页/筛选/导出联动规则

### 9.1 分页联动

| 操作 | pageNo 行为 | 筛选条件 |
|------|------------|---------|
| 点击搜索/重置按钮 | 重置为 1 | 保留/清空 |
| 翻页（点击页码或上下页） | 更新为点击的页码 | **保留当前所有筛选条件** |
| 切换 pageSize | 重置为 1 | 保留当前所有筛选条件 |
| 切换状态 Tab（小程序） | 重置为 1 | 保留 keyword |

### 9.2 导出联动

导出接口必须接受与列表查询相同的筛选参数，确保导出范围与当前页面展示一致。

### 9.3 权限隔离

所有查询必须包含 `store_id = 当前用户门店ID` 条件，不可移除。
平台管理员查询走独立的 `/api/platform/**` 接口。

### 9.4 已知 BUG 修复清单

| BUG | 位置 | 修复方式 |
|-----|------|---------|
| 分页永远停在第1页 | 管理端客户管理 `@current-change="handleSearch"` | 改为 `@current-change="fetchData"` |
| 分页永远停在第1页 | 管理端车辆档案 `@current-change="handleSearch"` | 改为 `@current-change="fetchData"` |
| cashierStatus 无效 | 管理端工单管理 API 映射 | 在 `backendParams` 中增加 `cashierStatus` |
| pageSize 切换不重置 | 管理端员工管理 | `@size-change` 事件中增加 `pageNo = 1` |

---

## 10. 实现批次

### M27B-1：小程序高频搜索增强

**范围**：
1. 创建工单页配件搜索：扩展本地过滤匹配 officialPartNo、defaultBarcode、model、locationRemark
2. 创建工单页配件搜索：添加 300ms 防抖
3. 入库页配件搜索：同步扩展本地过滤和防抖
4. 后端客户搜索 keyword 增加 remark 字段（B1）
5. 后端车辆搜索 keyword 增加 customerName 子查询（B2）
6. 小程序提示文案更新

**修改范围**：
- `mini-program/src/pages/create-work-order-placeholder/index.ts`
- `mini-program/src/pages/inbound-placeholder/index.ts`
- `mini-program/src/pages/work-orders/index.wxml`（提示文案）
- `mini-program/src/pages/inventory/index.wxml`（提示文案）
- `mini-program/src/pages/parts/index.wxml`（提示文案）
- `backend/.../customer/service/impl/CustomerServiceImpl.java`（B1）
- `backend/.../customer/service/impl/VehicleServiceImpl.java`（B2）

**是否需要 backend**：是（B1、B2）

**是否需要数据库 migration**：否

**是否影响核心业务规则**：否，仅查询条件扩展

**测试重点**：
- 创建工单页输入官方品号，验证能搜到配件
- 创建工单页输入客户姓名，验证能搜到车辆
- 工单列表输入手机号后4位，验证能搜到工单
- 状态筛选与 keyword 同时生效
- 搜索后翻页，keyword 不丢失

**部署方式**：后端独立部署 + 小程序上传体验版

**小程序是否需要上传体验版**：是

---

### M27B-2：管理端结构化模糊筛选增强

**范围**：
1. 修复管理端客户/车辆分页 BUG（F4、F5）
2. 修复 cashierStatus 筛选无效（F6 + B8）
3. 客户管理手机号改为模糊（B6）
4. 车辆档案增加客户姓名搜索框（F10 + B7）
5. 工单管理工单号改为模糊 + 增加车型搜索框（B8 + F11）
6. 配件管理增加条码/仓位搜索框 + partCode/officialPartNo 改模糊（F8 + B9）
7. 库存管理增加官方品号/条码/仓位搜索框 + partCode 改模糊（F9 + B10）
8. 员工管理增加手机号搜索框（F12 + B12）
9. 报销管理报销人改为姓名搜索（F13 + B11）
10. 管理端提示文案统一更新（F7）

**修改范围**：
- `admin-web/src/views/customer/index.vue`
- `admin-web/src/views/vehicle/index.vue`
- `admin-web/src/views/work-order/index.vue`
- `admin-web/src/views/parts/index.vue`
- `admin-web/src/views/inventory/index.vue`
- `admin-web/src/views/user/index.vue`
- `admin-web/src/views/reimbursement/index.vue`
- `admin-web/src/api/workOrder.ts`
- `backend/.../customer/service/impl/CustomerServiceImpl.java`
- `backend/.../customer/service/impl/VehicleServiceImpl.java`
- `backend/.../workorder/service/impl/WorkOrderServiceImpl.java`
- `backend/.../part/service/impl/PartServiceImpl.java`
- `backend/.../inventory/service/impl/InventoryServiceImpl.java`
- `backend/.../reimbursement/service/impl/ReimbursementServiceImpl.java`
- `backend/.../user/service/impl/AdminUserServiceImpl.java`

**是否需要 backend**：是

**是否需要数据库 migration**：否

**是否影响核心业务规则**：否，仅查询条件扩展

**测试重点**：
- 管理端客户/车辆翻页正常
- 工单管理 cashierStatus 筛选生效
- 各页面文本搜索支持模糊输入
- 新增搜索框功能正常
- 导出范围与筛选一致
- 权限隔离未被破坏

**部署方式**：后端独立部署 + 管理端独立部署

**小程序是否需要上传体验版**：否

---

### M27B-3：搜索输入规范与 LIKE 安全治理

**范围**：
1. 后端统一 trim（所有 Controller 层 keyword / 文本参数）
2. 后端 LIKE 通配符转义（% → \%，_ → \_）
3. 空字符串不传（前端统一：空 keyword 不传参；后端兜底：空字符串不加 LIKE 条件）
4. 大小写统一（前端本地过滤统一 toLowerCase；后端可选 COLLATE utf8mb4_general_ci 默认已不区分）
5. 报销页日期参数命名统一（dateFrom/dateTo → startTime/endTime 或反向统一）
6. 管理端员工管理 pageSize 切换修复

**修改范围**：
- 多个后端 Controller / Service（trim + LIKE 转义）
- `admin-web/src/api/reimbursement.ts`（日期参数）
- `admin-web/src/views/user/index.vue`（pageSize 修复）

**是否需要 backend**：是

**是否需要数据库 migration**：否

**是否影响核心业务规则**：否

**测试重点**：
- 搜索输入含 `%` 或 `_` 验证不返回异常结果
- 搜索输入前后有空格验证正常
- 空搜索不返回全量数据
- 报销页日期范围筛选正常

**部署方式**：后端独立部署 + 管理端独立部署

**小程序是否需要上传体验版**：否（如仅涉及后端则不需要）

---

## 11. 风险

### 11.1 不涉及的内容

| 类别 | 说明 |
|------|------|
| 不新增数据库字段 | 无车牌号、无规格等字段暂不新增，评估后再决定 |
| 不改表结构 | 不做 migration |
| 不改状态机 | 工单状态、库存状态机、支付流程不受影响 |
| 不改权限模型 | store_id 隔离不变 |
| 不做全文索引 | 当前数据量 LIKE %keyword% 可接受 |
| 不引入新基础设施 | 不引入 ES / Redis / MQ |

### 11.2 性能风险

| 风险 | 说明 | 缓解措施 |
|------|------|---------|
| LIKE %keyword% 全表扫描 | B-tree 索引无法用于前缀 % 查询 | 当前数据量小，按门店过滤后行数有限，可接受 |
| 配件/库存扩展 OR 条件 | OR 比 AND 更慢 | 按门店过滤后行数有限，可接受 |
| 小程序本地过滤无分页 | 全量加载配件到前端 | 当前配件量小；若增长超过 500 条，建议改为后端分页接口 |

### 11.3 兼容性风险

| 风险 | 说明 | 缓解措施 |
|------|------|---------|
| 改精确为模糊 | 管理端工单号/配件编码从精确改为模糊 | LIKE %keyword% 天然包含精确匹配，不会影响已有精确搜索场景 |
| keyword 扩展字段 | 小程序搜索匹配更多字段 | 结果变多而非变少，不会丢失已有结果 |
| cashierStatus 新增传递 | 之前不生效，修复后生效 | 需确认后端是否已支持该参数 |

---

## 12. 验收标准

### 12.1 小程序验收

| 场景 | 预期 |
|------|------|
| 创建工单-客户搜索输入手机号后4位 | 能搜到对应客户 |
| 创建工单-客户搜索输入备注关键词 | 能搜到对应客户 |
| 创建工单-车辆搜索输入车架号后6位 | 能搜到对应车辆 |
| 创建工单-车辆搜索输入客户姓名 | 能搜到对应车辆 |
| 创建工单-配件搜索输入官方品号 | 能搜到对应配件 |
| 创建工单-配件搜索输入条码 | 能搜到对应配件 |
| 工单列表输入手机号后4位 + 选择状态 | 两个条件同时生效 |
| 工单列表搜索后翻页 | keyword 和 status 不丢失 |
| 库存查询输入官方品号 | 能搜到对应库存 |
| 配件列表输入部分编码 | 能搜到对应配件 |
| 各搜索框提示文案 | 明确告知可搜索字段 |
| 搜索无结果 | 显示友好空状态，非报错 |

### 12.2 管理端验收

| 场景 | 预期 |
|------|------|
| 客户管理翻到第2页 | 结果正确，不跳回第1页 |
| 车辆档案翻到第2页 | 结果正确，不跳回第1页 |
| 工单管理 cashierStatus 筛选 | 结果有变化 |
| 工单管理输入部分工单号 | 能搜到相关工单 |
| 配件管理按条码搜索 | 能搜到对应配件 |
| 配件管理输入部分官方品号 | 能搜到对应配件 |
| 库存管理输入官方品号 | 能搜到对应库存 |
| 员工管理按手机号搜索 | 能搜到对应员工 |
| 报销管理按姓名搜索 | 能搜到对应报销 |
| 搜索后切换页码 | 筛选条件保留 |
| 搜索后点击重置 | 所有条件清空，回到默认列表 |
| 搜索后导出 | 导出范围与当前筛选一致 |

### 12.3 安全验收

| 场景 | 预期 |
|------|------|
| 搜索输入 `%` | 不返回全表数据 |
| 搜索输入 `_` | 不匹配任意字符 |
| 搜索输入前后空格 | 正常匹配（自动 trim） |
| 不同门店数据 | 互相不可见（store_id 隔离） |

---

## 最终输出

1. **是否修改代码**：否
2. **是否修改数据库**：否
3. **是否 push**：否
4. **文档路径**：`docs/dev/m27-search-design.md`
5. **小程序搜索字段矩阵摘要**：
   - 客户搜索：4 字段（姓名、手机号、手机号后4位、备注）
   - 车辆搜索：7 字段（车架号、车架后6位、车型、电池号、客户姓名、客户手机号、手机号后4位）
   - 配件搜索：6 字段（名称、编码、官方品号、条码、型号、仓位）
   - 工单列表：8 字段（工单号、客户名、手机号、手机号后4位、车架号、车架后6位、车型、电池号）
   - 库存/配件列表：6 字段（名称、编码、官方品号、条码、型号、仓位）
6. **管理端搜索字段矩阵摘要**：保留多条件结构化筛选，文本条件统一改模糊，新增条码/仓位/客户姓名/手机号/车型等搜索框
7. **是否需要 backend**：是（M27B-1 部分、M27B-2 全部、M27B-3 全部）
8. **是否需要数据库 migration**：否
9. **建议优先实现批次**：M27B-1（小程序高频）→ M27B-2（管理端增强）→ M27B-3（规范治理）
10. **git status**：见下方
