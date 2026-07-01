# 客户侧预约排号系统模块分析

## 1. 结论

### 1.1 推荐方案

**客户侧预约排号系统建议新增 Maven 模块，但仍运行在当前 Spring Boot 模块化单体内。**

推荐链路：

```text
客户预约 / 到店签到 / 排号
  ↓
员工叫号 / 接待确认
  ↓
预约转工单 DRAFT
  ↓
员工提交工单
  ↓
进入现有工单、库存、支付、退款、官方结算、财务链路
```

### 1.2 不推荐方案

不建议让客户侧直接调用 `/api/staff/**`，也不建议客户直接创建并提交正式工单。

原因：

- 客户不是员工，不能进入 `sys_user`、角色、权限体系。
- 预约是到店前意向，不应触发库存预占。
- 支付、退款、官方结算、财务口径必须继续由现有工单链路承接。
- 客户端字段、权限和脱敏要求明显不同于员工端和管理端。

## 2. 业务边界

### 2.1 客户侧可做

1. 选择门店。
2. 选择服务类型：看车、维修、售后、检测。
3. 选择预约日期和时间段。
4. 提交客户姓名、手机号、车型、车架号、问题描述。
5. 到店签到。
6. 查看排队状态。
7. 查看本人预约历史和脱敏服务进度。
8. 取消本人未接待预约。

### 2.2 客户侧禁止直接做

1. 直接提交工单。
2. 直接预占库存。
3. 直接扣减库存。
4. 直接记录收款。
5. 直接记录退款。
6. 直接交付关闭工单。
7. 直接操作官方结算。
8. 直接进入财务统计。

## 3. 领域模型

### 3.1 新增领域

| 领域 | 职责 | 是否影响现有核心链路 |
|---|---|---:|
| `client` | 客户微信身份、客户本人资料 | 否 |
| `appointment` | 预约、签到、排队、叫号、接待 | 间接影响 |
| `workorder` | 工单草稿、提交、维修完成、交付关闭 | 是 |
| `inventory` | 库存预占、释放、正式扣减 | 是 |
| `payment` | 支付、退款 | 是 |

### 3.2 预约状态

| 状态 | 含义 | 允许操作 |
|---|---|---|
| `BOOKED` | 已预约，未到店 | 取消、改期、签到 |
| `CHECKED_IN` | 已签到，排队中 | 叫号、过号、取消 |
| `SERVING` | 接待中 | 转工单、关闭 |
| `CONVERTED` | 已转工单 | 查看关联工单 |
| `NO_SHOW` | 爽约 | 终态 |
| `CANCELLED` | 已取消 | 终态 |
| `CLOSED` | 无需工单关闭 | 终态 |

## 4. Maven 模块建议

### 4.1 推荐模块结构

```text
backend-parent
  ├── aftermarket-core
  │   ├── common
  │   ├── auth
  │   ├── store
  │   ├── user
  │   ├── customer
  │   ├── workorder
  │   ├── inventory
  │   ├── payment
  │   ├── official
  │   ├── reimbursement
  │   ├── finance
  │   └── funding
  ├── client-appointment
  │   ├── client
  │   └── appointment
  └── aftermarket-boot
      └── Spring Boot 启动类与配置
```

### 4.2 当前仓库的现实落地

当前 `backend` 是单 Maven module。为了减少一次性重构风险，建议分两步：

1. **阶段 A：包级模块化**
   - 在现有 `backend` 内新增 `client` 和 `appointment` 包。
   - 先完成表、实体、Service、Controller、测试。
   - 不调整 Maven 父子结构。

2. **阶段 B：Maven 模块拆分**
   - 等预约域稳定后，把 `client` 和 `appointment` 移入 `client-appointment` module。
   - 再把启动层拆成 `aftermarket-boot`。
   - 通过 Maven 依赖控制方向，防止预约域反向污染核心域。

### 4.3 依赖方向

推荐依赖方向：

```text
client-appointment
  → common
  → store
  → customer
  → workorder application port

workorder / inventory / payment / finance
  不依赖 client-appointment
```

关键约束：

- `appointment` 可以保存 `converted_work_order_id`。
- `workorder` 不应反向依赖 `appointment`。
- 转工单动作由 `appointment` 调用 `workorder` 暴露的应用服务。
- 预约域不调用库存 Service。
- 预约域不调用支付/退款 Service。

## 5. 数据库设计建议

### 5.1 `client_user`

客户侧身份表，不能复用 `sys_user`。

关键字段：

```text
id
wechat_openid
wechat_unionid
phone
nickname
status
last_login_at
created_at
updated_at
deleted
```

### 5.2 `service_appointment`

预约主表。

关键字段：

```text
id
store_id
appointment_no
queue_no
client_user_id
customer_id
vehicle_id
customer_name_snapshot
customer_phone_snapshot
vehicle_model_snapshot
frame_no_snapshot
service_type
problem_description
scheduled_date
scheduled_start_time
scheduled_end_time
status
checked_in_at
served_at
converted_work_order_id
cancel_reason
created_by
created_at
updated_by
updated_at
deleted
```

### 5.3 `appointment_status_log`

预约状态日志。

关键字段：

```text
id
store_id
appointment_id
from_status
to_status
operator_type
operator_id
operated_at
remark
created_at
```

### 5.4 `appointment_capacity_rule`

门店预约容量配置。

关键字段：

```text
id
store_id
weekday
start_time
end_time
service_type
capacity
status
created_at
updated_at
deleted
```

## 6. API 设计建议

### 6.1 客户侧 API

| 接口 | 用途 | 影响核心链路 |
|---|---|---:|
| `POST /api/client/auth/wechat-login` | 客户微信登录 | 否 |
| `GET /api/client/stores` | 查询可预约门店 | 否 |
| `GET /api/client/appointment-slots` | 查询可预约时段 | 否 |
| `POST /api/client/appointments` | 创建预约 | 否 |
| `GET /api/client/appointments` | 我的预约 | 否 |
| `GET /api/client/appointments/{id}` | 预约详情 | 否 |
| `POST /api/client/appointments/{id}/check-in` | 到店签到 | 否 |
| `POST /api/client/appointments/{id}/cancel` | 取消预约 | 否 |

### 6.2 员工侧 API

| 接口 | 用途 | 影响核心链路 |
|---|---|---:|
| `GET /api/staff/appointments/today` | 今日队列 | 否 |
| `POST /api/staff/appointments/{id}/call` | 叫号 | 否 |
| `POST /api/staff/appointments/{id}/skip` | 过号 | 否 |
| `POST /api/staff/appointments/{id}/serve` | 开始接待 | 否 |
| `POST /api/staff/appointments/{id}/convert-work-order` | 转工单草稿 | 是，创建 DRAFT |
| `POST /api/staff/appointments/{id}/close` | 无需工单关闭 | 否 |

### 6.3 管理端 API

| 接口 | 用途 | 影响核心链路 |
|---|---|---:|
| `GET /api/admin/appointments` | 预约列表 | 否 |
| `GET /api/admin/appointments/{id}` | 预约详情 | 否 |
| `PUT /api/admin/appointment-capacity-rules` | 配置预约容量 | 否 |
| `GET /api/admin/appointment-reports/daily` | 预约日报 | 否 |

## 7. 测试重点

### 7.1 必测场景

1. 客户只能查看自己的预约。
2. 客户不能跨门店查看预约。
3. 同一门店同一日期排队号递增。
4. 同一预约不能重复转工单。
5. 预约转工单后工单为 `DRAFT`。
6. 转工单不触发库存预占。
7. 工单提交后才触发库存预占。
8. 客户取消已接待预约应被拒绝。
9. 爽约和取消不影响库存、支付、退款和财务。

### 7.2 不回归即风险

- 库存预占、释放、扣减。
- 支付和退款金额计算。
- 工单状态流转。
- 官方结算和客户支付分开统计。
- 财务报表口径。
- 门店数据隔离。

## 8. 实施顺序

1. 更新项目文档和边界说明。
2. 新增预约域数据库 migration。
3. 新增 `client_user` 和客户微信登录。
4. 新增客户预约创建、查询、取消。
5. 新增员工今日队列、签到、叫号、过号。
6. 新增预约转工单 DRAFT。
7. 新增管理端预约列表和预约容量配置。
8. 新增客户侧小程序页面。
9. 做全链路回归测试。

## 9. 风险结论

客户侧预约排号是合理新增能力，但属于新业务域，不能塞进现有员工小程序和员工权限体系。最稳的技术路线是先在当前单体中新增清晰包边界，再按 Maven 模块拆分；最稳的业务路线是预约只到接待层，转工单必须由员工确认。
