# 架构护栏

本文件用于防止项目在 AI 快速开发过程中变复杂、变散、变不可控。

## 1. 架构选择

当前架构：

```text
微信小程序 + Vue 管理端 + Spring Boot 后端 + MySQL
```

后端采用：

```text
模块化单体 Modular Monolith
```

## 2. 当前不采用

MVP 阶段不采用：

- 微服务
- 分布式事务
- MQ
- Redis
- Elasticsearch
- 大型工作流引擎
- 低代码平台
- 多租户复杂架构
- 数据中台
- BI 平台
- 自动采购系统
- 官方系统同步

## 3. 后端模块边界

建议模块：

```text
auth
user
dict
workorder
inventory
payment
official
reimbursement
finance
export
common
```

### workorder

只负责工单主流程和状态。

不直接负责库存数量修改，库存变化应通过 inventory 服务完成。

### inventory

负责所有库存变动。

任何库存变化都必须经过 inventory 服务，不能由其他模块直接改库存表。

### payment

负责支付和退款记录。

不直接改变库存。

### official

负责官方售后订单号和官方结算金额。

不混入客户支付。

### reimbursement

负责报销提交、确认、驳回。

只有已确认报销进入财务成本。

### finance

负责汇总查询。

尽量从明细表计算，不直接维护可随意编辑的利润字段。

## 4. 数据库设计护栏

### 4.1 所有业务表建议字段

```text
id
store_id
created_by
created_at
updated_by
updated_at
deleted
remark
```

### 4.2 金额字段

所有金额字段使用：

```text
DECIMAL(18,2)
```

Java 使用：

```text
BigDecimal
```

禁止：

```text
double
float
```

### 4.3 状态字段

状态字段必须统一管理。

建议：

```text
status varchar(32)
```

禁止魔法数字散落各处。

### 4.4 库存字段

库存表建议：

```text
actual_qty
available_qty
reserved_qty
```

库存流水表必须存在。

## 5. API 设计护栏

API 要区分：

```text
查询类
命令类
```

示例：

```text
GET    /api/work-orders
POST   /api/work-orders
POST   /api/work-orders/{id}/submit
POST   /api/work-orders/{id}/cancel
POST   /api/work-orders/{id}/mark-repair-done
POST   /api/work-orders/{id}/deliver
POST   /api/payments
POST   /api/refunds
POST   /api/inventory/inbound
POST   /api/inventory/adjust
```

关键状态变化不要用普通 update 接口模糊处理。

例如：

```text
不要：PUT /api/work-orders/{id} 直接改 status=REPAIR_DONE 或 DELIVERED
应该：POST /api/work-orders/{id}/mark-repair-done 或 POST /api/work-orders/{id}/deliver
```

## 6. 事务护栏

必须使用事务的场景：

- 入库 + 库存流水
- 提交工单 + 预占库存 + 流水
- 取消工单 + 释放库存 + 流水
- 标记维修完成 + 扣减库存 + 状态变化
- 交付关闭 + 状态变化
- 退款记录 + 实收金额变化相关校验
- 报销确认 + 成本统计相关状态变化

## 7. 幂等护栏

以下动作需要防止重复执行：

- 工单提交
- 工单取消
- 标记维修完成
- 交付关闭
- 库存扣减
- 退款记录提交
- 报销确认

至少要通过状态前置校验避免重复操作。

## 8. 前端护栏

前端不得自己计算最终财务结果作为权威数据。

前端可以展示计算结果，但后端是唯一可信来源。

前端不得通过直接传 status 来绕过业务动作接口。

## 9. 小程序护栏

小程序主要用于员工操作：

- 创建工单
- 查询库存
- 扫码/手动入库
- 工单处理
- 客户结算
- 报销提交

小程序不负责复杂财务管理和权限配置。

## 10. 管理端护栏

管理端主要用于：

- 库存管理
- 工单管理
- 财务报表
- 报销确认
- 权限管理
- Excel 导出

## 11. 过度设计警戒线

如果某个设计需要解释很多未来场景，且当前 MVP 用不上，默认先不做。

判断标准：

```text
现在不用
门店当前不会操作
没有真实数据来源
不影响主链路
可以人工处理
```

满足以上多数条件，就先不要实现。
