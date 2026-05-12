# 04_DOMAIN_MODEL.md

# 小牛电动官方授权店两轮车维修售后库存管理系统 - 领域模型设计文档

版本：v0.3  
日期：2026-05-10  
阶段：项目准备阶段 Step 4 / 8  
修订依据：工单费用项目明细模型升级 2026-05-10  
前置文档：

- `01_MVP_SCOPE.md`
- `02_BUSINESS_FLOWS.md`
- `03_CORE_BUSINESS_RULES.md`

用途：用于抽象系统核心领域对象、对象关系、边界和聚合，为后续数据库设计、API 设计和 Codex 实现提供业务模型依据。

本次修订重点：

```text
1. 将 WorkOrderItem（工单配件明细）升级为 WorkOrderChargeItem（工单费用项目明细），统一承载配件费、工时费和其他费用。
2. 明确 charge_type = PART / LABOR / OTHER 三类明细语义。
3. 明确只有 charge_type = PART 且 inventory_affecting = 1 的明细才影响库存。
4. 明确 labor_fee / other_fee 不再作为工单金额的主要来源字段。
5. 明确 receivable_amount 由 work_order_charge_item.line_amount 汇总得到。
6. 更新对象关系、核心动作、聚合设计和不应混淆的对象章节。
```

---

## 1. 文档目的

本文档用于回答：

1. 系统有哪些核心业务对象？
2. 每个对象的业务含义是什么？
3. 对象之间是什么关系？
4. 哪些对象是主对象，哪些对象是明细或流水？
5. 哪些对象不能混在一起？
6. 哪些对象需要独立建模，避免后续返工？
7. 后续数据库设计应围绕哪些领域模型展开？

本文档不是数据库设计文档。

本文档不会详细定义 SQL 字段、索引、外键、表结构和 migration。  
这些内容将在 Step 5：`05_DATABASE_DESIGN.md` 中完成。

---

## 2. 领域模型总览

本系统可以拆成 10 个核心领域：

```text
1. 门店与组织领域 Store & Organization
2. 用户与权限领域 User & Permission
3. 字典与配置领域 Dictionary & Configuration
4. 客户与车辆领域 Customer & Vehicle
5. 配件领域 Part
6. 库存领域 Inventory
7. 工单领域 Work Order
8. 支付与退款领域 Payment & Refund
9. 官方售后领域 Official After-Sales
10. 报销与财务领域 Reimbursement & Finance
```

总体对象关系：

```text
门店 Store
  ├── 员工 User
  ├── 角色 Role
  ├── 权限 Permission
  ├── 配件 Part
  │     └── 库存 InventoryStock
  │           └── 库存流水 InventoryFlow
  ├── 客户 Customer
  │     └── 车辆 Vehicle
  │           └── 工单 WorkOrder
  │                 ├── 工单费用明细 WorkOrderChargeItem
  │                 │     └── 配件 Part（仅 PART 类型）
  │                 ├── 支付记录 PaymentRecord
  │                 ├── 退款记录 RefundRecord
  │                 ├── 官方售后信息 OfficialAfterSales
  │                 └── 工单状态日志 WorkOrderStatusLog
  ├── 报销记录 Reimbursement
  └── 财务统计 FinanceReport
```

---

## 3. 核心建模原则

### 3.1 工单是业务主线

维修业务围绕工单展开。

工单关联：

```text
客户
车辆
维修项目
使用配件
支付记录
退款记录
官方售后订单号
结算状态
库存扣减
财务统计
```

工单不是简单表单，而是驱动库存和财务变化的核心业务对象。

---

### 3.2 库存是独立领域，不附属于配件字段

配件是主数据，库存是业务状态。

禁止把库存简单建模为：

```text
Part.stock
```

应该区分：

```text
Part               配件主数据
InventoryStock     当前库存快照
InventoryFlow      库存流水
```

库存变化必须通过库存领域完成。

---

### 3.3 支付和退款必须独立建模

支付和退款不是工单上的几个字段。

必须独立为：

```text
PaymentRecord
RefundRecord
```

原因：

```text
一个工单可以多次付款
一个工单可以混合付款
一个工单可以多次退款
退款不能删除原支付
实收金额来自支付和退款明细汇总
```

---

### 3.4 官方结算和客户支付必须独立

官方结算不是客户支付。

必须区分：

```text
客户支付收入
官方结算收入
```

官方售后信息可以关联工单，但不能混入支付记录。

---

### 3.5 报销是运营成本入口

MVP 阶段暂不统计房租、水电、工资等复杂成本。

报销记录是当前运营成本的主要入口。

只有已确认报销才进入财务成本。

---

### 3.6 财务报表是汇总视图，不是手工账本

财务统计来自业务明细：

```text
支付记录
退款记录
官方结算记录
工单配件成本（charge_type = PART 的 line_cost_amount）
报销记录
```

财务报表不应成为可随意编辑利润的主数据。

已结算工单发生后续退款时：

```text
退款记录影响财务报表中的客户支付净收入
不自动回退工单状态
不自动回滚库存
不自动触发反结算
```

复杂反结算、售后退货和库存回滚流程后续单独设计。

---

### 3.7 创建工单和提交工单必须分离

为了保证库存预占时机清楚，必须区分：

```text
创建工单：生成 DRAFT 草稿，不预占库存，不形成收入
提交工单：DRAFT → PENDING_ACCEPT，触发库存预占
```

禁止把“创建工单”和“提交工单”合并成一个隐式动作。

后续 Codex 实现时，提交工单必须是独立业务动作，不允许通过普通更新接口直接修改状态。

---

### 3.8 核心对象必须有审计字段

所有核心业务对象应统一保留审计字段：

```text
created_by
created_at
updated_by
updated_at
```

关键业务动作还应记录对应操作人和操作时间，例如：

```text
提交人 / 提交时间
结算人 / 结算时间
取消人 / 取消时间
退款操作人 / 退款时间
官方结算操作人 / 官方结算时间
报销确认人 / 确认时间
```

审计字段不替代业务流水，库存、支付、退款、官方结算、报销仍必须有独立明细或状态日志。

---

## 4. 领域边界划分

### 4.1 Store & Organization 门店与组织领域

负责：

```text
门店信息
单门店 MVP
未来多门店预留
```

核心对象：

```text
Store
```

MVP 规则：

```text
当前只启用一个门店
所有核心业务对象预留 store_id
不做门店调拨
不做多门店汇总
```

---

### 4.2 User & Permission 用户与权限领域

负责：

```text
员工账号
微信绑定
手机号登录
角色
权限点
访问控制
操作人记录
```

核心对象：

```text
User
Role
Permission
UserRole
RolePermission
```

MVP 规则：

```text
角色 + 权限点
不把权限写死在角色名称里
关键操作必须校验权限
```

---

### 4.3 Dictionary & Configuration 字典与配置领域

负责：

```text
工单状态展示
支付方式
配件来源
配件分类
报销状态
库存流水类型
官方结算状态
```

核心对象：

```text
DictType
DictItem
```

MVP 规则：

```text
简单字典表即可
不做低代码配置平台
核心状态仍需后端枚举保护
工单状态字典仅用于展示名称、排序、启停等展示配置
工单状态流转必须由后端枚举 + 状态机 / 业务动作控制
```

---

### 4.4 Customer & Vehicle 客户与车辆领域

负责：

```text
客户基础信息
车辆基础信息
车架号维修历史
```

核心对象：

```text
Customer
Vehicle
```

MVP 规则：

```text
不做复杂 CRM
维修记录必须能按车架号追溯
客户信息可以先轻量化
```

---

### 4.5 Part 配件领域

负责：

```text
官方配件
第三方配件
配件编码
条形码
配件基础信息
配件启停
```

核心对象：

```text
Part
PartBarcode
```

MVP 规则：

```text
官方配件使用官方品号
第三方配件系统生成编码
工单销售价在工单中手动填写
临时配件也进入配件主数据
```

---

### 4.6 Inventory 库存领域

负责：

```text
入库
库存快照
库存预占
库存释放
库存扣减
库存调整
库存流水
```

核心对象：

```text
InventoryStock
InventoryFlow
```

说明：

```text
MVP 不单独建模 InventoryOperation。
入库、预占、释放、消耗、调整等库存动作通过 InventoryFlow.flow_type 和业务来源字段表达。
避免 Codex 因 InventoryOperation 概念不清而额外造表。
```

MVP 规则：

```text
所有库存变化都必须产生库存流水
提交工单时预占
取消工单时释放
结算工单时扣减
库存调整必须记录原因
```

---

### 4.7 Work Order 工单领域

负责：

```text
维修工单
工单状态
工单费用项目明细（配件费 / 工时费 / 其他费用）
结算动作
取消动作
维修历史
```

核心对象：

```text
WorkOrder
WorkOrderChargeItem
WorkOrderStatusLog
```

MVP 规则：

```text
工单是维修业务主对象
创建工单进入 DRAFT 草稿，不预占库存
提交工单从 DRAFT 变为 PENDING_ACCEPT，只对 charge_type = PART 且 inventory_affecting = 1 的明细触发库存预占
工单结算只对 PART 类型明细扣减库存
工单取消只对 PART 类型明细释放库存
工单应收金额 receivable_amount = Σ work_order_charge_item.line_amount
```

---

### 4.8 Payment & Refund 支付与退款领域

负责：

```text
客户付款
多次付款
混合付款
退款记录
实收金额计算
结算条件支持
```

核心对象：

```text
PaymentRecord
RefundRecord
```

MVP 规则：

```text
支付和退款都是明细
退款不删除原支付
实收金额 = 支付总额 - 退款总额
```

---

### 4.9 Official After-Sales 官方售后领域

负责：

```text
官方售后订单号
官方结算金额
官方结算状态
官方结算时间
官方结算备注
```

核心对象：

```text
OfficialAfterSales
```

后续扩展对象：

```text
OfficialSettlementRecord
```

说明：

```text
MVP 单次手动官方结算场景下不单独拆 OfficialSettlement。
官方结算金额、状态、时间、备注先作为 OfficialAfterSales 的字段表达。
```

MVP 规则：

```text
官方结算手动录入
官方结算和客户支付分开
不对接官方系统
```

---

### 4.10 Reimbursement & Finance 报销与财务领域

负责：

```text
报销提交
报销确认
报销驳回/取消
运营成本
收入统计
成本统计
利润统计
Excel 导出
```

核心对象：

```text
Reimbursement
FinanceReport
```

可选扩展对象：

```text
ExportRecord
```

说明：

```text
MVP Excel 导出可先采用同步下载文件流。
ExportRecord 只作为导出审计或异步导出扩展概念，不作为第一版核心领域对象。
```

MVP 规则：

```text
已确认报销才计入成本
财务统计从业务明细汇总
Excel 导出基于查询条件
```

---

## 5. 核心领域对象清单

### 5.1 Store 门店

业务含义：

```text
系统中的门店主体。
MVP 只启用单门店，但所有核心业务对象需要预留门店维度。
```

核心属性概念：

```text
门店名称
门店编码
联系电话
地址
启用状态
```

关联关系：

```text
Store 1 - N User
Store 1 - N WorkOrder
Store 1 - N Part
Store 1 - N InventoryStock
Store 1 - N PaymentRecord
Store 1 - N Reimbursement
```

MVP 规则：

```text
只有一个默认门店
不做门店间调拨
```

---

### 5.2 User 员工 / 系统用户

业务含义：

```text
门店员工、管理员、财务、老板等系统使用者。
```

核心属性概念：

```text
姓名
手机号
微信 openid / unionid
登录账号
启用状态
所属门店
```

关联关系：

```text
User N - N Role
User 1 - N 操作记录
User 1 - N 工单创建记录
User 1 - N 支付/退款操作记录
User 1 - N 报销记录
```

MVP 规则：

```text
支持手机号登录
支持微信登录和绑定手机号
停用用户不能登录
```

---

### 5.3 Role 角色

业务含义：

```text
用于聚合权限点，控制不同岗位可访问的功能。
```

默认角色：

```text
超级管理员
门店管理员
财务
维修员 / 前台
```

关联关系：

```text
Role N - N Permission
Role N - N User
```

MVP 规则：

```text
角色名称不能代替权限判断
具体操作必须基于权限点控制
```

---

### 5.4 Permission 权限点

业务含义：

```text
系统中可授权的具体操作能力。
```

示例权限点：

```text
PART_MANAGE
INVENTORY_ADJUST
WORK_ORDER_SETTLE
REFUND_RECORD
OFFICIAL_SETTLEMENT_MANAGE
REIMBURSEMENT_CONFIRM
FINANCE_VIEW
EXCEL_EXPORT
```

MVP 规则：

```text
关键操作必须检查权限
权限点可配置但核心权限编码需稳定
```

---

### 5.5 DictType / DictItem 字典

业务含义：

```text
用于维护系统中的可配置选项。
```

适用内容：

```text
支付方式
配件来源
配件分类
报销状态
官方结算状态
库存流水类型
```

MVP 规则：

```text
字典用于展示和扩展
核心业务状态仍由后端枚举约束
```

---

### 5.6 Customer 客户

业务含义：

```text
维修业务中的客户信息。
```

核心属性概念：

```text
客户姓名
联系方式
备注
```

关联关系：

```text
Customer 1 - N Vehicle
Customer 1 - N WorkOrder
```

MVP 规则：

```text
不做复杂客户档案
允许同一客户有多辆车
```

---

### 5.7 Vehicle 车辆

业务含义：

```text
客户的两轮车车辆信息。
```

核心属性概念：

```text
车型
车架号
电池号
客户
备注
```

关联关系：

```text
Vehicle 1 - N WorkOrder
Vehicle N - 1 Customer
```

MVP 规则：

```text
维修历史优先按车架号追溯
车架号应尽量唯一
```

---

### 5.8 Part 配件

业务含义：

```text
官方配件或第三方配件的主数据。
```

核心属性概念：

```text
配件编码
官方品号
配件名称
型号
来源
分类
条形码
成本价参考
启用状态
库存位置备注
创建来源
```

配件来源：

```text
OFFICIAL      官方配件
THIRD_PARTY   第三方配件
```

创建来源：

```text
NORMAL          正常创建
INBOUND         入库时创建
WORK_ORDER_TEMP 工单临时创建
```

关联关系：

```text
Part 1 - N InventoryStock
Part 1 - N InventoryFlow
Part 1 - N WorkOrderChargeItem
```

MVP 规则：

```text
第三方配件编码由系统生成
工单销售价不依赖配件默认售价
临时配件也必须成为 Part
```

---

### 5.9 PartBarcode 配件条码

业务含义：

```text
用于扫码识别配件。
```

核心属性概念：

```text
配件
条码值
条码类型
是否主条码
启用状态
```

关联关系：

```text
Part 1 - N PartBarcode
```

MVP 规则：

```text
一个配件可有一个或多个条码
扫码找不到时允许创建配件
```

---

### 5.10 InventoryStock 库存快照

业务含义：

```text
当前某门店某配件的库存状态。
```

核心属性概念：

```text
门店
配件
实际库存 actual_qty
可用库存 available_qty
预占库存 reserved_qty
最后变动时间
```

关联关系：

```text
InventoryStock N - 1 Store
InventoryStock N - 1 Part
InventoryStock 1 - N InventoryFlow
```

MVP 规则：

```text
库存快照用于快速查询
库存变化必须通过库存流水追溯
不能只修改库存快照而不写流水
```

---

### 5.11 InventoryFlow 库存流水

业务含义：

```text
记录每一次库存变化。
```

流水类型：

```text
INBOUND   入库
RESERVE   预占
RELEASE   释放
CONSUME   消耗
ADJUST    调整
```

核心属性概念：

```text
门店
配件
库存快照
流水类型
变动数量
变动前实际库存
变动后实际库存
变动前可用库存
变动后可用库存
变动前预占库存
变动后预占库存
业务来源类型
业务来源 ID
操作人
操作时间
原因/备注
```

关联关系：

```text
InventoryFlow N - 1 InventoryStock
InventoryFlow N - 1 Part
InventoryFlow N - 1 Store
InventoryFlow N - 1 WorkOrder 可选
InventoryFlow N - 1 WorkOrderChargeItem 可选
```

MVP 规则：

```text
库存变化必须有流水
库存流水原则上不删除
```

---

### 5.12 WorkOrder 工单

业务含义：

```text
一次维修售后业务的核心单据。
```

核心属性概念：

```text
工单编号
门店
客户
车辆
客户姓名快照
客户联系方式快照
车型快照
车架号快照
电池号快照
维修项目
状态
应收金额
提交时间
提交人
结算时间
结算人
取消时间
取消人
取消原因
备注
```

说明：

```text
labor_fee / other_fee 不再作为工单金额的主要来源字段，已废弃或弱化。
应收金额 receivable_amount 由 work_order_charge_item.line_amount 汇总得到。
```

关联关系：

```text
WorkOrder N - 1 Store
WorkOrder N - 1 Customer
WorkOrder N - 1 Vehicle
WorkOrder 1 - N WorkOrderChargeItem
WorkOrder 1 - N PaymentRecord
WorkOrder 1 - N RefundRecord
WorkOrder 1 - 0..1 OfficialAfterSales
WorkOrder 1 - N WorkOrderStatusLog
```

MVP 规则：

```text
创建工单后状态为 DRAFT，不预占库存
提交工单后状态从 DRAFT 变为 PENDING_ACCEPT，只对 charge_type = PART 且 inventory_affecting = 1 的明细触发库存预占
工单取消只对 PART 类型明细释放预占库存
工单结算只对 PART 类型明细扣减库存
工单结算必须满足实收金额 >= 应收金额
应收金额 receivable_amount = Σ work_order_charge_item.line_amount
```

---

### 5.13 WorkOrderChargeItem 工单费用项目明细

业务含义：

```text
工单中统一的收费项目明细，可为配件费、工时费或其他费用。
```

charge_type 取值：

```text
PART    配件费
LABOR   工时费
OTHER   其他费用
```

核心属性概念：

```text
工单
门店
费用类型 charge_type
项目名称 item_name
配件（仅 PART 类型）part_id
配件编码快照（仅 PART）
配件名称快照（仅 PART）
配件来源快照（仅 PART）
数量 quantity
单位 unit
单价 unit_price
金额 line_amount = quantity × unit_price
成本价快照（仅 PART）cost_price_snapshot
成本金额（仅 PART）line_cost_amount = quantity × cost_price_snapshot
是否影响库存 inventory_affecting（PART = 1，LABOR / OTHER = 0）
是否临时配件 is_temp_part
状态 ACTIVE / REMOVED
备注
```

核心规则：

```text
charge_type = PART 的明细才影响库存（预占、释放、扣减）
charge_type = PART 时 part_id 必填，inventory_affecting = 1
charge_type = LABOR / OTHER 时 part_id 为空，inventory_affecting = 0
配件收入 = Σ charge_type = PART 的 line_amount
人工费收入 = Σ charge_type = LABOR 的 line_amount
其他收入 = Σ charge_type = OTHER 的 line_amount
配件成本 = Σ charge_type = PART 的 line_cost_amount
```

关联关系：

```text
WorkOrderChargeItem N - 1 WorkOrder
WorkOrderChargeItem N - 0..1 Part（仅 PART 类型关联）
WorkOrderChargeItem 1 - N InventoryFlow 可选（仅 PART 类型）
```

MVP 规则：

```text
单价在工单明细中手动填写
成本价应记录快照，避免历史利润受后续成本变化影响
DRAFT 草稿阶段可普通增删改明细，不影响库存
DRAFT 删除 charge item 直接删除草稿明细，不生成库存流水
提交工单后不允许通过普通编辑直接改明细
如必须调整已提交未结算工单的明细，必须走”调整工单费用项目”专门动作
PART 类型调整必须同步库存预占差异
LABOR / OTHER 类型调整只更新明细和 receivable_amount，不影响库存
已结算工单不允许走该调整动作
```

---

### 5.14 WorkOrderStatusLog 工单状态日志

业务含义：

```text
记录工单状态变化过程。
```

核心属性概念：

```text
工单
原状态
新状态
操作人
操作时间
原因/备注
```

关联关系：

```text
WorkOrderStatusLog N - 1 WorkOrder
```

MVP 规则：

```text
每次关键状态变化都应记录
尤其是提交、取消、结算
```

---

### 5.15 PaymentRecord 支付记录

业务含义：

```text
客户对某个工单的付款明细。
```

核心属性概念：

```text
工单
支付金额
支付方式
支付时间
收款人
操作人
备注
```

关联关系：

```text
PaymentRecord N - 1 WorkOrder
PaymentRecord N - 1 Store
```

MVP 规则：

```text
一个工单允许多笔支付
一个工单允许混合支付
支付不等于结算
```

---

### 5.16 RefundRecord 退款记录

业务含义：

```text
客户退款明细。
```

核心属性概念：

```text
工单
退款金额
退款方式
退款时间
操作人
退款原因
备注
```

关联关系：

```text
RefundRecord N - 1 WorkOrder
RefundRecord N - 1 Store
```

MVP 规则：

```text
退款必须生成记录
不得删除原支付记录
退款影响实收金额
已结算后退款不自动反结算
```

---

### 5.17 OfficialAfterSales 官方售后信息

业务含义：

```text
工单对应的小牛官方售后业务信息。
```

核心属性概念：

```text
工单
是否官方售后
官方售后订单号
官方结算金额
官方结算状态
官方结算时间
官方结算备注
操作人
```

关联关系：

```text
OfficialAfterSales 1 - 1 WorkOrder
```

MVP 规则：

```text
官方结算手动录入
官方结算与客户支付分开统计
不自动同步官方系统
```

说明：

```text
MVP 不拆 OfficialSettlementRecord。
官方结算字段先放在 OfficialAfterSales 中。
后续如出现多次官方结算、部分结算、冲销等复杂场景，再扩展 OfficialSettlementRecord。
```

无论是否后续拆表，官方结算都不能混入 PaymentRecord。

---

### 5.18 Reimbursement 报销记录

业务含义：

```text
员工提交的门店运营支出报销。
```

核心属性概念：

```text
门店
报销人
用途
金额
备注
提交时间
状态
确认人
确认时间
驳回原因
```

状态：

```text
PENDING
CONFIRMED
REJECTED_OR_CANCELLED
```

关联关系：

```text
Reimbursement N - 1 Store
Reimbursement N - 1 User
```

MVP 规则：

```text
只有已确认报销计入成本
待确认不计入成本
已驳回/取消不计入成本
```

---

### 5.19 FinanceReport 财务报表

业务含义：

```text
基于业务明细汇总出来的收入、成本和利润视图。
```

FinanceReport 在领域上可以先作为查询模型，不一定需要物理表。

汇总来源：

```text
WorkOrder
WorkOrderChargeItem
PaymentRecord
RefundRecord
OfficialAfterSales
InventoryFlow / WorkOrderChargeItem 成本快照
Reimbursement
```

统计项：

```text
客户支付收入
官方结算收入
配件收入
人工费收入
其他收入
配件成本
报销成本
利润
```

MVP 规则：

```text
财务报表从明细汇总
不允许直接手改利润
日报、月报、时间范围查询即可
已结算工单后续发生退款时，退款会降低客户支付净收入
已结算后退款不自动回退工单状态，也不自动回滚库存
官方结算收入与客户支付净收入必须分开列示
```

---

### 5.20 ExportRecord 导出记录（可选扩展）

业务含义：

```text
记录 Excel 导出行为，用于审计或异步导出扩展。
```

MVP 处理原则：

```text
第一版可以不建 ExportRecord 表
Excel 导出可由接口按查询条件同步生成并返回文件
必须受权限控制
导出结果应与查询条件一致
```

后续数据量变大或需要异步导出时，再引入导出记录或导出任务。

可选属性概念：

```text
导出类型
查询条件
导出人
导出时间
文件名称
导出状态
```

---

## 6. 聚合设计建议

本节是领域建模建议，不是强制技术实现。

### 6.1 WorkOrder 聚合

聚合根：

```text
WorkOrder
```

聚合内对象：

```text
WorkOrderChargeItem
WorkOrderStatusLog
```

外部关联：

```text
Customer
Vehicle
PaymentRecord
RefundRecord
OfficialAfterSales
InventoryFlow
```

核心动作：

```text
创建工单
提交工单
取消工单
结算工单
调整工单费用项目
```

需要注意：

```text
提交、取消、结算会跨 Inventory、Payment 领域，因此必须通过应用服务协调事务。
```

---

### 6.2 Inventory 聚合

聚合根：

```text
InventoryStock
```

聚合内对象：

```text
InventoryFlow
```

核心动作：

```text
入库
预占
释放
消耗
调整
```

需要注意：

```text
任何库存变化都必须通过 Inventory 服务完成。
其他模块不得直接修改库存快照。
```

---

### 6.3 Payment 聚合

聚合根：

```text
PaymentRecord / RefundRecord
```

也可以视为围绕 WorkOrder 的支付明细集合。

核心动作：

```text
记录支付
记录退款
计算实收金额
校验可退款金额
```

需要注意：

```text
PaymentRecord 不负责修改工单状态。
工单结算由 WorkOrder 应用服务调用支付汇总结果判断。
```

---

### 6.4 OfficialAfterSales 聚合

聚合根：

```text
OfficialAfterSales
```

核心动作：

```text
录入官方订单号
录入官方结算金额
标记官方结算
```

需要注意：

```text
官方结算不等于客户支付。
不能把官方结算写入 PaymentRecord。
```

---

### 6.5 Reimbursement 聚合

聚合根：

```text
Reimbursement
```

核心动作：

```text
提交报销
确认报销
驳回/取消报销
```

需要注意：

```text
只有 CONFIRMED 报销进入财务成本。
```

---

### 6.6 Finance 查询模型

Finance 不建议作为强聚合根。

它更适合作为：

```text
查询服务
报表服务
汇总视图
```

数据来源：

```text
支付记录
退款记录
官方结算
工单费用项目明细成本（charge_type = PART 的 line_cost_amount）
报销记录
```

后期数据量大时，可再考虑日报/月报快照表。

MVP 不需要提前做复杂财务快照。

---

## 7. 关键对象关系

### 7.1 门店关系

```text
Store 1 - N User
Store 1 - N Part
Store 1 - N InventoryStock
Store 1 - N WorkOrder
Store 1 - N PaymentRecord
Store 1 - N RefundRecord
Store 1 - N Reimbursement
```

说明：

```text
MVP 只有一个 Store
但关系上必须保留 store_id
```

---

### 7.2 客户车辆关系

```text
Customer 1 - N Vehicle
Vehicle 1 - N WorkOrder
Customer 1 - N WorkOrder
```

说明：

```text
工单应保存客户和车辆快照字段
避免客户信息后续修改影响历史工单
```

---

### 7.3 工单费用项目关系

```text
WorkOrder 1 - N WorkOrderChargeItem
WorkOrderChargeItem N - 0..1 Part
Part 1 - N InventoryFlow
```

说明：

```text
WorkOrderChargeItem 必须保存单价和成本价快照
只有 charge_type = PART 的明细关联 Part 并影响库存
charge_type = LABOR / OTHER 的明细不关联 Part，不影响库存
```

---

### 7.4 工单支付关系

```text
WorkOrder 1 - N PaymentRecord
WorkOrder 1 - N RefundRecord
```

说明：

```text
实收金额 = PaymentRecord 汇总 - RefundRecord 汇总
```

---

### 7.5 工单官方售后关系

```text
WorkOrder 1 - 0..1 OfficialAfterSales
```

说明：

```text
普通第三方维修工单可以没有官方售后信息
官方售后工单必须有官方售后订单号或官方售后标记
```

---

### 7.6 报销财务关系

```text
User 1 - N Reimbursement
Store 1 - N Reimbursement
Reimbursement(CONFIRMED) → Finance 成本统计
```

---

## 8. 核心动作与领域协作

### 8.1 创建工单

参与对象：

```text
WorkOrder
WorkOrderChargeItem
Customer
Vehicle
Part
```

领域结果：

```text
生成工单
生成工单费用项目明细
工单状态为 DRAFT
不预占库存
不扣减库存
不形成收入
DRAFT 阶段添加 charge items 不影响库存
```

---

### 8.2 提交工单

参与对象：

```text
WorkOrder
WorkOrderChargeItem
InventoryStock
InventoryFlow
WorkOrderStatusLog
```

领域协作：

```text
校验工单状态必须为 DRAFT
遍历 work_order_charge_item，只处理 charge_type = PART 且 inventory_affecting = 1 的明细
校验 PART 类型明细对应配件库存
预占库存
生成库存流水
更新工单状态为 PENDING_ACCEPT
记录提交人和提交时间
记录状态日志
```

库存影响：

```text
只对 charge_type = PART 且 inventory_affecting = 1 的明细处理
available_qty 减少
reserved_qty 增加
actual_qty 不变
```

---

### 8.3 取消工单

参与对象：

```text
WorkOrder
WorkOrderChargeItem
InventoryStock
InventoryFlow
WorkOrderStatusLog
RefundRecord 可选
```

领域协作：

```text
校验工单未结算
只释放 charge_type = PART 且 inventory_affecting = 1 的明细对应的预占库存
生成库存流水
更新工单状态为已取消
记录取消原因
已有付款时另行记录退款
```

库存影响：

```text
只对 charge_type = PART 且 inventory_affecting = 1 的明细处理
reserved_qty 减少
available_qty 增加
actual_qty 不变
```

---

### 8.4 记录支付

参与对象：

```text
WorkOrder
PaymentRecord
User
```

领域协作：

```text
校验工单存在
记录支付金额、方式、收款人、时间
更新实收查询结果
不直接修改工单为已结算
```

---

### 8.5 记录退款

参与对象：

```text
WorkOrder
RefundRecord
PaymentRecord
User
```

领域协作：

```text
校验可退款金额
记录退款明细
影响实收金额
不删除原支付
```

---

### 8.6 调整工单费用项目（已提交未结算）

参与对象：

```text
WorkOrder
WorkOrderChargeItem
InventoryStock
InventoryFlow
WorkOrderStatusLog 可选
```

适用边界：

```text
仅用于已提交、未结算、未取消的工单
DRAFT 草稿阶段可普通编辑 charge items，不需要库存流水
已结算工单不走该动作
```

领域协作：

```text
校验工单已提交且未结算、未取消
对 charge_type = PART 且 inventory_affecting = 1 的明细：
  计算旧明细与新明细的数量差异
  对减少或删除的明细释放对应预占库存，生成 RELEASE 流水
  对新增或增加数量的明细校验可用库存后建立预占，生成 RESERVE 流水
对 charge_type = LABOR / OTHER 的明细：
  只更新明细和 receivable_amount，不写库存流水
更新工单费用项目明细
重新计算 receivable_amount
记录调整人、调整时间和调整原因
```

MVP 控制原则：

```text
第一版不允许通过普通编辑接口直接修改已提交工单费用明细
如果实现成本过高，可以先限制为：已提交工单如需大幅修改，取消后重建
无论采用哪种 UI，后端必须保证 PART 类型预占库存与工单费用明细一致
已结算工单不允许调整 charge item
```

库存影响：

```text
仅 charge_type = PART 且 inventory_affecting = 1 的明细涉及库存：
减少明细：reserved_qty 减少，available_qty 增加，actual_qty 不变
增加明细：available_qty 减少，reserved_qty 增加，actual_qty 不变
LABOR / OTHER 类型变更：不影响库存
```

---

### 8.7 结算工单

参与对象：

```text
WorkOrder
WorkOrderChargeItem
PaymentRecord
RefundRecord
InventoryStock
InventoryFlow
WorkOrderStatusLog
```

领域协作：

```text
计算应收金额 = Σ work_order_charge_item.line_amount
计算实收金额
校验实收金额 >= 应收金额
校验工单未取消、未结算
只对 charge_type = PART 且 inventory_affecting = 1 的明细扣减库存
生成库存消耗流水
更新工单状态为已结算
记录结算人和结算时间
```

库存影响：

```text
仅 charge_type = PART 且 inventory_affecting = 1 的明细涉及库存：
reserved_qty 减少
actual_qty 减少
available_qty 不变
```

---

### 8.8 录入官方结算

参与对象：

```text
WorkOrder
OfficialAfterSales
User
```

领域协作：

```text
校验工单存在
录入官方订单号
录入官方结算金额
标记官方结算状态
用于财务报表单独统计
```

不影响：

```text
客户支付记录
工单实收金额
库存
```

---

### 8.9 提交报销

参与对象：

```text
Reimbursement
User
Store
```

领域结果：

```text
生成报销记录
状态为待确认
暂不计入成本
```

---

### 8.10 确认报销

参与对象：

```text
Reimbursement
User
Finance 查询模型
```

领域结果：

```text
报销状态变为已确认
记录确认人和确认时间
进入财务成本统计
```

---

## 9. 领域对象状态设计

### 9.1 WorkOrder 状态

```text
DRAFT               草稿 / 未提交
PENDING_ACCEPT      待接单
ACCEPTED            已接单
PART_ORDERED        已定件
PART_ARRIVED        已到件
SETTLED             已结算
CANCELLED           已取消
```

关键状态边界：

```text
DRAFT：创建后未提交，不预占库存，可普通编辑工单明细
PENDING_ACCEPT 及之后未结算状态：已提交，库存已预占，不允许普通编辑配件明细
SETTLED：已结算，库存已正式扣减，不允许普通编辑核心金额和配件消耗
CANCELLED：已取消，预占库存已释放
```

MVP 可在页面展示上适度简化，但后端必须保留：

```text
草稿
已提交未结算
已结算
已取消
```

这四个业务边界。

---

### 9.2 InventoryFlow 类型

```text
INBOUND   入库
RESERVE   预占
RELEASE   释放
CONSUME   消耗
ADJUST    调整
```

---

### 9.3 Part 来源

```text
OFFICIAL      官方配件
THIRD_PARTY   第三方配件
```

---

### 9.4 Part 创建来源

```text
NORMAL           正常创建
INBOUND          入库创建
WORK_ORDER_TEMP  工单临时创建
```

---

### 9.5 Payment 方式

默认：

```text
WECHAT
ALIPAY
UNIONPAY
CASH
OTHER
```

具体展示名通过字典维护。

---

### 9.6 OfficialAfterSales 官方结算状态

建议：

```text
NOT_REQUIRED  无需结算
PENDING       未结算
SETTLED       已结算
```

数据库设计阶段可进一步确认。MVP 先作为 OfficialAfterSales 的字段，不单独拆 OfficialSettlement 表。

---

### 9.7 Reimbursement 状态

```text
PENDING                待确认
CONFIRMED              已确认
REJECTED_OR_CANCELLED  已驳回/取消
```

---

## 10. 不应混淆的对象

### 10.1 Part 和 InventoryStock 不能混

错误理解：

```text
配件 = 库存
```

正确理解：

```text
Part 是配件主数据
InventoryStock 是某门店某配件的库存状态
```

---

### 10.2 PaymentRecord 和 OfficialAfterSales 不能混

错误理解：

```text
官方结算金额也是客户支付
```

正确理解：

```text
PaymentRecord 是客户支付
OfficialAfterSales 是官方结算
二者分开统计
```

---

### 10.3 WorkOrderChargeItem 中 PART / LABOR / OTHER 语义不能混

错误理解：

```text
工单费用项目明细中的所有项目都是配件费
工单费用项目明细中的所有项目都影响库存
```

正确理解：

```text
charge_type = PART：配件费，关联 part_id，影响库存，参与配件收入和配件成本统计
charge_type = LABOR：工时费，不关联 part_id，不影响库存，参与人工费收入统计
charge_type = OTHER：其他费用，不关联 part_id，不影响库存，参与其他收入统计
```

---

### 10.4 Reimbursement 和 PaymentRecord 不能混

错误理解：

```text
报销也是付款记录
```

正确理解：

```text
PaymentRecord 是客户给门店的钱
Reimbursement 是门店运营支出
```

---

### 10.5 FinanceReport 和业务明细不能混

错误理解：

```text
财务报表是一张可以手动改的利润表
```

正确理解：

```text
财务报表来自业务明细汇总
利润不能随便手动改
```

---

## 11. 建议的模块映射

后端模块建议：

```text
auth              登录、微信绑定、token
user              员工、角色、权限
dict              字典配置
customer          客户、车辆
part              配件、条码
inventory         库存、库存流水
workorder         工单、工单费用项目明细、状态日志
payment           支付、退款
official          官方售后、官方结算
reimbursement     报销台账
finance           财务统计
export            Excel 导出
common            通用异常、审计字段、分页、工具类
```

说明：

```text
模块名可以在架构文档中最终确定。
但领域边界应尽量保持稳定。
```

---

## 12. 应用服务协作建议

某些业务动作会跨多个领域，不能只放在单一 CRUD Service 中。

### 12.1 SubmitWorkOrderService / 提交工单应用服务

协调：

```text
WorkOrder
Inventory
WorkOrderStatusLog
```

职责：

```text
校验工单
校验库存
预占库存
生成流水
更新状态
```

---

### 12.2 AdjustWorkOrderChargeItemsService / 调整工单费用项目应用服务

协调：

```text
WorkOrder
WorkOrderChargeItem
Inventory
InventoryFlow
```

职责：

```text
校验工单已提交且未结算、未取消
对 charge_type = PART 且 inventory_affecting = 1 的明细计算差异
释放减少部分的预占库存
预占增加部分的库存
生成对应库存流水
对 charge_type = LABOR / OTHER 的明细只更新明细和 receivable_amount
更新工单费用项目明细
重新计算 receivable_amount
记录调整原因
```

说明：

```text
这是已提交工单修改费用明细的唯一安全入口。
普通更新接口不得直接修改已提交工单的费用明细。
```

---

### 12.3 CancelWorkOrderService / 取消工单应用服务

协调：

```text
WorkOrder
Inventory
Refund 可选
WorkOrderStatusLog
```

职责：

```text
校验状态
释放库存
生成流水
更新状态
记录取消原因
```

---

### 12.4 SettleWorkOrderService / 结算工单应用服务

协调：

```text
WorkOrder
Payment
Refund
Inventory
Finance 查询依据
WorkOrderStatusLog
```

职责：

```text
计算应收
计算实收
校验结算条件
扣减库存
生成流水
更新结算状态
```

---

### 12.5 RecordPaymentService / 记录支付服务

协调：

```text
WorkOrder
PaymentRecord
User
```

职责：

```text
记录支付明细
供结算时汇总
```

---

### 12.6 RecordRefundService / 记录退款服务

协调：

```text
WorkOrder
PaymentRecord
RefundRecord
```

职责：

```text
校验可退款金额
记录退款明细
影响实收金额
```

---

### 12.7 ConfirmReimbursementService / 确认报销服务

协调：

```text
Reimbursement
User
Finance 查询依据
```

职责：

```text
确认报销
记录确认人和时间
使该报销进入成本统计
```

---

## 13. MVP 暂不建模或弱建模对象

### 13.1 客户端小程序用户

MVP 不做客户端小程序，因此不建模客户登录用户。

只记录客户信息即可。

---

### 13.2 员工提成

MVP 不做员工提成，不建模提成规则。

---

### 13.3 图片凭证

MVP 不做图片上传，不建模图片附件。

后续可扩展：

```text
Attachment
Voucher
```

---

### 13.4 自动采购

MVP 不做自动采购，不建模采购单、供应商、采购审批。

后续可扩展：

```text
Supplier
PurchaseOrder
PurchaseOrderItem
```

---

### 13.5 门店调拨

MVP 不做多门店调拨，不建模调拨单。

后续可扩展：

```text
TransferOrder
TransferOrderItem
```

---

### 13.6 复杂盘点

MVP 不做盘点单和盘点任务。

只做库存调整。

后续可扩展：

```text
StocktakeOrder
StocktakeItem
```

---

### 13.7 反结算 / 售后退货

MVP 不做自动反结算和自动退货回库。

后续可扩展：

```text
AfterSaleReturn
ReverseSettlement
```

---

## 14. 后续数据库设计重点

Step 5 数据库设计需要重点处理：

```text
1. WorkOrder 与 WorkOrderChargeItem 的字段设计，尤其是 charge_type / DRAFT / 提交时间 / 提交人
2. InventoryStock 与 InventoryFlow 的一致性，InventoryFlow 应能关联 InventoryStock
3. PaymentRecord 与 RefundRecord 的明细结构
4. OfficialAfterSales 独立表及官方结算字段，MVP 不拆 OfficialSettlement
5. Reimbursement 状态流转字段
6. WorkOrderChargeItem 中 PART 类型的成本价快照
7. 已提交工单调整费用项目时的预占差异处理（仅 PART 类型）
8. 所有业务表 store_id 预留
9. 审计字段 created_by / created_at / updated_by / updated_at
10. 软删除策略
11. 状态字段编码
12. 金额字段 BigDecimal / DECIMAL 精度
13. 索引和唯一约束
14. ExportRecord 是否暂不建表，先由同步导出接口完成
```

---

## 15. Codex 实现提示

后续 Codex 实现时，应注意：

```text
1. 不要把领域对象简单等同于数据库表。
2. 不要把所有逻辑写成普通 CRUD。
3. 提交、取消、结算、退款、报销确认必须是业务动作。
4. 库存变化必须由 inventory 模块统一处理。
5. 支付和退款不能塞进工单单字段。
6. 官方结算不能写入 PaymentRecord。
7. 财务统计从明细汇总，不要建可手工修改利润的表。
8. 领域服务 / 应用服务要承担跨领域事务协调。
9. 不要为 InventoryOperation、OfficialSettlement、ExportTask 凭空创建 MVP 核心表。
10. 工单 DRAFT 与提交动作必须分离，提交才允许预占库存。
11. 不得把 LABOR / OTHER 明细纳入库存预占/扣减。
12. 不得再使用 labor_fee / other_fee 作为工单应收金额的主要来源。
```

---

## 16. Claude 实现提示

后续 Claude 执行任务时，应注意：

```text
1. 可以做页面、表单、列表、测试、文档。
2. 不得擅自修改本文档定义的核心领域关系。
3. 不得把库存、支付、结算逻辑简化成前端字段。
4. 不得通过页面 update 直接修改工单核心状态。
5. 发现领域模型不匹配时，应报告，不要自行重构。
6. 不得通过前端普通编辑接口修改已提交工单的费用明细。
7. 不得在页面中绕过 charge_type，不能把所有维修项目默认当 PART。
```

---

## 17. 当前领域模型状态

本文档状态：

```text
Reviewed v0.3
已将 WorkOrderItem 升级为 WorkOrderChargeItem
已明确 charge_type = PART / LABOR / OTHER 语义
已更新对象关系、核心动作和应用服务建议
可进入 Step 5：05_DATABASE_DESIGN.md
```

本次已解决的问题：

```text
1. WorkOrderItem 替换为 WorkOrderChargeItem，统一承载配件费、工时费、其他费用。
2. 明确 charge_type = PART 且 inventory_affecting = 1 的明细才影响库存。
3. 明确 charge_type = LABOR / OTHER 不影响库存，只影响应收金额和财务分类。
4. labor_fee / other_fee 不再作为工单金额的主要来源字段。
5. receivable_amount 由 work_order_charge_item.line_amount 汇总得到。
6. 工单提交、取消、结算只处理 PART 类型明细。
7. 已提交工单调整费用项目时，PART 类型同步库存，LABOR / OTHER 只更新明细。
8. 更新了对象关系图、聚合设计、核心动作和不应混淆的对象章节。
9. Codex 和 Claude 提示中增加 charge_type 相关护栏。
```

下一步进入：

```text
05_DATABASE_DESIGN.md
```
