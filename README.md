# 小牛电动官方授权店维修售后库存管理系统

> 本仓库用于开发“小牛电动官方授权店两轮车维修售后库存管理系统”。
> 当前目标是完成单门店 MVP：维修工单、配件库存、支付退款、官方售后结算、报销台账、财务报表和 Excel 导出。

## 1. 项目定位

本系统面向小牛电动官方授权售后门店，同时兼容第三方维修业务。

核心目标：

- 规范维修工单流程
- 管理官方/第三方配件库存
- 支持扫码/手动入库
- 支持工单提交时库存预占
- 支持结算后正式扣减库存
- 支持多次付款、混合付款、退款记录
- 支持官方售后结算金额手动录入
- 支持员工报销台账
- 支持基础财务报表和 Excel 导出
- 为未来多门店预留 `store_id`

## 2. MVP 范围

### 2.1 当前要做

- 单门店
- 员工登录
- 角色与权限
- 字典配置
- 配件管理
- 官方配件 / 第三方配件
- 第三方配件编码自动生成
- 配件扫码 / 手动入库
- 工单创建、提交、取消、结算
- 工单临时配件入库
- 库存预占、释放、扣减、调整
- 库存流水
- 支付记录
- 退款记录
- 官方售后订单号备注
- 官方结算金额手动录入
- 报销台账
- 基础财务统计
- Excel 导出
- 管理端
- 员工小程序

### 2.2 当前不做

- 客户端小程序
- 员工提成
- 客户签字
- 图片上传
- 自动采购
- 同步官方系统
- 自动打款
- 多门店正式运营
- 复杂审批流
- 复杂盘点流程
- 复杂 BI 系统
- 微服务
- Redis / MQ 等非必要基础设施

## 3. 技术栈

### 后端

- Java 17 或 Java 21
- Spring Boot 3.x
- MyBatis / MyBatis-Plus
- MySQL 8
- EasyExcel
- JWT / Sa-Token / Spring Security 之一

### 管理端

- Vue 3
- Element Plus
- TypeScript 可选

### 小程序

- 微信原生小程序优先
- 如未来明确多端需求，再考虑 Taro

### 部署

- 单机部署优先
- Docker Compose 可选
- Nginx + Spring Boot + MySQL

## 4. 架构原则

本项目采用：

> 模块化单体 Modular Monolith

不要在 MVP 阶段引入微服务、消息队列、复杂工作流引擎或过度平台化设计。

推荐后端模块：

```text
auth              登录、微信绑定、手机号、token
user              员工、角色、权限
dict              字典配置
workorder         工单主流程
inventory         配件、库存、入库、预占、扣减、调整
payment           支付记录、退款记录、结算判断
official          官方售后订单、官方结算金额
reimbursement     报销台账
finance           财务统计、利润报表
export            Excel 导出
common            通用异常、审计字段、分页、工具类
```

## 5. AI 协作分工

本项目由一个人主导开发，AI 作为辅助开发团队。

### Owner / 人类负责人

负责：

- MVP 范围判断
- 业务规则最终裁决
- 数据模型审查
- 状态流转审查
- 代码运行与验收
- 是否合并代码的最终决定

### ChatGPT

负责：

- 方案设计
- 架构分析
- 任务拆解
- Prompt / Task Card 生成
- 代码审查建议
- 测试场景设计
- 风险分析
- 文档维护建议

ChatGPT 默认不直接假设代码事实。涉及代码实现状态时，需要以仓库实际内容为准。

### Codex

负责：

- 核心模块实现
- 数据库设计和迁移
- 库存/支付/工单/财务等高风险逻辑
- 架构重构
- 关键 bug 修复
- 状态机和事务一致性
- 后端核心测试

详见 `CODEX.md`。

### Claude

负责：

- 简单、边界清晰、可回滚的实现
- 页面小改动
- 表单、列表、字段展示
- 简单接口对接
- 单元测试、集成测试、测试数据
- 文档同步
- 不触碰核心状态机和关键财务逻辑

详见 `CLAUDE.md`。

## 6. 开发流程

标准流程：

```text
需求确认
  ↓
ChatGPT 输出方案 / 任务卡
  ↓
Owner 审查并决定
  ↓
Codex 处理核心任务
  ↓
Claude 处理简单实现和测试
  ↓
Owner 本地运行
  ↓
按验收清单测试
  ↓
合并
```

禁止流程：

```text
PRD 未确认 → AI 直接全量生成系统
数据库未确认 → AI 直接写业务代码
库存/支付/财务未审查 → 直接合并
Claude 自行改核心状态机
Codex 自行扩展 MVP 以外能力
```

## 7. 优先级

推荐阶段：

1. 项目骨架
2. 登录、角色、权限、字典
3. 配件与库存
4. 工单主流程
5. 支付、退款、结算
6. 官方售后结算
7. 报销台账
8. 财务报表与 Excel 导出
9. 小程序体验完善
  - M1/M2/M3/M4: 草稿创建与费用明细 (已支持)
  - M5B: 已支持 Staff 提交工单（触发后端库存预占 RESERVE 流水）。
  - M6B: 已支持 Staff 取消工单。DRAFT 取消不释放库存，已提交未结算取消会由后端释放预占库存（RELEASE 流水）。
  - M7B: 已支持 Staff 记录支付。当前记录的是线下/人工确认后的支付结果，不接真实支付网关，且支付动作不会自动结算工单。
  - M8B: 已支持 Staff 记录退款。前端只负责登记金额及退款明细状态，一切越界和可退上限由后端强制把关且不产生任何库存/结算的流转异动。
  - M9B: 已支持 Staff 结算工单。前端仅发起动作和交互提醒，真实的资金实收校验与库存 CONSUME 扣减行为完全由后端代持结算，前端不伪造状态。
  - M10A: Staff 主链路回归 smoke / 断点检查已完成。433 tests passed，8 步主链路全部验证通过。
  - M11A: 后端官方售后结算补强完成。新增 officialOrderNo 门店维度唯一约束、金额校验、状态校验、跨店隔离测试。445 tests passed。
  - M11B: admin-web 官方售后结算真实接口对接完成。列表/详情/录入订单号/标记结算/标记无需结算均已对接后端真实 API。
  - M12A: 后端报销台账最小实现完成。Staff 可提交 PENDING 报销，Admin 可分页/详情/确认/驳回；仅 CONFIRMED 计入成本语义，不影响工单、库存、支付、退款或官方结算。
  - M12B: 小程序 Staff 提交报销页面完成。仅提交 purpose/amount/remark，表单校验 + submitLoading 防重复，不涉确认/驳回/审批流。
  - M12C: admin-web 报销台账真实接口对接完成。列表/详情/确认/驳回均对接后端 API，PENDING 显示操作按钮，已终态隐匿；确认校验金额>0，驳回校验原因必填。
    - M13A: 后端财务基础聚合查询完成。日报/月报/时间范围查询，汇总客户收入(实收-退款)、官方结算收入、配件成本(工单消耗)、已确认报销成本，计算利润=收入-成本。480 tests passed。
    - M13B: admin-web 财务报表真实接口对接完成。日报/月报/自定义范围查询均对接后端 API，金额 2 位小数，口径说明展示，无 mock/Excel/BI。
    - M13C: 业务闭环端到端断点检查通过。14 项口径验证无断点，后端 480 tests 全通过，前端 build 成功，finance 类型 0 错误。
    - M14A: 后端 Excel 导出最小能力完成。新增财务报表与报销台账同步 `.xlsx` 下载接口；只读导出，不建导出任务表，不改财务/报销/库存/工单口径。
10. 真实门店试运行

## 8. 重要文档

- `AGENTS.md`：所有 AI Agent 通用规则
- `CODEX.md`：Codex 工作规范
- `CLAUDE.md`：Claude 工作规范
- `docs/xiaoniu_01_MVP_SCOPE.md`：准备阶段 Step 1，MVP 范围冻结文档
- `docs/xiaoniu_02_BUSINESS_FLOWS.md`：准备阶段 Step 2，业务流程梳理文档
- `docs/xiaoniu_03_CORE_BUSINESS_RULES.md`：准备阶段 Step 3，核心业务规则文档
- `docs/xiaoniu_04_DOMAIN_MODEL.md`：准备阶段 Step 4，领域模型设计文档
- `docs/xiaoniu_05_DATABASE_DESIGN.md`：准备阶段 Step 5，数据库设计文档
- `docs/xiaoniu_06_ARCHITECTURE_AND_API_STYLE.md`：准备阶段 Step 6，架构与 API 风格文档
- `docs/xiaoniu_07_IMPLEMENTATION_ROADMAP.md`：准备阶段 Step 7，实施路线与任务拆解文档
- `docs/xiaoniu_08_AI_COLLABORATION_AND_ACCEPTANCE.md`：准备阶段 Step 8，AI 协作与验收规范文档
- `docs/AI_WORKFLOW.md`：Owner + AI 协作流程
- `docs/MVP_SCOPE.md`：MVP 范围
- `docs/PROJECT_RULES.md`：业务核心规则
- `docs/TASK_CARD_TEMPLATE.md`：任务卡模板
- `docs/ACCEPTANCE_CHECKLIST.md`：验收清单
- `docs/ARCHITECTURE_GUARDRAILS.md`：架构护栏

## 9. 当前最重要的工程原则

- 库存必须有流水
- 支付和退款必须有明细
- 官方结算和客户支付必须分开统计
- 金额必须使用 `BigDecimal`
- 状态流转必须校验前置状态
- 所有业务表预留 `store_id`
- 禁止把可配置项写死在代码里
- MVP 不做过度基础设施
- AI 不能擅自扩大范围

## 10. 后端启动

后端工程位于 `backend/`，当前为 Spring Boot 基础骨架。

```bash
cd backend
mvn test
mvn package -DskipTests
mvn spring-boot:run
curl http://localhost:8080/api/health
```

`application-dev.yml` 默认读取 MySQL 环境变量：

```bash
MYSQL_URL
MYSQL_USERNAME
MYSQL_PASSWORD
```

后端已有 Flyway 数据库迁移（V1-V7 业务表/增量 + V5 Staff seed 数据），测试环境使用 H2 内存数据库。
