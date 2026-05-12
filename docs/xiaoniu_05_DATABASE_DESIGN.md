# 05_DATABASE_DESIGN.md

# 小牛电动官方授权店两轮车维修售后库存管理系统 - 数据库设计文档

版本：v0.3  
日期：2026-05-10  
阶段：项目准备阶段 Step 5 / 8  
修订依据：工单费用项目明细模型升级 2026-05-10  
前置文档：

- `01_MVP_SCOPE.md`
- `02_BUSINESS_FLOWS.md`
- `03_CORE_BUSINESS_RULES.md`
- `04_DOMAIN_MODEL.md` v0.3

用途：用于定义 MVP 阶段 MySQL 数据库初版设计，供 Owner、Opus、Codex 复核。本文档是后续 Spring Boot 后端实现、migration SQL、实体建模、接口设计和测试验收的数据库依据。

本次修订重点：

```text
1. 将 work_order_item 表替换为 work_order_charge_item 表，统一承载配件费、工时费、其他费用。
2. work_order 表中标注 labor_fee / other_fee 为废弃字段，receivable_amount 改为由 charge items 汇总。
3. 新增 work_order_charge_item 表字段设计，包含 charge_type、inventory_affecting 等核心字段。
4. 财务统计来源改为按 charge_type 分类汇总。
5. 库存一致性章节明确只处理 charge_type = PART 且 inventory_affecting = 1 的明细。
6. Codex migration 要求同步更新为 work_order_charge_item。
7. Opus / Owner 复核重点中加入 charge_type 设计验证。
```

---

## 1. 文档目的

本文档用于把已确认的 MVP 范围、业务流程、核心规则和领域模型落到数据库层。

本文档重点回答：

1. MVP 阶段需要哪些核心表？
2. 每张表承担什么业务职责？
3. 哪些字段是必须字段？
4. 哪些状态、金额、数量字段必须提前设计清楚？
5. 哪些表之间有关联？
6. 哪些表必须有 `store_id`？
7. 哪些字段需要索引和唯一约束？
8. 哪些数据不能物理删除？
9. 哪些能力暂不建表，避免过度设计？
10. Codex 后续如何基于本文档生成 migration SQL？

本文档不是最终 SQL migration 文件。  
后续 Codex 生成 migration 时，必须基于本文档再结合实际项目命名规范、MyBatis / MyBatis-Plus 习惯和代码结构落地。

---

## 2. 数据库设计总原则

### 2.1 数据库选型

```text
MySQL 8.x
字符集：utf8mb4
排序规则：utf8mb4_0900_ai_ci 或 utf8mb4_general_ci
时间类型：DATETIME
主键类型：BIGINT
```

### 2.2 架构原则

```text
模块化单体
单库
单门店 MVP
所有核心业务表预留 store_id
不做多租户复杂架构
不做分库分表
不做微服务拆库
```

### 2.3 业务原则

```text
工单驱动业务
库存通过流水记账
支付通过明细记账
退款不能删除原支付
官方结算和客户支付分开
报销确认后才计入成本
财务报表从业务明细汇总
```

### 2.4 表设计原则

```text
1. 核心业务表保留审计字段。
2. 核心业务表保留 store_id。
3. 金额字段使用 DECIMAL。
4. 数量字段使用 INT 或 DECIMAL，根据业务性质选择。
5. 状态字段使用 VARCHAR 编码，不使用魔法数字。
6. 字典表用于展示和扩展，核心状态仍由后端枚举保护。
7. 库存、支付、退款、报销等流水/明细表原则上不物理删除。
8. 已有业务引用的主数据不物理删除，使用停用或软删除。
```

---

## 3. 通用字段规范

### 3.1 主键字段

所有表默认使用：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 ID |

主键生成方式可由 Codex 后续根据技术方案选择：

```text
雪花 ID
数据库自增
MyBatis-Plus ASSIGN_ID
```

建议优先：

```text
MyBatis-Plus ASSIGN_ID / 雪花 ID
```

理由：

```text
后续多门店或数据迁移更方便
```

但 MVP 使用数据库自增也可接受。最终由 Codex 根据项目脚手架统一。

---

### 3.2 门店字段

核心业务表保留：

| 字段 | 类型 | 说明 |
|---|---|---|
| store_id | BIGINT | 门店 ID |

MVP 当前只有一个默认门店，但字段必须保留。

---

### 3.3 审计字段

核心表建议统一字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| created_by | BIGINT NULL | 创建人 |
| created_at | DATETIME NOT NULL | 创建时间 |
| updated_by | BIGINT NULL | 更新人 |
| updated_at | DATETIME NOT NULL | 更新时间 |

说明：

```text
审计字段不替代业务字段。
例如工单仍需要 submitted_by / submitted_at、settled_by / settled_at。
库存流水仍需要 operator_id / operated_at。
```

---

### 3.4 删除、停用与软删除策略

主数据和可编辑业务表可以保留统一软删除字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| deleted | TINYINT NOT NULL DEFAULT 0 | 0 正常，1 删除 |

但 MVP 阶段的业务策略是：

```text
核心业务数据不开放普通删除。
主数据优先使用 status=DISABLED 停用。
明细和流水通过状态、冲正、调整或新增记录表达变化。
```

原因：

```text
1. 避免唯一约束与软删除记录冲突。
2. 避免历史工单、库存流水、支付退款、财务统计断链。
3. 避免误删后影响对账和追溯。
```

不建议物理删除，也不建议开放普通软删除：

```text
work_order
work_order_charge_item
work_order_status_log
inventory_flow
payment_record
refund_record
official_after_sales
reimbursement
```

可以停用而非删除：

```text
part
sys_user
sys_role
sys_dict_item
```

唯一约束处理策略：

```text
MVP 采用“编码被使用后不复用”的简单策略。
例如配件编码、工单编号、支付编号、退款编号、报销编号一旦生成，不因停用或删除而释放。
不为唯一索引额外加入 deleted 维度。
```

---

### 3.5 状态字段

状态字段统一使用：

```text
VARCHAR(32)
```

示例：

```text
DRAFT
PENDING_ACCEPT
SETTLED
CANCELLED
CONFIRMED
```

原则：

```text
后端枚举控制状态机
字典表只负责展示名称、排序、启停
```

---

### 3.6 金额字段

金额字段建议：

```text
DECIMAL(18,2)
```

成本单价可考虑：

```text
DECIMAL(18,4)
```

建议：

| 类型 | 数据库类型 |
|---|---|
| 支付金额 | DECIMAL(18,2) |
| 退款金额 | DECIMAL(18,2) |
| 人工费 | DECIMAL(18,2) |
| 其他费用 | DECIMAL(18,2) |
| 销售价 | DECIMAL(18,2) |
| 成本价 | DECIMAL(18,4) |
| 官方结算金额 | DECIMAL(18,2) |
| 报销金额 | DECIMAL(18,2) |

Java 中必须使用：

```text
BigDecimal
```

禁止使用：

```text
double
float
```

---

### 3.7 数量字段

配件库存数量如果只按件管理，建议：

```text
INT
```

字段包括：

```text
actual_qty
available_qty
reserved_qty
quantity
```

如果未来存在小数计量单位，再扩展为 DECIMAL。MVP 按整数处理即可。

---

## 4. 表清单总览

### 4.1 基础组织与权限

| 表名 | 说明 |
|---|---|
| store | 门店 |
| sys_user | 用户 / 员工 |
| sys_role | 角色 |
| sys_permission | 权限点 |
| sys_user_role | 用户角色关联 |
| sys_role_permission | 角色权限关联 |
| sequence_daily | 业务编号日序号计数 |

---

### 4.2 字典配置

| 表名 | 说明 |
|---|---|
| sys_dict_type | 字典类型 |
| sys_dict_item | 字典项 |

---

### 4.3 客户与车辆

| 表名 | 说明 |
|---|---|
| customer | 客户 |
| vehicle | 车辆 |

---

### 4.4 配件与库存

| 表名 | 说明 |
|---|---|
| part | 配件主数据 |
| part_barcode | 配件条码 |
| inventory_stock | 库存快照 |
| inventory_flow | 库存流水 |

---

### 4.5 工单

| 表名 | 说明 |
|---|---|
| work_order | 工单主表 |
| work_order_charge_item | 工单费用项目明细 |
| work_order_status_log | 工单状态日志 |

---

### 4.6 支付、退款、官方售后

| 表名 | 说明 |
|---|---|
| payment_record | 支付记录 |
| refund_record | 退款记录 |
| official_after_sales | 官方售后信息与官方结算 |

---

### 4.7 报销与财务

| 表名 | 说明 |
|---|---|
| reimbursement | 报销台账 |

说明：

```text
MVP 不单独建 finance_report 表。
财务报表从 payment_record、refund_record、official_after_sales、work_order_charge_item、reimbursement 等明细表实时汇总。
```

---

### 4.8 暂不建表

MVP 暂不建：

| 表名 / 概念 | 原因 |
|---|---|
| inventory_operation | 库存动作由 inventory_flow.flow_type 表达 |
| official_settlement_record | MVP 单次手动官方结算，字段放 official_after_sales |
| export_task / export_record | MVP 可同步导出，后续需要审计/异步再建 |
| purchase_order | 不做自动采购 |
| transfer_order | 不做多门店调拨 |
| stocktake_order | 不做复杂盘点 |
| attachment / voucher | 不做图片上传 |
| commission_rule | 不做员工提成 |
| reverse_settlement | 不做反结算 |

---

## 5. 组织与权限表

## 5.1 store - 门店表

### 业务职责

记录门店基础信息。MVP 只有一个默认门店，但所有核心业务表通过 `store_id` 预留未来多门店扩展。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 门店 ID |
| store_code | VARCHAR(64) | UNIQUE NOT NULL | 门店编码 |
| store_name | VARCHAR(128) | NOT NULL | 门店名称 |
| contact_phone | VARCHAR(32) | NULL | 联系电话 |
| address | VARCHAR(255) | NULL | 地址 |
| status | VARCHAR(32) | NOT NULL | ENABLED / DISABLED |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 索引建议

```text
uk_store_code(store_code)
idx_store_status(status)
```

---

## 5.2 sys_user - 用户表

### 业务职责

记录员工、管理员、财务、老板等系统使用者。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 用户 ID |
| store_id | BIGINT | NOT NULL | 所属门店 |
| username | VARCHAR(64) | NULL | 登录账号 |
| password_hash | VARCHAR(255) | NULL | 密码哈希，若只用验证码/微信可为空 |
| real_name | VARCHAR(64) | NOT NULL | 姓名 |
| phone | VARCHAR(32) | NULL | 手机号 |
| wechat_openid | VARCHAR(128) | NULL | 微信 openid |
| wechat_unionid | VARCHAR(128) | NULL | 微信 unionid |
| status | VARCHAR(32) | NOT NULL | ENABLED / DISABLED |
| last_login_at | DATETIME | NULL | 最近登录时间 |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 索引建议

```text
idx_user_store_id(store_id)
uk_user_phone(phone)
uk_user_username(username)
uk_user_wechat_openid(wechat_openid)
idx_user_status(status)
```

### 设计说明

```text
1. 手机号登录和微信登录都需要支持。
2. 微信登录后仍需要支持绑定手机号。
3. 停用用户不能登录。
4. password_hash 可根据登录方式选择是否启用。
5. 创建用户时，phone / username / wechat_openid 至少必须具备一个登录标识。
6. 第 5 条由应用层强校验，避免出现无法登录、无法绑定、无法识别身份的用户记录。
```

---

## 5.3 sys_role - 角色表

### 业务职责

记录角色，例如超级管理员、门店管理员、财务、维修员/前台。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 角色 ID |
| store_id | BIGINT | NULL | 门店 ID，系统内置角色可为空 |
| role_code | VARCHAR(64) | NOT NULL | 角色编码 |
| role_name | VARCHAR(64) | NOT NULL | 角色名称 |
| status | VARCHAR(32) | NOT NULL | ENABLED / DISABLED |
| sort_order | INT | NOT NULL DEFAULT 0 | 排序 |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 索引建议

```text
uk_role_store_code(store_id, role_code)
idx_role_status(status)
```

---

## 5.4 sys_permission - 权限点表

### 业务职责

记录具体权限点。业务代码不应只用角色名称判断权限，应以权限点为准。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 权限 ID |
| permission_code | VARCHAR(128) | UNIQUE NOT NULL | 权限编码 |
| permission_name | VARCHAR(128) | NOT NULL | 权限名称 |
| module_code | VARCHAR(64) | NOT NULL | 模块编码 |
| status | VARCHAR(32) | NOT NULL | ENABLED / DISABLED |
| sort_order | INT | NOT NULL DEFAULT 0 | 排序 |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 推荐权限点

```text
USER_MANAGE
ROLE_MANAGE
DICT_MANAGE
PART_MANAGE
INVENTORY_VIEW
INVENTORY_INBOUND
INVENTORY_ADJUST
WORK_ORDER_CREATE
WORK_ORDER_UPDATE
WORK_ORDER_SUBMIT
WORK_ORDER_CANCEL
WORK_ORDER_SETTLE
PAYMENT_RECORD
REFUND_RECORD
OFFICIAL_SETTLEMENT_MANAGE
REIMBURSEMENT_SUBMIT
REIMBURSEMENT_CONFIRM
FINANCE_VIEW
EXCEL_EXPORT
```

---

## 5.5 sys_user_role - 用户角色关联表

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | ID |
| user_id | BIGINT | NOT NULL | 用户 ID |
| role_id | BIGINT | NOT NULL | 角色 ID |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 索引建议

```text
uk_user_role(user_id, role_id)
idx_user_role_role_id(role_id)
```

---

## 5.6 sys_role_permission - 角色权限关联表

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | ID |
| role_id | BIGINT | NOT NULL | 角色 ID |
| permission_id | BIGINT | NOT NULL | 权限 ID |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 索引建议

```text
uk_role_permission(role_id, permission_id)
idx_role_permission_permission_id(permission_id)
```


---

## 5.7 sequence_daily - 业务编号日序号计数表

### 业务职责

用于生成工单、支付、退款、报销、第三方配件等业务编号的日序号。

MVP 不引入 Redis，不能使用应用内存计数，也不建议使用 `MAX + 1` 方式生成编号。该表作为数据库内持久化计数器，配合事务或行锁生成稳定编号。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | ID |
| seq_type | VARCHAR(32) | NOT NULL | 编号类型 |
| seq_date | DATE | NOT NULL | 编号日期 |
| current_val | BIGINT | NOT NULL DEFAULT 0 | 当前序号 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### seq_type 建议取值

```text
WORK_ORDER
PAYMENT
REFUND
REIMBURSEMENT
PART_CODE
```

### 索引建议

```text
uk_sequence_daily(seq_type, seq_date)
```

### 设计说明

```text
1. 编号生成必须在数据库事务中完成。
2. 同一 seq_type + seq_date 只允许一条记录。
3. 生成编号时应锁定对应记录并递增 current_val。
4. 禁止使用 MAX(编号) + 1 或应用内存计数生成关键业务编号。
5. 该表是 MVP 推荐建表，不属于过度设计。
```

---

# 6. 字典配置表

## 6.1 sys_dict_type - 字典类型表

### 业务职责

记录字典分类，例如支付方式、配件来源、库存流水类型等。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 字典类型 ID |
| type_code | VARCHAR(64) | UNIQUE NOT NULL | 字典类型编码 |
| type_name | VARCHAR(128) | NOT NULL | 字典类型名称 |
| status | VARCHAR(32) | NOT NULL | ENABLED / DISABLED |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 推荐字典类型

```text
WORK_ORDER_STATUS
PAYMENT_METHOD
PART_SOURCE
PART_CATEGORY
REIMBURSEMENT_STATUS
INVENTORY_FLOW_TYPE
OFFICIAL_SETTLEMENT_STATUS
USER_STATUS
COMMON_STATUS
```

---

## 6.2 sys_dict_item - 字典项表

### 业务职责

记录具体字典项。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 字典项 ID |
| type_id | BIGINT | NOT NULL | 字典类型 ID |
| item_code | VARCHAR(64) | NOT NULL | 字典项编码 |
| item_name | VARCHAR(128) | NOT NULL | 展示名称 |
| sort_order | INT | NOT NULL DEFAULT 0 | 排序 |
| status | VARCHAR(32) | NOT NULL | ENABLED / DISABLED |
| is_system | TINYINT | NOT NULL DEFAULT 0 | 是否系统内置 |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 索引建议

```text
uk_dict_item_type_code(type_id, item_code)
idx_dict_item_status(status)
```

### 设计说明

```text
1. 工单状态字典只负责展示，不负责状态流转。
2. 状态流转必须由后端枚举和业务动作控制。
3. 支付方式、配件来源、配件分类可通过字典扩展。
```

---

# 7. 客户与车辆表

## 7.1 customer - 客户表

### 业务职责

记录维修客户基础信息。MVP 不做复杂 CRM。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 客户 ID |
| store_id | BIGINT | NOT NULL | 门店 ID |
| customer_name | VARCHAR(64) | NOT NULL | 客户姓名 |
| phone | VARCHAR(32) | NULL | 联系方式 |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 索引建议

```text
idx_customer_store_phone(store_id, phone)
idx_customer_name(customer_name)
```

---

## 7.2 vehicle - 车辆表

### 业务职责

记录客户车辆信息，用于通过车架号追溯维修历史。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 车辆 ID |
| store_id | BIGINT | NOT NULL | 门店 ID |
| customer_id | BIGINT | NULL | 客户 ID |
| model | VARCHAR(128) | NULL | 车型 |
| frame_no | VARCHAR(128) | NOT NULL | 车架号 |
| battery_no | VARCHAR(128) | NULL | 电池号 |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 索引建议

```text
uk_vehicle_store_frame_no(store_id, frame_no)
idx_vehicle_customer_id(customer_id)
idx_vehicle_battery_no(battery_no)
```

### 设计说明

```text
1. 车架号用于维修历史绑定，建议在同一门店内唯一。
2. 工单中仍需要保存车辆快照，避免车辆信息后续修改影响历史工单展示。
```

---

# 8. 配件与库存表

## 8.1 part - 配件主数据表

### 业务职责

记录官方配件和第三方配件的主数据。临时配件也必须进入此表。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 配件 ID |
| store_id | BIGINT | NOT NULL | 门店 ID |
| part_code | VARCHAR(64) | NOT NULL | 系统配件编码 |
| official_part_no | VARCHAR(128) | NULL | 官方品号 |
| part_name | VARCHAR(128) | NOT NULL | 配件名称 |
| model | VARCHAR(128) | NULL | 型号 |
| source | VARCHAR(32) | NOT NULL | OFFICIAL / THIRD_PARTY |
| category_code | VARCHAR(64) | NULL | 配件分类编码 |
| reference_cost_price | DECIMAL(18,4) | NULL | 参考成本价 |
| default_barcode | VARCHAR(128) | NULL | 默认条码 |
| location_remark | VARCHAR(255) | NULL | 库存位置备注 |
| create_source | VARCHAR(32) | NOT NULL | NORMAL / INBOUND / WORK_ORDER_TEMP |
| status | VARCHAR(32) | NOT NULL | ENABLED / DISABLED |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 索引建议

```text
uk_part_store_code(store_id, part_code)
idx_part_official_no(official_part_no)
idx_part_name(part_name)
idx_part_source(source)
idx_part_category(category_code)
idx_part_status(status)
idx_part_default_barcode(default_barcode)
```

### 设计说明

```text
1. 第三方配件 part_code 由系统生成。
2. 官方配件可录入 official_part_no。
3. 工单销售价不从 part 默认带出，销售价保存在 work_order_charge_item。
4. 已被工单或库存引用的配件不物理删除，使用停用。
5. 临时配件 create_source = WORK_ORDER_TEMP。
```

---

## 8.2 part_barcode - 配件条码表

### 业务职责

支持一个配件多个条码，满足扫码入库、扫码查询、条码打印。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 条码 ID |
| store_id | BIGINT | NOT NULL | 门店 ID |
| part_id | BIGINT | NOT NULL | 配件 ID |
| barcode | VARCHAR(128) | NOT NULL | 条码 |
| barcode_type | VARCHAR(32) | NULL | SYSTEM / MANUAL / OFFICIAL |
| is_primary | TINYINT | NOT NULL DEFAULT 0 | 是否主条码 |
| status | VARCHAR(32) | NOT NULL | ENABLED / DISABLED |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 索引建议

```text
uk_barcode_store_barcode(store_id, barcode)
idx_barcode_part_id(part_id)
idx_barcode_status(status)
```

---

## 8.3 inventory_stock - 库存快照表

### 业务职责

记录某门店某配件当前库存状态，用于快速查询库存。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 库存快照 ID |
| store_id | BIGINT | NOT NULL | 门店 ID |
| part_id | BIGINT | NOT NULL | 配件 ID |
| actual_qty | INT | NOT NULL DEFAULT 0 | 实际库存 |
| available_qty | INT | NOT NULL DEFAULT 0 | 可用库存 |
| reserved_qty | INT | NOT NULL DEFAULT 0 | 预占库存 |
| last_flow_id | BIGINT | NULL | 最近库存流水 ID |
| last_changed_at | DATETIME | NULL | 最近变动时间 |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 索引建议

```text
uk_inventory_stock_store_part(store_id, part_id)
idx_inventory_stock_part_id(part_id)
idx_inventory_stock_available_qty(available_qty)
```

### 约束建议

```text
actual_qty >= 0
available_qty >= 0
reserved_qty >= 0
```

MySQL 8 可以使用 CHECK，但 Codex 应根据实际兼容性决定是否启用。

### 设计说明

```text
1. 通常 actual_qty = available_qty + reserved_qty。
2. 库存快照只表示当前状态。
3. 任何库存变化必须同时写 inventory_flow。
4. 业务代码不得直接改库存快照而不写流水。
```

---

## 8.4 inventory_flow - 库存流水表

### 业务职责

记录每一次库存变化，是库存追溯和审计的核心表。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 流水 ID |
| store_id | BIGINT | NOT NULL | 门店 ID |
| inventory_stock_id | BIGINT | NOT NULL | 库存快照 ID |
| part_id | BIGINT | NOT NULL | 配件 ID |
| flow_type | VARCHAR(32) | NOT NULL | INBOUND / RESERVE / RELEASE / CONSUME / ADJUST |
| quantity_delta | INT | NOT NULL | 本次变化数量，正负按类型约定 |
| actual_before | INT | NOT NULL | 变动前实际库存 |
| actual_after | INT | NOT NULL | 变动后实际库存 |
| available_before | INT | NOT NULL | 变动前可用库存 |
| available_after | INT | NOT NULL | 变动后可用库存 |
| reserved_before | INT | NOT NULL | 变动前预占库存 |
| reserved_after | INT | NOT NULL | 变动后预占库存 |
| business_type | VARCHAR(64) | NOT NULL | 业务来源类型 |
| business_id | BIGINT | NULL | 业务来源 ID |
| work_order_id | BIGINT | NULL | 工单 ID |
| work_order_charge_item_id | BIGINT | NULL | 工单费用项目明细 ID，关联 work_order_charge_item.id，仅工单相关库存流水使用 |
| operator_id | BIGINT | NOT NULL | 操作人 |
| operated_at | DATETIME | NOT NULL | 操作时间 |
| reason | VARCHAR(255) | NULL | 原因 |
| remark | VARCHAR(512) | NULL | 备注 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### flow_type 取值

```text
INBOUND   入库
RESERVE   预占
RELEASE   释放
CONSUME   消耗
ADJUST    调整
```

### business_type 建议取值

```text
NORMAL_INBOUND
WORK_ORDER_SUBMIT
WORK_ORDER_CANCEL
WORK_ORDER_SETTLE
WORK_ORDER_CHARGE_ITEM_ADJUST
MANUAL_ADJUST
WORK_ORDER_TEMP_INBOUND
```

### 索引建议

```text
idx_flow_store_part(store_id, part_id)
idx_flow_stock_id(inventory_stock_id)
idx_flow_type(flow_type)
idx_flow_business(business_type, business_id)
idx_flow_work_order_id(work_order_id)
idx_flow_operated_at(operated_at)
```

### 设计说明

```text
1. inventory_flow 原则上不允许删除。
2. 库存变化必须写入库存快照和库存流水。
3. business_type + business_id 是通用业务来源追溯字段。在工单相关库存流水中：
   - business_type 可取 WORK_ORDER_SUBMIT / WORK_ORDER_CANCEL / WORK_ORDER_SETTLE / WORK_ORDER_CHARGE_ITEM_ADJUST 等。
   - business_id 是通用业务来源 ID，优先指向触发本次库存动作的业务动作或主业务对象；工单场景下可指向 work_order.id 或操作批次 ID，按实现约定确定。
4. work_order_id 用于快速关联工单。
5. work_order_charge_item_id 用于快速关联具体 PART 类型费用项目明细，关联 work_order_charge_item.id，仅工单相关库存流水使用。
6. 三者语义不混淆：work_order_id = 工单维度，work_order_charge_item_id = 明细维度，business_id = 通用追溯维度。
```

---

# 9. 工单表

## 9.1 work_order - 工单主表

### 业务职责

记录一次维修售后业务，是系统主业务单据。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 工单 ID |
| store_id | BIGINT | NOT NULL | 门店 ID |
| work_order_no | VARCHAR(64) | UNIQUE NOT NULL | 工单编号 |
| customer_id | BIGINT | NULL | 客户 ID |
| vehicle_id | BIGINT | NULL | 车辆 ID |
| customer_name_snapshot | VARCHAR(64) | NOT NULL | 客户姓名快照 |
| customer_phone_snapshot | VARCHAR(32) | NULL | 客户联系方式快照 |
| vehicle_model_snapshot | VARCHAR(128) | NULL | 车型快照 |
| frame_no_snapshot | VARCHAR(128) | NULL | 车架号快照 |
| battery_no_snapshot | VARCHAR(128) | NULL | 电池号快照 |
| repair_item | VARCHAR(512) | NOT NULL | 维修项目 |
| status | VARCHAR(32) | NOT NULL | 工单状态 |
| labor_fee | DECIMAL(18,2) | NOT NULL DEFAULT 0.00 | 人工费（已废弃，不再作为主要金额来源） |
| other_fee | DECIMAL(18,2) | NOT NULL DEFAULT 0.00 | 其他费用（已废弃，不再作为主要金额来源） |
| receivable_amount | DECIMAL(18,2) | NOT NULL DEFAULT 0.00 | 应收金额快照，由 work_order_charge_item.line_amount 汇总 |
| submitted_by | BIGINT | NULL | 提交人 |
| submitted_at | DATETIME | NULL | 提交时间 |
| settled_by | BIGINT | NULL | 结算人 |
| settled_at | DATETIME | NULL | 结算时间 |
| cancelled_by | BIGINT | NULL | 取消人 |
| cancelled_at | DATETIME | NULL | 取消时间 |
| cancel_reason | VARCHAR(255) | NULL | 取消原因 |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 状态取值

```text
DRAFT               草稿 / 未提交
PENDING_ACCEPT      待接单
ACCEPTED            已接单
PART_ORDERED        已定件
PART_ARRIVED        已到件
SETTLED             已结算
CANCELLED           已取消
```

### 索引建议

```text
uk_work_order_no(work_order_no)
idx_work_order_store_status(store_id, status)
idx_work_order_store_created(store_id, created_at)
idx_work_order_customer_id(customer_id)
idx_work_order_vehicle_id(vehicle_id)
idx_work_order_frame_no(frame_no_snapshot)
idx_work_order_created_at(created_at)
idx_work_order_settled_at(settled_at)
```

### 设计说明

```text
1. 创建工单状态为 DRAFT，不预占库存。
2. 提交工单从 DRAFT → PENDING_ACCEPT，只对 charge_type = PART 且 inventory_affecting = 1 的明细触发库存预占。
3. 工单结算只对 PART 类型明细扣减库存。
4. 工单取消只对 PART 类型明细释放库存。
5. receivable_amount 是应收金额快照，由 work_order_charge_item.line_amount 汇总得到，防止历史工单金额因明细后续变化产生歧义。
6. labor_fee / other_fee 已废弃，不再作为工单金额的主要来源字段。
7. 支付和退款不放在 work_order 单字段里，而是通过 payment_record / refund_record 汇总。
8. 官方售后信息不放在 payment_record，单独在 official_after_sales。
```

---

## 9.2 work_order_charge_item - 工单费用项目明细表

### 业务职责

记录工单中统一的收费项目明细，可为配件费、工时费或其他费用。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 工单费用明细 ID |
| store_id | BIGINT | NOT NULL | 门店 ID |
| work_order_id | BIGINT | NOT NULL | 工单 ID |
| charge_type | VARCHAR(32) | NOT NULL | 费用类型：PART / LABOR / OTHER |
| item_name | VARCHAR(255) | NOT NULL | 项目名称，如”后减震器更换””前面板喷漆””检测费” |
| part_id | BIGINT | NULL | 配件 ID，仅 PART 类型必填 |
| part_code_snapshot | VARCHAR(64) | NULL | 配件编码快照，仅 PART 类型 |
| part_name_snapshot | VARCHAR(128) | NULL | 配件名称快照，仅 PART 类型 |
| part_source_snapshot | VARCHAR(32) | NULL | 配件来源快照，仅 PART 类型 |
| quantity | INT | NOT NULL | 数量，PART 类型为配件数量，LABOR / OTHER 默认 1 |
| unit | VARCHAR(32) | NULL | 单位，如 个 / 对 / 次 / 项 |
| unit_price | DECIMAL(18,2) | NOT NULL | 单价 |
| line_amount | DECIMAL(18,2) | NOT NULL | 明细金额 = quantity × unit_price |
| cost_price_snapshot | DECIMAL(18,4) | NULL | 配件成本价快照，仅 PART 类型 |
| line_cost_amount | DECIMAL(18,2) | NULL | 明细成本金额 = quantity × cost_price_snapshot，仅 PART 类型 |
| inventory_affecting | TINYINT | NOT NULL DEFAULT 0 | 是否影响库存：PART = 1，LABOR / OTHER = 0 |
| is_temp_part | TINYINT | NOT NULL DEFAULT 0 | 是否临时配件，仅 PART 类型 |
| status | VARCHAR(32) | NOT NULL DEFAULT 'ACTIVE' | ACTIVE / REMOVED |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### charge_type 取值

```text
PART    配件费
LABOR   工时费
OTHER   其他费用
```

### 索引建议

```text
idx_charge_item_order_id(work_order_id)
idx_charge_item_part_id(part_id)
idx_charge_item_store_type(store_id, charge_type)
idx_charge_item_store_part(store_id, part_id)
```

### 设计说明

```text
1. charge_type 是核心分类字段，PART / LABOR / OTHER 语义必须区分。
2. charge_type = PART 时 part_id 必填，inventory_affecting = 1，影响库存预占/释放/扣减。
3. charge_type = LABOR / OTHER 时 part_id 为空，inventory_affecting = 0，不影响库存。
4. DRAFT 阶段可以普通增删改 charge items，不影响库存。
5. DRAFT 删除 charge item 直接删除草稿明细，不生成库存流水。
6. 已提交未结算工单不允许普通编辑 charge items。
7. 如必须调整已提交工单 charge items，必须走 adjust-charge-items 动作，PART 类型同步库存预占差异。
8. 已结算工单不允许调整 charge items。
9. unit_price 手动填写。
10. cost_price_snapshot 必须保存，用于历史利润统计。
11. line_amount = quantity × unit_price。
12. line_cost_amount = quantity × cost_price_snapshot（仅 PART 类型）。
13. 应收金额 receivable_amount = Σ work_order_charge_item.line_amount。
14. 配件收入 = Σ charge_type = PART 的 line_amount。
15. 人工费收入 = Σ charge_type = LABOR 的 line_amount。
16. 其他收入 = Σ charge_type = OTHER 的 line_amount。
17. 配件成本 = Σ charge_type = PART 的 line_cost_amount。
```

---

## 9.3 work_order_status_log - 工单状态日志表

### 业务职责

记录工单状态变化。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 状态日志 ID |
| store_id | BIGINT | NOT NULL | 门店 ID |
| work_order_id | BIGINT | NOT NULL | 工单 ID |
| from_status | VARCHAR(32) | NULL | 原状态 |
| to_status | VARCHAR(32) | NOT NULL | 新状态 |
| action_type | VARCHAR(64) | NOT NULL | CREATE / SUBMIT / CANCEL / SETTLE / UPDATE_STATUS |
| operator_id | BIGINT | NOT NULL | 操作人 |
| operated_at | DATETIME | NOT NULL | 操作时间 |
| reason | VARCHAR(255) | NULL | 原因 |
| remark | VARCHAR(512) | NULL | 备注 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 索引建议

```text
idx_status_log_order_id(work_order_id)
idx_status_log_store_operated(store_id, operated_at)
idx_status_log_action(action_type)
```

---

# 10. 支付、退款、官方售后表

## 10.1 payment_record - 支付记录表

### 业务职责

记录客户付款明细。一个工单可多次付款、混合付款。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 支付记录 ID |
| store_id | BIGINT | NOT NULL | 门店 ID |
| work_order_id | BIGINT | NOT NULL | 工单 ID |
| payment_no | VARCHAR(64) | UNIQUE NOT NULL | 支付记录编号 |
| amount | DECIMAL(18,2) | NOT NULL | 支付金额 |
| payment_method | VARCHAR(32) | NOT NULL | WECHAT / ALIPAY / UNIONPAY / CASH / OTHER |
| paid_at | DATETIME | NOT NULL | 支付时间 |
| receiver_id | BIGINT | NULL | 收款人 |
| operator_id | BIGINT | NOT NULL | 操作人 |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 索引建议

```text
uk_payment_no(payment_no)
idx_payment_order_id(work_order_id)
idx_payment_store_paid_at(store_id, paid_at)
idx_payment_method(payment_method)
```

### 设计说明

```text
1. 支付记录不得因退款而删除。
2. 工单实收金额由支付记录总额减退款记录总额计算。
3. 支付方式来自字典，核心编码由后端保护。
```

---

## 10.2 refund_record - 退款记录表

### 业务职责

记录客户退款明细。退款不能删除原支付记录。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 退款记录 ID |
| store_id | BIGINT | NOT NULL | 门店 ID |
| work_order_id | BIGINT | NOT NULL | 工单 ID |
| refund_no | VARCHAR(64) | UNIQUE NOT NULL | 退款记录编号 |
| amount | DECIMAL(18,2) | NOT NULL | 退款金额 |
| refund_method | VARCHAR(32) | NOT NULL | WECHAT / ALIPAY / UNIONPAY / CASH / OTHER |
| refunded_at | DATETIME | NOT NULL | 退款时间 |
| operator_id | BIGINT | NOT NULL | 操作人 |
| reason | VARCHAR(255) | NOT NULL | 退款原因 |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 索引建议

```text
uk_refund_no(refund_no)
idx_refund_order_id(work_order_id)
idx_refund_store_refunded_at(store_id, refunded_at)
idx_refund_method(refund_method)
```

### 设计说明

```text
1. 退款金额不能超过可退金额：支付总额 - 已退款总额。
2. 已结算后退款只影响财务客户支付净收入，不自动回滚工单状态和库存。
3. 退款必须保留原因。
```

---

## 10.3 official_after_sales - 官方售后信息表

### 业务职责

记录工单对应的官方售后信息和官方结算信息。MVP 不拆 `official_settlement_record`。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 官方售后 ID |
| store_id | BIGINT | NOT NULL | 门店 ID |
| work_order_id | BIGINT | UNIQUE NOT NULL | 工单 ID |
| is_official_after_sales | TINYINT | NOT NULL DEFAULT 0 | 是否官方售后 |
| official_order_no | VARCHAR(128) | NULL | 官方售后订单号 |
| official_settlement_amount | DECIMAL(18,2) | NULL | 官方结算金额 |
| official_settlement_status | VARCHAR(32) | NOT NULL DEFAULT 'PENDING' | NOT_REQUIRED / PENDING / SETTLED |
| official_settlement_time | DATETIME | NULL | 官方结算时间 |
| official_settlement_operator_id | BIGINT | NULL | 官方结算操作人 |
| official_settlement_remark | VARCHAR(512) | NULL | 官方结算备注 |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 索引建议

```text
uk_official_work_order(work_order_id)
idx_official_store_status(store_id, official_settlement_status)
idx_official_order_no(official_order_no)
idx_official_settlement_time(official_settlement_time)
```

### 设计说明

```text
1. official_after_sales 与 work_order 是 1:1。
2. 普通第三方维修工单可以没有 official_after_sales 记录，也可以有记录但 is_official_after_sales = 0。实现时二选一统一。
3. 官方结算金额不能写入 payment_record。
4. 官方结算收入在财务报表中单独统计。
5. 后续如出现多次结算、部分结算、冲销，再扩展 official_settlement_record。
```

---

# 11. 报销表

## 11.1 reimbursement - 报销台账表

### 业务职责

记录员工报销台账。只有已确认报销才计入运营成本。

### 字段设计

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK | 报销 ID |
| store_id | BIGINT | NOT NULL | 门店 ID |
| reimbursement_no | VARCHAR(64) | UNIQUE NOT NULL | 报销编号 |
| applicant_id | BIGINT | NOT NULL | 报销人 |
| purpose | VARCHAR(255) | NOT NULL | 报销用途 |
| amount | DECIMAL(18,2) | NOT NULL | 申请报销金额 |
| confirmed_amount | DECIMAL(18,2) | NULL | 确认报销金额 |
| status | VARCHAR(32) | NOT NULL | PENDING / CONFIRMED / REJECTED_OR_CANCELLED |
| submitted_at | DATETIME | NOT NULL | 提交时间 |
| confirmed_by | BIGINT | NULL | 确认人 |
| confirmed_at | DATETIME | NULL | 确认时间 |
| rejected_by | BIGINT | NULL | 驳回/取消人 |
| rejected_at | DATETIME | NULL | 驳回/取消时间 |
| reject_reason | VARCHAR(255) | NULL | 驳回/取消原因 |
| remark | VARCHAR(512) | NULL | 备注 |
| created_by | BIGINT | NULL | 创建人 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_by | BIGINT | NULL | 更新人 |
| updated_at | DATETIME | NOT NULL | 更新时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 软删除 |

### 索引建议

```text
uk_reimbursement_no(reimbursement_no)
idx_reimbursement_store_status(store_id, status)
idx_reimbursement_applicant(applicant_id)
idx_reimbursement_submitted_at(submitted_at)
idx_reimbursement_confirmed_at(confirmed_at)
```

### 设计说明

```text
1. PENDING 不计入成本。
2. CONFIRMED 计入成本，金额取 confirmed_amount；如果为空，可取 amount。
3. REJECTED_OR_CANCELLED 不计入成本。
4. MVP 不做审批流和自动打款。
```

---

# 12. 财务报表设计

## 12.1 MVP 不建 finance_report 表

MVP 阶段财务报表通过查询明细实时汇总，不单独建可编辑财务表。

原因：

```text
1. 避免利润被手工修改。
2. 避免明细和报表快照不一致。
3. MVP 数据量可控。
4. 后续数据量大时再考虑日报/月报快照表。
```

---

## 12.2 财务统计来源

### 客户支付净收入

来源：

```text
payment_record
refund_record
```

计算：

```text
客户支付净收入 = 支付总额 - 退款总额
```

注意：

```text
已结算后退款仍会降低客户支付净收入。
不自动回退工单状态或库存。
```

---

### 官方结算收入

来源：

```text
official_after_sales
```

条件：

```text
official_settlement_status = SETTLED
official_settlement_amount IS NOT NULL
```

---

### 配件收入

来源：

```text
work_order_charge_item.line_amount WHERE charge_type = 'PART'
```

建议统计条件：

```text
work_order.status = SETTLED
```

---

### 人工费和其他收入

来源：

```text
work_order_charge_item.line_amount WHERE charge_type = 'LABOR'
work_order_charge_item.line_amount WHERE charge_type = 'OTHER'
```

建议统计条件：

```text
work_order.status = SETTLED
```

---

### 配件成本

来源：

```text
work_order_charge_item.line_cost_amount WHERE charge_type = 'PART'
```

建议统计条件：

```text
work_order.status = SETTLED
```

---

### 报销成本

来源：

```text
reimbursement.confirmed_amount 或 reimbursement.amount
```

条件：

```text
reimbursement.status = CONFIRMED
```

---

### 利润

计算：

```text
利润 = 收入 - 成本
```

收入：

```text
配件收入 + 人工费 + 其他收入 + 官方结算收入
```

成本：

```text
配件成本 + 报销成本
```

说明：

```text
客户支付净收入和工单收入统计存在业务口径差异，报表设计时需要明确展示：
1. 应收维度收入：配件收入 + 人工费 + 其他收入
2. 实收维度收入：客户支付净收入
3. 官方维度收入：官方结算收入
```

建议 MVP 财务页面至少分开展示：

```text
客户支付净收入
官方结算收入
配件销售收入
人工费收入
其他收入
配件成本
报销成本
利润
```

时间口径说明：

```text
1. 客户支付净收入按 payment_record.paid_at 与 refund_record.refunded_at 统计。
2. 配件销售收入、人工费收入、其他收入、配件成本按 work_order.settled_at 统计。
3. 官方结算收入按 official_after_sales.official_settlement_time 统计。
4. 报销成本按 reimbursement.confirmed_at 统计。
5. 同一天的“客户支付净收入”和“工单结算收入”可能不相等，这是业务时间口径差异，不是错误。
6. 前端报表应分开展示实收口径、结算口径和官方结算口径，不强制三者相等。
```

---

# 13. 核心关系图

## 13.1 主关系

```text
store
  ├── sys_user
  ├── part
  │     ├── part_barcode
  │     └── inventory_stock
  │           └── inventory_flow
  ├── customer
  │     └── vehicle
  ├── work_order
  │     ├── work_order_charge_item
  │     ├── work_order_status_log
  │     ├── payment_record
  │     ├── refund_record
  │     └── official_after_sales
  └── reimbursement
```

---

## 13.2 工单主链路关系

```text
work_order
  ├── work_order_charge_item
  │     └── part（仅 PART 类型关联）
  ├── payment_record
  ├── refund_record
  ├── official_after_sales
  └── work_order_status_log
```

---

## 13.3 库存追溯关系

```text
part
  └── inventory_stock
        └── inventory_flow
              ├── work_order_id
              └── work_order_charge_item_id（关联 work_order_charge_item.id）
```

---

# 14. 编号设计建议

系统需要生成以下业务编号：

| 编号 | 表 | 示例 |
|---|---|---|
| 工单编号 | work_order.work_order_no | WO202605100001 |
| 支付编号 | payment_record.payment_no | PAY202605100001 |
| 退款编号 | refund_record.refund_no | REF202605100001 |
| 报销编号 | reimbursement.reimbursement_no | REIM202605100001 |
| 第三方配件编码 | part.part_code | TP202605100001 |

编号生成原则：

```text
1. 由后端生成。
2. 前端不得生成最终业务编号。
3. 同类编号全局唯一。
4. 建议包含日期和序号，便于人工核对。
```

Codex 必须采用数据库内可持久化的编号生成方式。

MVP 推荐并纳入本文档的方案：

```text
sequence_daily 日序号计数表
```

不推荐：

```text
Redis 计数器：MVP 不引入 Redis
应用内存计数：重启后不可靠
MAX(编号) + 1：并发下不安全
```

编号生成建议：

```text
1. 根据 seq_type + seq_date 获取或创建 sequence_daily 记录。
2. 在事务中锁定该记录。
3. current_val + 1。
4. 按前缀 + 日期 + 序号生成业务编号。
```

---

# 15. 数据一致性要求

## 15.1 库存一致性

必须保证：

```text
库存快照变化和库存流水写入在同一事务中完成。
```

关键动作：

```text
入库
提交工单预占（仅 charge_type = PART 且 inventory_affecting = 1 的明细）
取消工单释放（仅 PART 类型明细）
结算工单消耗（仅 PART 类型明细）
已提交工单调整费用项目（仅 PART 类型明细涉及库存）
库存调整
```

必须同时处理：

```text
inventory_stock
inventory_flow
相关 work_order / work_order_charge_item 状态
```

已提交工单调整费用项目属于库存一致性高风险动作，必须与提交、取消、结算同等级处理。

调整规则：

```text
仅对 charge_type = PART 且 inventory_affecting = 1 的明细执行库存调整：
1. REMOVED 的明细：释放对应预占库存，生成 RELEASE 流水。
2. 新增的明细：校验可用库存后增加预占库存，生成 RESERVE 流水。
3. 修改数量的明细：按差量调整预占库存。
4. 数量减少：释放差量预占库存，生成 RELEASE 流水。
5. 数量增加：校验可用库存后预占差量库存，生成 RESERVE 流水。
6. LABOR / OTHER 类型明细变更：只更新明细和 receivable_amount，不写库存流水。
7. 费用明细变更、库存快照变更、库存流水写入必须在同一事务中完成。
8. 调整动作必须记录调整人、调整时间和调整原因。
9. 已结算工单不允许走该调整动作。
```

---

## 15.2 工单状态一致性

必须保证：

```text
状态变化和状态日志在同一事务中完成。
```

涉及：

```text
提交
取消
结算
```

---

## 15.3 支付退款一致性

必须保证：

```text
退款金额不能超过可退金额。
```

可退金额：

```text
支付总额 - 已退款总额
```

---

## 15.4 官方结算一致性

必须保证：

```text
官方结算金额不进入 payment_record。
官方结算收入只从 official_after_sales 统计。
```

---

## 15.5 报销成本一致性

必须保证：

```text
只有 CONFIRMED 报销进入成本。
PENDING 和 REJECTED_OR_CANCELLED 不进入成本。
```

---

# 16. 关键约束与检查建议

## 16.1 建议数据库约束

可通过数据库或应用层控制：

```text
inventory_stock.actual_qty >= 0
inventory_stock.available_qty >= 0
inventory_stock.reserved_qty >= 0
work_order_charge_item.quantity > 0
payment_record.amount > 0
refund_record.amount > 0
reimbursement.amount > 0
```

MVP 中，库存和金额约束必须在应用层强校验。MySQL CHECK 可作为补充。

---

## 16.2 唯一约束

必须唯一：

```text
store.store_code
sys_user.phone
sys_user.username
sys_user.wechat_openid
sys_permission.permission_code
sys_dict_type.type_code
sys_dict_item(type_id, item_code)
part(store_id, part_code)
part_barcode(store_id, barcode)
inventory_stock(store_id, part_id)
work_order.work_order_no
payment_record.payment_no
refund_record.refund_no
official_after_sales.work_order_id
reimbursement.reimbursement_no
vehicle(store_id, frame_no)
```

注意：

```text
部分唯一约束与 deleted 软删除的兼容需要 Codex 在 migration 设计时处理。
例如可使用 store_id + code + deleted，或不允许同编码软删除后重建。
```

---

## 16.3 查询索引重点

高频查询：

```text
工单列表：store_id + status + created_at
工单按车架号查询：frame_no_snapshot
库存查询：store_id + part_id / part_name / barcode
库存流水：store_id + part_id + operated_at
支付记录：work_order_id / paid_at
退款记录：work_order_id / refunded_at
官方结算：store_id + official_settlement_status
报销：store_id + status + submitted_at
财务报表：paid_at / refunded_at / settled_at / confirmed_at / official_settlement_time
```

---

# 17. 初始化数据建议

## 17.1 默认门店

```text
store_code = DEFAULT
store_name = 默认门店
status = ENABLED
```

---

## 17.2 默认角色

```text
SUPER_ADMIN
STORE_ADMIN
FINANCE
TECHNICIAN_FRONT_DESK
```

---

## 17.3 默认权限

至少初始化：

```text
USER_MANAGE
ROLE_MANAGE
DICT_MANAGE
PART_MANAGE
INVENTORY_VIEW
INVENTORY_INBOUND
INVENTORY_ADJUST
WORK_ORDER_CREATE
WORK_ORDER_UPDATE
WORK_ORDER_SUBMIT
WORK_ORDER_CANCEL
WORK_ORDER_SETTLE
PAYMENT_RECORD
REFUND_RECORD
OFFICIAL_SETTLEMENT_MANAGE
REIMBURSEMENT_SUBMIT
REIMBURSEMENT_CONFIRM
FINANCE_VIEW
EXCEL_EXPORT
```

---

## 17.4 默认字典

### 工单状态

```text
DRAFT
PENDING_ACCEPT
ACCEPTED
PART_ORDERED
PART_ARRIVED
SETTLED
CANCELLED
```

### 支付方式

```text
WECHAT
ALIPAY
UNIONPAY
CASH
OTHER
```

### 配件来源

```text
OFFICIAL
THIRD_PARTY
```

### 库存流水类型

```text
INBOUND
RESERVE
RELEASE
CONSUME
ADJUST
```

### 官方结算状态

```text
NOT_REQUIRED
PENDING
SETTLED
```

### 报销状态

```text
PENDING
CONFIRMED
REJECTED_OR_CANCELLED
```

---

## 17.5 默认编号类型

sequence_daily 不需要预置每天的数据，但系统应支持以下 seq_type：

```text
WORK_ORDER
PAYMENT
REFUND
REIMBURSEMENT
PART_CODE
```

---

# 18. Codex migration 生成要求

后续交给 Codex 生成 migration SQL 时，必须要求：

```text
1. 基于本文档生成 MySQL 8 migration。
2. 不额外创建 MVP 暂不需要的表。
3. 所有表使用 utf8mb4。
4. 所有金额字段使用 DECIMAL。
5. 所有核心业务表包含 store_id。
6. 所有核心表包含审计字段。
7. 库存、支付、退款、官方结算、报销相关表不得简化为工单字段。
8. inventory_flow 必须能关联 inventory_stock。
9. official_after_sales 不得拆 official_settlement_record，除非 Owner 明确批准。
10. ExportRecord / ExportTask 暂不建表。
11. 必须创建 sequence_daily 表用于业务编号生成。
12. work_order 必须包含 idx_work_order_store_created(store_id, created_at) 索引。
13. sys_user 创建逻辑必须在应用层校验至少一个登录标识。
14. 必须创建 work_order_charge_item 表（非 work_order_item），支持 PART / LABOR / OTHER 三类明细。
15. work_order.labor_fee / other_fee 已废弃，仍保留字段但不再作为主要金额来源。
16. 输出完整 SQL、字段说明、索引说明、初始化数据 SQL。
17. 标出自己认为有疑问的地方，不得自行扩大范围。
```

---

# 19. Opus / Owner 复核重点

复核时重点看：

```text
1. 表是否过多或过少？
2. 是否遗漏 DRAFT 草稿状态？
3. 创建工单和提交工单是否分离？
4. 库存快照和库存流水是否足够支撑预占、释放、消耗、调整？
5. 已提交工单调整费用项目是否有数据库支撑？
6. 支付和退款是否独立建模？
7. 官方结算是否和客户支付彻底分离？
8. 报销是否只有确认后才进成本？
9. 财务报表是否能从明细汇总出来？
10. 是否提前引入了不该进入 MVP 的复杂表？
11. 是否所有核心业务表都预留了 store_id？
12. 是否有足够索引支持高频查询？
13. sequence_daily 是否能安全生成业务编号？
14. 已提交工单调整费用项目是否在事务中同步库存差异？
15. charge_type 设计是否正确？PART / LABOR / OTHER 语义是否清晰？
16. PART 类型是否能支撑库存预占、释放、扣减？
17. LABOR / OTHER 类型是否不影响库存？
18. 财务分类（配件收入、人工费收入、其他收入）是否清楚？
```

---

# 20. 当前文档状态

本文档状态：

```text
Reviewed v0.3
已将 work_order_item 表替换为 work_order_charge_item 表
已明确 charge_type = PART / LABOR / OTHER 设计
已更新财务统计和库存一致性章节
可进入 Step 6：06_ARCHITECTURE_AND_API_STYLE.md
```

本文档确认后，下一步进入：

```text
06_ARCHITECTURE_AND_API_STYLE.md
```

同时可准备给 Codex 的后续任务：

```text
Codex 任务：基于 05_DATABASE_DESIGN.md 生成 MySQL 8 migration SQL 初稿
Codex 任务：复核库存、工单、支付退款、官方结算、报销的数据库一致性风险
```
