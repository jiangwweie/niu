# 07_IMPLEMENTATION_ROADMAP.md

# 小牛电动官方授权店两轮车维修售后库存管理系统 - 实施路线与任务拆解文档

版本：v0.2  
日期：2026-05-10  
阶段：项目准备阶段 Step 7 / 8  
修订依据：工单费用项目明细模型升级 2026-05-10  
前置文档：

- `01_MVP_SCOPE.md`
- `02_BUSINESS_FLOWS.md`
- `03_CORE_BUSINESS_RULES.md`
- `04_DOMAIN_MODEL.md` v0.3
- `05_DATABASE_DESIGN.md` v0.3
- `06_ARCHITECTURE_AND_API_STYLE.md` v0.2

用途：用于明确 MVP 从项目初始化到真实门店试运行的实施顺序、阶段目标、任务边界、Codex / Claude 分工、验收关口和风险控制方式。

---

## 1. 文档目的

本文档用于回答：

1. 项目正式编码后按什么顺序推进？
2. 哪些任务先交给 Codex，哪些任务交给 Claude？
3. 哪些模块是高风险核心模块？
4. 每个阶段的产出物是什么？
5. 每个阶段的完成标准是什么？
6. 哪些节点必须暂停复核？
7. 哪些任务不能并行？
8. 什么时候可以进入小程序和管理端开发？
9. 什么时候可以进入真实门店试运行？
10. 后续如何避免 AI 写快但写散？

本文档不是代码任务卡全集。  
本文档是路线图。每个阶段真正执行前，还需要由 ChatGPT 生成对应 Codex / Claude 任务卡。

---

## 2. 总体实施原则

### 2.1 先后端闭环，再前端体验

本项目核心复杂度在后端：

```text
工单状态
库存预占 / 释放 / 扣减
库存流水
支付退款
官方结算
报销入账
财务统计
```

因此实施顺序必须是：

```text
先数据库
再后端核心闭环
再管理端
再小程序体验优化
最后真实试运行
```

禁止一开始先做大量页面，然后反过来返工后端模型。

---

### 2.2 先主链路，后边缘能力

优先跑通主链路：

```text
配件入库
  ↓
创建 DRAFT 工单
  ↓
提交工单并预占库存
  ↓
记录客户付款
  ↓
结算工单并扣减库存
  ↓
查看财务报表
```

主链路稳定后，再补：

```text
退款
官方结算
报销
Excel 导出
中间状态推进
已提交工单调整配件
小程序体验优化
```

---

### 2.3 Codex 做核心，Claude 做低风险

Codex 负责：

```text
数据库 migration
后端项目骨架
权限模型
库存模块
工单状态机
支付退款
官方结算
报销确认
财务统计
事务一致性
核心测试
```

Claude 负责：

```text
文档同步
简单页面
表单
列表
字段展示
前端校验
接口测试
单元测试补充
低风险 bug 修复
```

Claude 不直接修改：

```text
库存预占
库存扣减
工单状态机
支付退款结算
官方结算统计
财务利润口径
数据库核心结构
```

---

### 2.4 每个阶段必须有验收

每个阶段至少要有：

```text
1. Codex 输出变更摘要
2. 本地运行结果
3. 自动测试或手工验收清单
4. ChatGPT / Opus 复核，视风险决定
5. Owner 确认进入下一阶段
```

不要让多个高风险模块同时推进。

---

## 3. 实施阶段总览

推荐阶段：

```text
Phase 0：方案冻结与仓库准备
Phase 1：项目骨架、基础设施、登录权限、字典
Phase 2：配件与库存
Phase 3：工单主流程
Phase 4：支付、退款与结算
Phase 5：官方售后
Phase 6：报销台账
Phase 7：财务报表与 Excel 导出
Phase 8：管理端 MVP
Phase 9：微信小程序 MVP
Phase 10：集成测试与真实门店试运行
```

说明：

```text
Phase 8 和 Phase 9 可以在后端核心接口稳定后部分并行。
但 Phase 2~4 不建议并行，必须按顺序打牢。
```

---

## 4. 阶段依赖关系

```text
Phase 0
  ↓
Phase 1
  ↓
Phase 2 配件库存
  ↓
Phase 3 工单主流程
  ↓
Phase 4 支付退款结算
  ↓
Phase 5 官方售后
  ↓
Phase 6 报销
  ↓
Phase 7 财务导出
  ↓
Phase 8 管理端
  ↓
Phase 9 小程序
  ↓
Phase 10 试运行
```

更准确的依赖：

```text
管理端配件/库存页面 依赖 Phase 2
管理端工单页面 依赖 Phase 3
管理端支付退款页面 依赖 Phase 4
管理端官方结算页面 依赖 Phase 5
管理端报销页面 依赖 Phase 6
管理端财务页面 依赖 Phase 7
小程序创建工单 依赖 Phase 3
小程序客户结算记录 依赖 Phase 4
小程序报销提交 依赖 Phase 6
```

---

# Phase 0：方案冻结与仓库准备

## 目标

完成正式编码前的基础准备，确保仓库、文档、AI 规范和分支策略可用。

---

## 输入文档

```text
README.md
AGENTS.md
CODEX.md
CLAUDE.md
01_MVP_SCOPE.md
02_BUSINESS_FLOWS.md
03_CORE_BUSINESS_RULES.md
04_DOMAIN_MODEL.md
05_DATABASE_DESIGN.md
06_ARCHITECTURE_AND_API_STYLE.md
```

---

## 主要任务

### 0.1 建立项目文档目录

建议目录：

```text
docs/
├── 01_MVP_SCOPE.md
├── 02_BUSINESS_FLOWS.md
├── 03_CORE_BUSINESS_RULES.md
├── 04_DOMAIN_MODEL.md
├── 05_DATABASE_DESIGN.md
├── 06_ARCHITECTURE_AND_API_STYLE.md
├── 07_IMPLEMENTATION_ROADMAP.md
├── 08_AI_COLLABORATION_AND_ACCEPTANCE.md
├── task-cards/
└── reviews/
```

执行 Agent：

```text
Claude 或 Owner
```

风险：

```text
LOW
```

---

### 0.2 放置 AI 规范文档

必须放置：

```text
README.md
AGENTS.md
CODEX.md
CLAUDE.md
```

执行 Agent：

```text
Owner / Claude
```

完成标准：

```text
Codex 和 Claude 每次任务前能读取对应规范
```

---

### 0.3 确认分支策略

建议：

```text
main        稳定版本
dev         日常集成
feature/*   单任务分支
```

规则：

```text
高风险任务必须走 feature 分支
通过验收后合并 dev
阶段稳定后再合并 main 或打 tag
```

执行 Agent：

```text
Owner
```

---

## 阶段完成标准

```text
1. 所有准备文档在仓库中。
2. README / AGENTS / CODEX / CLAUDE 可用。
3. 分支策略确认。
4. 进入 Phase 1 前，Owner 确认不再改大方向。
```

---

# Phase 1：项目骨架、基础设施、登录权限、字典

## 目标

建立可运行的后端基础骨架和基础管理能力。

---

## 主要产出

```text
Spring Boot 项目骨架
MySQL 连接
数据库 migration 初版
统一返回结构
统一异常处理
错误码体系
分页结构
登录认证
当前用户上下文
角色权限
字典配置
sequence_daily 编号服务
```

---

## Codex 任务

### 1.1 创建 Spring Boot 后端骨架

执行 Agent：

```text
Codex
```

任务内容：

```text
1. 创建 Spring Boot 3 项目结构。
2. 配置 MySQL 连接。
3. 配置 MyBatis / MyBatis-Plus。
4. 建立 common、auth、user、dict 等基础模块。
5. 建立统一返回和统一异常。
6. 建立基础分页对象。
```

不做范围：

```text
不实现库存
不实现工单
不实现支付
不实现财务
不引入 Redis / MQ / 微服务
```

验收标准：

```text
项目能启动
健康检查接口可访问
数据库连接成功
统一返回格式可用
```

风险：

```text
MEDIUM
```

---

### 1.2 生成数据库 migration 初版

执行 Agent：

```text
Codex
```

任务内容：

```text
1. 基于 05_DATABASE_DESIGN.md v0.3 生成 MySQL migration。
2. 创建所有 MVP 表。
3. 创建 sequence_daily 表。
4. 创建必要索引。
5. 创建初始化数据 SQL。
```

必须包含：

```text
store
sys_user
sys_role
sys_permission
sys_user_role
sys_role_permission
sequence_daily
sys_dict_type
sys_dict_item
customer
vehicle
part
part_barcode
inventory_stock
inventory_flow
work_order
work_order_charge_item
work_order_status_log
payment_record
refund_record
official_after_sales
reimbursement
```

不做：

```text
inventory_operation
official_settlement_record
export_task
purchase_order
transfer_order
stocktake_order
attachment
commission_rule
reverse_settlement
```

验收标准：

```text
migration 可在空库执行成功
初始化默认门店、角色、权限、字典成功
表结构与数据库设计文档一致
```

风险：

```text
HIGH
```

必须复核：

```text
ChatGPT / Opus
Owner
```

---

### 1.3 实现 sequence_daily 编号服务

执行 Agent：

```text
Codex
```

任务内容：

```text
1. 实现 SequenceService.next(seqType)。
2. 使用 sequence_daily 表。
3. 支持工单、支付、退款、报销、第三方配件编码。
4. 禁止 MAX + 1。
5. 禁止内存计数。
```

验收标准：

```text
同一天同类型编号递增
不同类型互不影响
重启服务后编号继续递增
并发调用不重复
```

风险：

```text
HIGH
```

---

### 1.4 实现登录与权限基础

执行 Agent：

```text
Codex
```

任务内容：

```text
1. 手机号登录。
2. 微信登录占位或基础实现。
3. 微信绑定手机号。
4. 当前用户上下文。
5. 权限点校验。
6. 用户创建时 phone / username / wechat_openid 至少一个登录标识校验。
```

验收标准：

```text
用户可登录
接口能识别当前用户
接口能识别 store_id
接口能判断权限点
停用用户不能登录
```

风险：

```text
HIGH
```

---

### 1.5 实现字典配置

执行 Agent：

```text
Codex
```

任务内容：

```text
1. 字典类型管理。
2. 字典项管理。
3. 默认字典初始化。
4. 字典启停。
```

注意：

```text
工单状态字典只做展示。
状态流转由后端枚举和状态机控制。
```

风险：

```text
MEDIUM
```

---

## Claude 任务

```text
1. 补充 README 启动说明。
2. 补充基础接口测试。
3. 补充默认字典文档。
4. 管理端登录页雏形，若前端已初始化。
```

风险：

```text
LOW
```

---

## Phase 1 完成标准

```text
1. 后端项目可启动。
2. migration 可执行。
3. 默认数据可初始化。
4. 登录和权限基础可用。
5. sequence_daily 可生成稳定业务编号。
6. 字典配置可用。
7. Owner 可以本地跑通基础服务。
```

---

# Phase 2：配件与库存

## 目标

建立配件主数据、条码、库存快照、库存流水和库存基础动作。

这是第一个高风险核心模块。

---

## 主要产出

```text
配件管理
第三方配件编码生成
配件条码
正常入库
临时入库能力
库存查询
库存流水查询
库存调整
库存快照和流水一致性
```

---

## Codex 任务

### 2.1 实现配件主数据

任务内容：

```text
1. part CRUD。
2. part_barcode 管理。
3. 官方配件 official_part_no。
4. 第三方配件系统编码。
5. 配件启停。
6. 条码查询配件。
```

不做：

```text
不做价格策略
不做自动采购
不做供应商
```

验收标准：

```text
官方配件可创建
第三方配件自动生成编码
配件可停用
条码可查询
停用配件不能被新工单选择
```

风险：

```text
MEDIUM
```

---

### 2.2 实现正常入库

任务内容：

```text
1. POST /api/inventory/inbound。
2. 入库时增加 actual_qty。
3. 入库时增加 available_qty。
4. reserved_qty 不变。
5. 写 inventory_flow，flow_type = INBOUND。
6. 支持扫码找不到时新增配件并入库。
```

事务要求：

```text
part 可选创建
inventory_stock 更新
inventory_flow 写入
必须在同一事务中
```

验收标准：

```text
新配件入库后库存增加
已有配件入库后库存累加
每次入库都有流水
库存 before / after 正确
```

风险：

```text
HIGH
```

---

### 2.3 实现库存查询与流水查询

任务内容：

```text
1. 查询库存快照。
2. 按配件名称、编码、条码查询。
3. 查询库存流水。
4. 按时间、配件、流水类型过滤。
```

风险：

```text
MEDIUM
```

---

### 2.4 实现库存调整

任务内容：

```text
1. POST /api/inventory/adjust。
2. 授权账号才可调整。
3. 必须填写调整原因。
4. 更新 inventory_stock。
5. 写 inventory_flow，flow_type = ADJUST。
```

验收标准：

```text
调整后库存正确
调整原因保存
流水完整
无权限不能调整
```

风险：

```text
HIGH
```

---

## Claude 任务

```text
1. 配件列表页面。
2. 配件表单。
3. 库存查询页面。
4. 库存流水页面。
5. 入库表单基础 UI。
6. 库存模块接口测试。
```

注意：

```text
Claude 不修改库存服务核心逻辑。
```

---

## Phase 2 完成标准

```text
1. 配件可创建、查询、停用。
2. 第三方配件编码可生成。
3. 条码可查询配件。
4. 正常入库可用。
5. 库存快照正确。
6. 库存流水完整。
7. 库存调整可用且受权限控制。
```

必须通过主测试：

```text
入库 10 个配件
actual_qty = 10
available_qty = 10
reserved_qty = 0
inventory_flow 有 INBOUND 流水
```

---

# Phase 3：工单主流程

## 目标

建立工单 DRAFT、提交预占库存、取消释放库存、工单状态日志、已提交工单配件调整和中间状态推进能力。

这是第二个高风险核心模块。

---

## 主要产出

```text
创建 DRAFT 工单
编辑 DRAFT 工单
DRAFT 阶段添加/修改/删除 charge items（配件费、工时费、其他费用）
DRAFT 删除 charge item 不影响库存
提交工单只对 PART 类型明细预占库存
取消工单只对 PART 类型明细释放库存
已提交工单调整费用项目并同步 PART 类型预占差异
工单中间状态推进
工单状态日志
车架号历史维修查询
```

---

## Codex 任务

### 3.1 创建 DRAFT 工单

任务内容：

```text
1. POST /api/work-orders。
2. 创建状态为 DRAFT 的工单。
3. 保存客户和车辆信息。
4. 保存客户和车辆快照。
5. 生成工单编号。
6. 不预占库存。
7. 支持同时创建多个 charge items（PART / LABOR / OTHER）。
```

验收标准：

```text
创建工单后 status = DRAFT
库存无变化
无 RESERVE 流水
```

风险：

```text
HIGH
```

---

### 3.2 编辑 DRAFT 工单和 charge items

任务内容：

```text
1. PUT /api/work-orders/{id} 仅允许 DRAFT。
2. DRAFT 阶段可添加/修改/删除 work_order_charge_item。
3. DRAFT 阶段支持添加三类 charge items：配件费、工时费、其他费用。
4. DRAFT 删除 charge item 按草稿明细物理删除或直接移除记录处理。
5. DRAFT 删除 charge item 不生成库存流水。
6. 重新计算 receivable_amount = Σ work_order_charge_item.line_amount。
```

说明：

```text
复核建议已吸收：DRAFT 阶段 remove charge item 是草稿明细删除，不是业务状态删除。
已提交后不得走此接口修改 charge items。
```

验收标准：

```text
DRAFT 工单可编辑
DRAFT 工单添加 LABOR 明细不影响库存
DRAFT 工单添加 OTHER 明细不影响库存
DRAFT 工单添加 PART 明细不影响库存
DRAFT 工单删除明细不影响库存
已提交工单不能通过普通编辑改 charge items
```

风险：

```text
HIGH
```

---

### 3.3 提交工单并预占库存

任务内容：

```text
1. POST /api/work-orders/{id}/submit。
2. 校验状态必须为 DRAFT。
3. 遍历 work_order_charge_item，只处理 charge_type = PART 且 inventory_affecting = 1 的明细。
4. 校验 PART 类型明细对应配件可用库存。
5. 预占库存。
6. 写 RESERVE 流水。
7. 工单状态变为 PENDING_ACCEPT。
8. 写 submitted_by / submitted_at。
9. 写 work_order_status_log。
```

验收标准：

```text
提交后 PART 类型明细对应 available_qty 减少
提交后 PART 类型明细对应 reserved_qty 增加
actual_qty 不变
LABOR 明细不影响库存
OTHER 明细不影响库存
PART 明细提交后预占库存
生成 RESERVE 流水
DRAFT 不能重复提交
```

风险：

```text
HIGH
```

必须复核：

```text
ChatGPT / Opus
Owner
```

---

### 3.4 工单中间状态推进

任务内容：

```text
补充中间状态推进 API：

POST /api/work-orders/{id}/accept
POST /api/work-orders/{id}/mark-part-ordered
POST /api/work-orders/{id}/mark-part-arrived
```

状态流转：

```text
PENDING_ACCEPT → ACCEPTED
ACCEPTED → PART_ORDERED
PART_ORDERED → PART_ARRIVED
```

规则：

```text
1. 只修改工单状态和状态日志。
2. 不影响库存。
3. 不影响支付。
4. 不影响结算。
5. 必须校验前置状态。
```

说明：

```text
复核建议已吸收：06 文档只有 submit/cancel/settle，实施阶段补入中间状态推进任务。
```

风险：

```text
MEDIUM
```

---

### 3.5 取消工单并释放库存

任务内容：

```text
1. POST /api/work-orders/{id}/cancel。
2. 校验工单未结算、未取消。
3. 如果已提交并有预占库存，只释放 charge_type = PART 且 inventory_affecting = 1 的明细对应预占。
4. 写 RELEASE 流水。
5. 工单状态变为 CANCELLED。
6. 记录取消原因。
7. 写状态日志。
```

验收标准：

```text
取消后 PART 类型明细对应 reserved_qty 减少
取消后 PART 类型明细对应 available_qty 增加
actual_qty 不变
LABOR / OTHER 类型明细不影响库存
生成 RELEASE 流水
重复取消不会重复释放
```

风险：

```text
HIGH
```

---

### 3.6 调整已提交工单费用项目

任务内容：

```text
1. POST /api/work-orders/{id}/adjust-charge-items。
2. 仅允许已提交、未结算、未取消工单。
3. 计算新旧费用项目差异。
4. PART 类型费用项目（inventory_affecting = 1）：
   - 减少或删除：释放预占，写 RELEASE 流水。
   - 增加或新增：校验可用库存，预占差量，写 RESERVE 流水。
   - 库存不足时整体回滚。
5. LABOR / OTHER 类型费用项目：
   - 仅更新费用项目金额和明细。
   - 不影响库存，不生成库存流水。
6. 更新 work_order_charge_item。
7. 重算 receivable_amount = Σ work_order_charge_item.line_amount。
8. 记录调整原因。
```

验收标准：

```text
PART 类型减少数量能释放预占
PART 类型增加数量能新增预占
PART 类型库存不足时调整失败且整体回滚
LABOR 类型调整不影响库存
OTHER 类型调整不影响库存
工单明细和库存预占保持一致
已结算工单不能调整
receivable_amount 正确重算
```

风险：

```text
HIGH
```

默认策略：

```text
Phase 3 默认不实现 adjust-charge-items。
已提交工单如需调整费用项目，走"取消 → 重建"流程。
```

追加条件：

```text
Phase 3 主测试通过后，Owner 评估是否在 Phase 4 之前追加 adjust-charge-items。
如果追加，必须严格按事务处理，不允许简化库存预占差异逻辑。
如果不追加，Phase 10 试运行阶段根据门店反馈再决定。
```

---

## Claude 任务

```text
1. 工单列表页面。
2. DRAFT 工单表单（需支持添加三类费用项目：配件费 PART、工时费 LABOR、其他费用 OTHER）。
3. 工单详情页面（展示 charge_type 分类和各类费用小计）。
4. 状态标签展示。
5. 车架号历史工单查询页面。
6. 工单相关接口测试。
```

注意：

```text
Claude 不实现 submit/cancel/settle/adjust-charge-items 核心逻辑。
```

---

## Phase 3 完成标准

```text
1. DRAFT 工单可创建，支持三类费用项目（PART / LABOR / OTHER）。
2. DRAFT 可编辑和删除费用项目。
3. DRAFT 阶段添加/修改/删除三类费用项目均不影响库存。
4. 提交工单只对 PART 类型且 inventory_affecting = 1 的费用项目预占库存。
5. 取消工单只对 PART 类型且 inventory_affecting = 1 的费用项目释放库存。
6. 中间状态推进可用。
7. 工单状态日志完整。
8. 已提交工单不能通过普通编辑破坏库存一致性。
```

必须通过主测试：

```text
入库 10 个后减震器（PART）
创建 DRAFT 工单，添加：
  - 后减震器 x2（PART，inventory_affecting=1）¥350
  - 前面板喷漆（LABOR，inventory_affecting=0）¥800
  - 检测费（OTHER，inventory_affecting=0）¥120
库存不变：actual=10, available=10, reserved=0
提交工单
actual_qty = 10
available_qty = 8
reserved_qty = 2
取消工单
actual_qty = 10
available_qty = 10
reserved_qty = 0
```

---

# Phase 4：支付、退款与结算

## 目标

建立客户付款、退款、实收金额计算和工单结算扣减库存闭环。

这是第三个高风险核心模块。

---

## 主要产出

```text
支付记录
多次付款
混合付款
退款记录
可退款金额校验
实收金额计算
实收不足禁止结算
结算工单并扣减库存
```

---

## Codex 任务

### 4.1 记录支付

任务内容：

```text
1. POST /api/payments。
2. 创建 payment_record。
3. 支持多次付款。
4. 支持混合付款。
5. 支付编号由 sequence_daily 生成。
```

验收标准：

```text
一个工单多笔支付可累计
不同支付方式可混合
支付不自动结算工单
```

风险：

```text
HIGH
```

---

### 4.2 记录退款

任务内容：

```text
1. POST /api/refunds。
2. 创建 refund_record。
3. 退款金额不能超过支付总额 - 已退款总额。
4. 退款编号由 sequence_daily 生成。
5. 退款必须填写原因。
```

验收标准：

```text
退款会降低实收金额
退款不删除原支付
退款超额失败
已结算后退款不回滚库存和状态
```

风险：

```text
HIGH
```

---

### 4.3 结算工单

任务内容：

```text
1. POST /api/work-orders/{id}/settle。
2. 计算应收金额 = Σ work_order_charge_item.line_amount。
3. 计算实收金额 = Σ payment_record.paid_amount - Σ refund_record.refund_amount。
4. 校验实收金额 >= 应收金额。
5. 只对 PART 类型且 inventory_affecting = 1 的费用项目扣减库存。
6. 写 CONSUME 流水。
7. 工单状态变为 SETTLED。
8. 写 settled_by / settled_at。
9. 写状态日志。
```

验收标准：

```text
实收不足不能结算
实收达到应收可以结算
结算后 reserved_qty 减少
结算后 actual_qty 减少
available_qty 不变
重复结算不会重复扣库存
```

风险：

```text
HIGH
```

必须复核：

```text
ChatGPT / Opus
Owner
```

---

## Claude 任务

```text
1. 支付记录页面。
2. 退款记录页面。
3. 工单详情展示支付/退款汇总。
4. 支付表单校验。
5. 退款表单校验。
6. 支付退款接口测试。
```

---

## Phase 4 完成标准

```text
1. 多次付款可用。
2. 混合付款可用。
3. 退款记录可用。
4. 实收金额计算正确。
5. 实收不足不能结算。
6. 结算工单扣减库存。
7. 支付、退款、结算都有测试。
```

主链路测试：

```text
入库 10
创建工单使用 2
提交后 available=8 reserved=2 actual=10
应收 500
付款 200，不能结算
付款 300，可以结算
结算后 actual=8 available=8 reserved=0
退款 100 后，财务实收减少，但工单仍 SETTLED，库存不回滚
```

---

# Phase 5：官方售后

## 目标

实现官方售后订单号和官方结算金额手动录入，确保官方结算和客户支付分离。

---

## Codex 任务

### 5.1 官方售后信息维护

任务内容：

```text
1. 创建或更新 official_after_sales。
2. 支持是否官方售后。
3. 支持官方售后订单号。
4. 支持官方结算状态。
```

风险：

```text
MEDIUM
```

---

### 5.2 官方结算录入

任务内容：

```text
1. POST /api/work-orders/{id}/official-after-sales/settle。
2. 录入 official_settlement_amount。
3. 设置 official_settlement_status = SETTLED。
4. 记录 official_settlement_time。
5. 记录 official_settlement_operator_id。
```

规则：

```text
不得写 payment_record
不得影响工单实收金额
不得影响库存
```

验收标准：

```text
官方结算收入可在财务报表单独统计
客户支付收入不受官方结算影响
```

风险：

```text
HIGH
```

---

## Claude 任务

```text
1. 官方售后信息展示。
2. 官方结算录入表单。
3. 官方结算列表筛选。
4. 官方结算相关测试。
```

---

## Phase 5 完成标准

```text
1. 官方订单号可维护。
2. 官方结算金额可手动录入。
3. 官方结算和客户支付分开。
4. 财务可统计官方结算收入。
```

---

# Phase 6：报销台账

## 目标

实现员工报销提交、老板确认、驳回/取消和报销成本统计依据。

---

## Codex 任务

### 6.1 报销提交

任务内容：

```text
1. POST /api/reimbursements。
2. 报销编号由 sequence_daily 生成。
3. 状态为 PENDING。
4. 不计入成本。
```

风险：

```text
MEDIUM
```

---

### 6.2 报销确认

任务内容：

```text
1. POST /api/reimbursements/{id}/confirm。
2. 仅 PENDING 可确认。
3. 记录 confirmed_amount。
4. 记录 confirmed_by / confirmed_at。
5. 状态变 CONFIRMED。
```

规则：

```text
只有 CONFIRMED 进入财务成本。
```

风险：

```text
HIGH
```

---

### 6.3 报销驳回/取消

任务内容：

```text
1. POST /api/reimbursements/{id}/reject。
2. POST /api/reimbursements/{id}/cancel。
3. 记录原因。
4. 不计入成本。
```

风险：

```text
MEDIUM
```

---

## Claude 任务

```text
1. 小程序报销提交页面。
2. 管理端报销列表。
3. 管理端报销确认表单。
4. 报销测试用例。
```

---

## Phase 6 完成标准

```text
1. 员工可提交报销。
2. 老板可确认报销。
3. 报销驳回/取消可用。
4. 只有确认报销计入成本。
```

---

# Phase 7：财务报表与 Excel 导出

## 目标

实现日报、月报、时间范围查询、收入成本利润统计和 Excel 导出。

---

## Codex 任务

### 7.1 财务汇总接口

任务内容：

```text
1. GET /api/finance/daily。
2. GET /api/finance/monthly。
3. GET /api/finance/range。
4. 汇总客户支付净收入。
5. 汇总官方结算收入。
6. 汇总配件销售收入 = Σ work_order_charge_item.line_amount WHERE charge_type = PART。
7. 汇总人工费收入 = Σ work_order_charge_item.line_amount WHERE charge_type = LABOR。
8. 汇总其他收入 = Σ work_order_charge_item.line_amount WHERE charge_type = OTHER。
9. 汇总配件成本 = Σ work_order_charge_item.line_cost_amount WHERE charge_type = PART。
10. 汇总报销成本（CONFIRMED 状态）。
11. 计算利润 = 收入 - 成本。
```

时间口径：

```text
客户支付净收入：paid_at / refunded_at
工单结算收入和配件成本：settled_at
官方结算收入：official_settlement_time
报销成本：confirmed_at
```

风险：

```text
HIGH
```

---

### 7.2 Excel 导出

任务内容：

```text
1. 工单列表导出。
2. 库存报表导出。
3. 库存流水导出。
4. 财务报表导出。
5. 利润报表导出。
6. 报销台账导出。
```

规则：

```text
导出必须受权限控制
导出结果与查询条件一致
MVP 同步导出
不建 export_task
```

风险：

```text
MEDIUM
```

---

## Claude 任务

```text
1. 财务报表页面。
2. 时间范围选择器。
3. Excel 导出按钮。
4. 导出权限和前端提示。
5. 财务接口测试。
```

---

## Phase 7 完成标准

```text
1. 日报可查看。
2. 月报可查看。
3. 时间范围报表可查看。
4. 收入、成本、利润口径正确。
5. 客户支付和官方结算分开。
6. Excel 导出可用。
```

---

# Phase 8：管理端 MVP

## 目标

实现老板、管理员、财务使用的后台管理界面。

---

## 页面优先级

```text
1. 登录页
2. 工作台
3. 配件管理
4. 库存管理
5. 库存流水
6. 工单管理
7. 工单详情
8. 支付退款查看
9. 官方结算录入
10. 报销确认
11. 财务报表
12. Excel 导出
13. 用户角色权限
14. 字典配置
```

---

## Claude 主要任务

```text
管理端页面
表单
列表
筛选
分页
字段展示
按钮状态
基础权限展示
```

---

## Codex 支持任务

```text
补接口缺口
修复后端 bug
补充权限接口
优化查询性能
```

---

## Phase 8 完成标准

```text
1. 老板能看工单。
2. 管理员能管配件和库存。
3. 财务能看报表和导出。
4. 老板能确认报销。
5. 官方结算可录入。
6. 权限基础可用。
```

---

# Phase 9：微信小程序 MVP

## 目标

实现员工现场高频操作入口。

---

## 页面优先级

```text
1. 登录页
2. 工作台
3. 工单列表
4. 创建 DRAFT 工单
5. 编辑 DRAFT 工单
6. 工单详情
7. 提交工单
8. 库存查询
9. 扫码 / 手动入库
10. 客户支付记录
11. 退款记录
12. 报销提交
```

---

## Claude 主要任务

```text
小程序页面
表单
扫码入口
字段校验
接口对接
状态展示
```

---

## Codex 支持任务

```text
补小程序登录
微信 openid 绑定
接口权限问题
后端 bug 修复
```

---

## Phase 9 完成标准

```text
1. 员工能登录。
2. 员工能创建工单。
3. 员工能查库存。
4. 员工能扫码或手动入库。
5. 员工能提交工单。
6. 员工能记录付款。
7. 员工能提交报销。
```

---

# Phase 10：集成测试与真实门店试运行

## 目标

从开发环境进入真实门店小范围试运行。

---

## 集成测试主链路

必须跑通：

```text
1. 创建配件。
2. 入库 10 个。
3. 创建 DRAFT 工单，使用 2 个。
4. DRAFT 阶段删除/修改配件，库存不变。
5. 提交工单，库存预占。
6. 状态推进到 ACCEPTED / PART_ORDERED / PART_ARRIVED。
7. 记录客户付款。
8. 实收不足不能结算。
9. 补足尾款。
10. 结算工单，库存扣减。
11. 记录退款，财务实收减少，库存不回滚。
12. 录入官方结算。
13. 提交并确认报销。
14. 查看财务报表。
15. 导出 Excel。
```

---

## 异常测试

必须覆盖：

```text
库存不足提交失败
重复提交不重复预占
重复取消不重复释放
重复结算不重复扣减
退款金额超过可退金额失败
已提交工单普通编辑配件失败
已结算工单调整配件失败
无权限库存调整失败
待确认报销不计入成本
官方结算不进入客户支付
```

---

## 试运行方式

建议：

```text
先只让 1~2 个员工使用
先只录真实业务的一部分
保留纸质/Excel 备份 1~2 周
每日核对库存和收款
每日记录问题清单
一周后再决定是否扩大使用
```

---

## 试运行问题分类

```text
BLOCKER：库存、支付、结算、财务错误，必须立即修复
HIGH：影响主流程效率或数据准确性，优先修复
MEDIUM：页面体验、字段缺失、查询不便，排期修复
LOW：文案、样式、小优化，后续处理
```

---

## Phase 10 完成标准

```text
1. 真实业务可录入。
2. 库存对账基本正确。
3. 支付退款可核对。
4. 财务报表口径可接受。
5. 员工能完成高频操作。
6. 老板能查看关键数据。
7. 问题清单可控，没有 BLOCKER。
```

---

# 11. 阶段评审机制

## 11.1 必须评审的节点

```text
Phase 1 数据库 migration 完成
Phase 2 库存模块完成
Phase 3 工单提交/取消完成
Phase 4 支付退款结算完成
Phase 7 财务报表完成
Phase 10 试运行前
```

---

## 11.2 评审方式

每个关键节点要求 Codex 输出：

```text
1. 修改文件
2. 数据库变更
3. 实现内容
4. 涉及业务规则
5. 测试结果
6. 风险说明
7. 未做范围
```

ChatGPT / Opus 复核：

```text
是否符合文档
是否扩大范围
是否破坏库存/支付/财务规则
是否需要阻塞
```

Owner 决定：

```text
通过
修正后通过
阻塞
```

---

# 12. 并行策略

## 12.1 不建议并行的任务

以下任务不要并行：

```text
数据库 migration
库存核心逻辑
工单状态机
支付退款结算
财务统计口径
```

原因：

```text
这些模块互相依赖，返工成本高。
```

---

## 12.2 可以并行的任务

在后端接口稳定后，可以并行：

```text
管理端页面
小程序页面
接口测试
文档同步
Excel 导出页面入口
字段展示优化
```

Phase 8 和 Phase 9 并行说明：

```text
管理端（Phase 8）和小程序（Phase 9）可以并行推进，
前提是后端 Phase 1~7 核心接口已完成且通过评审。
两个前端没有代码依赖关系。
如果 Claude 资源有限（同时只有一个 Claude session），
则按 Phase 8 → Phase 9 顺序推进，因为管理端覆盖更多管理功能。
```

---

# 13. 任务卡生成规则

每个执行任务必须使用任务卡。

任务卡必须包含：

```text
任务名称
执行 Agent
任务目标
涉及模块
涉及表
涉及接口
必须遵守的规则
不做范围
实现要求
测试要求
验收标准
输出要求
风险等级
```

高风险任务必须交给 Codex。

低风险任务可以交给 Claude。

---

# 14. 风险等级规则

## BLOCKER

必须停止推进：

```text
库存可能对不上
支付退款可能对不上
结算条件错误
官方结算混入客户支付
报销未确认就入成本
财务口径错误
数据库结构无法支撑核心流程
charge_type 判断错误导致 LABOR/OTHER 误触发库存操作
```

## HIGH

需要 Codex 修复或复查：

```text
事务缺失
状态机不完整
重复提交风险
核心索引缺失
权限绕过
库存流水缺失
```

## MEDIUM

可以进入下一步，但要记录：

```text
页面体验不足
查询条件不完整
测试覆盖不足
非核心字段可优化
```

## LOW

不阻塞：

```text
文案
样式
字段顺序
轻微交互优化
```

---

# 15. 不允许的推进方式

禁止：

```text
1. 让 AI 一次性生成全系统。
2. 数据库未复核就写核心业务。
3. 库存模块未验收就进入工单提交。
4. 工单提交未验收就进入结算。
5. 支付退款未验收就进入财务报表。
6. Claude 修改库存、结算、财务核心逻辑。
7. Codex 擅自引入 Redis / MQ / 微服务。
8. 前端先行绕过后端状态动作。
9. 没有验收清单就合并。
```

---

# 16. 版本里程碑建议

建议按阶段打 tag：

```text
v0.1-backend-skeleton
v0.2-auth-dict
v0.3-inventory
v0.4-workorder
v0.5-payment-settlement
v0.6-official-reimbursement
v0.7-finance-export
v0.8-admin-mvp
v0.9-miniapp-mvp
v1.0-store-trial
```

---

# 17. 当前下一步

本文档确认后进入 Step 8：

```text
08_AI_COLLABORATION_AND_ACCEPTANCE.md
```

Step 8 将进一步固化：

```text
ChatGPT / Codex / Claude / Opus / Owner 分工
任务卡制度
复核制度
验收清单制度
Agent 输出格式
何时阻塞
何时放行
```

Step 8 完成后，正式进入执行阶段的第一个 Codex 任务：

```text
Codex 任务：项目仓库准备与基础文档落位
Codex 任务：基于 xiaoniu_05_DATABASE_DESIGN.md v0.3 生成 MySQL migration 初稿
```

---

## 18. 当前文档状态

本文档状态：

```text
Reviewed v0.2
已通过 Owner 确认，可进入执行阶段
```

复核重点：

```text
1. 实施阶段顺序是否合理？
2. 是否过早进入前端？
3. Codex / Claude 分工是否清楚？
4. 高风险模块是否都有评审关口？
5. 是否吸收 Step 6 的两个低风险优化建议？
6. 是否遗漏关键测试主链路？
7. 是否能支持后续任务卡生成？
8. 工单费用项目模型升级后，Phase 3~4 任务和验收标准是否一致？
9. 财务汇总口径是否已按 charge_type 拆分？
```
