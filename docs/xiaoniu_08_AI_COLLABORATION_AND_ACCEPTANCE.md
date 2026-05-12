# 08_AI_COLLABORATION_AND_ACCEPTANCE.md

# 小牛电动官方授权店两轮车维修售后库存管理系统 - AI 协作与验收规范文档

版本：v0.1  
日期：2026-05-10  
阶段：项目准备阶段 Step 8 / 8  
前置文档：

- `01_MVP_SCOPE.md`
- `02_BUSINESS_FLOWS.md`
- `03_CORE_BUSINESS_RULES.md` v0.2
- `04_DOMAIN_MODEL.md` v0.3
- `05_DATABASE_DESIGN.md` v0.3
- `06_ARCHITECTURE_AND_API_STYLE.md` v0.2
- `07_IMPLEMENTATION_ROADMAP.md` v0.2
- `README.md`
- `AGENTS.md`
- `CODEX.md`
- `CLAUDE.md`

用途：用于固化 Owner、ChatGPT、Codex、Claude、Opus 在本项目中的分工、任务卡格式、复核机制、验收标准、风险分级和执行放行规则。本文档是准备阶段最后一个总控文档，完成后可进入正式执行阶段。

---

## 1. 文档目的

本文档用于回答：

1. Owner、ChatGPT、Codex、Claude、Opus 分别负责什么？
2. 什么任务交给 Codex？
3. 什么任务交给 Claude？
4. 什么任务需要 Opus 复核？
5. ChatGPT 在执行阶段如何继续做总控？
6. 每个任务开始前需要哪些输入？
7. 每个任务完成后必须输出什么？
8. 什么情况必须阻塞？
9. 什么情况可以放行？
10. 如何避免 AI 擅自扩大范围？
11. 如何避免过度审查导致项目停滞？
12. Owner 如何验收每个阶段？

本文档的核心目标：

```text
让 AI 快速执行，但不能失控。
让流程足够严格，但不能吞噬项目。
```

---

## 2. 总体协作原则

本项目采用：

```text
Owner 主导
ChatGPT 总控
Codex 核心实现
Claude 简单实现与测试
Opus 复核
```

核心原则：

```text
1. Owner 是最终决策者。
2. ChatGPT 负责方案、任务拆解、风险判断和审查。
3. Codex 负责核心工程实现。
4. Claude 负责低风险实现、测试和文档。
5. Opus 负责关键文档、设计和高风险结果复核。
6. AI 不替 Owner 做最终业务决策。
7. AI 不擅自扩大 MVP 范围。
8. 高风险模块必须慢，低风险任务可以快。
```

---

## 3. Owner 角色

### 3.1 Owner 职责

Owner 负责：

```text
1. 确认业务需求。
2. 确认 MVP 范围。
3. 确认是否接受 Opus / ChatGPT 的建议。
4. 决定是否进入下一阶段。
5. 本地运行代码。
6. 进行业务验收。
7. 决定是否合并。
8. 判断门店实际使用反馈。
9. 控制项目节奏。
```

### 3.2 Owner 不需要做的事

Owner 不需要亲自承担：

```text
完整代码实现
完整 SQL 编写
完整前端页面开发
所有测试代码编写
所有文档排版
```

但 Owner 必须理解和验收：

```text
库存是否对
支付是否对
结算是否对
官方结算是否分开
报销是否按确认入成本
财务口径是否可接受
```

### 3.3 Owner 决策原则

Owner 判断时优先看：

```text
1. 是否影响主链路？
2. 是否影响库存、支付、结算、财务准确性？
3. 是否属于 MVP？
4. 是否会增加后续返工？
5. 是否当前必须做？
6. 是否可以先人工处理？
```

---

## 4. ChatGPT 角色

### 4.1 ChatGPT 职责

ChatGPT 负责：

```text
方案设计
架构分析
文档输出
任务拆解
Codex 任务卡生成
Claude 任务卡生成
Opus 反馈吸收
复核意见判断
风险分级
验收清单生成
Owner 决策辅助
```

### 4.2 ChatGPT 执行规则

ChatGPT 必须：

```text
1. 不重复询问已确认的信息。
2. 没有必须沟通确认的问题时，直接输出下一步文档或任务卡。
3. 不反复使用“如果你愿意”等弱推进表达。
4. 不在核心事实不明时假装确定。
5. 对 Codex / Claude 输出保持独立判断。
6. 明确说“可以继续”或“这里必须阻塞”。
7. 不为了谨慎而无限增加审查关卡。
8. 不因为发现 LOW / MEDIUM 问题就自动阻塞项目。
```

### 4.3 ChatGPT 主要产出

```text
准备文档
任务卡
复核摘要
验收清单
风险判断
Codex Prompt
Claude Prompt
修订后的文档
Owner 决策建议
```

### 4.4 ChatGPT 不做的事

ChatGPT 不直接：

```text
假设仓库代码事实
替 Owner 合并代码
替 Codex 执行复杂实现
让 Claude 修改核心逻辑
```

---

## 5. Codex 角色

### 5.1 Codex 定位

Codex 是本项目核心工程实现 Agent。

Codex 负责：

```text
数据库 migration
后端项目骨架
核心模块
状态机
事务一致性
库存逻辑
支付退款
官方结算
报销确认
财务统计
关键 bug 修复
核心测试
```

### 5.2 Codex 适合处理的任务

Codex 负责 HIGH 风险任务：

```text
数据库结构
migration SQL
sequence_daily 编号服务
登录认证与权限
库存入库
库存预占
库存释放
库存扣减
库存调整
工单 submit / cancel / settle
已提交工单 adjust-charge-items
支付记录
退款记录
结算判断
官方结算
报销确认
财务汇总
权限拦截
事务处理
并发控制
```

### 5.3 Codex 禁止擅自做的事

Codex 不得擅自：

```text
1. 扩大 MVP 范围。
2. 引入微服务。
3. 引入 Redis / MQ。
4. 引入复杂工作流引擎。
5. 创建文档明确暂不需要的表。
6. 把库存写成 part.stock。
7. 绕过 inventory_flow。
8. 把支付和退款塞进 work_order 字段。
9. 把官方结算写进 payment_record。
10. 用普通 update 修改工单核心状态。
11. 使用 MAX + 1 或内存计数生成业务编号。
12. 使用 double / float 表示金额。
13. 让前端决定库存或结算结果。
14. 让 LABOR / OTHER 类型费用项目参与库存预占/释放/扣减。
15. 把 work_order.labor_fee / other_fee 作为费用主要来源。
```

### 5.4 Codex 开始任务前必须读取

Codex 每次任务前必须读取：

```text
README.md
AGENTS.md
CODEX.md
当前任务卡
相关准备文档
```

如果涉及库存、工单、支付、财务，还必须读取：

```text
03_CORE_BUSINESS_RULES.md
04_DOMAIN_MODEL.md
05_DATABASE_DESIGN.md
06_ARCHITECTURE_AND_API_STYLE.md
```

---

## 6. Claude 角色

### 6.1 Claude 定位

Claude 是简单实现、页面、测试和文档辅助 Agent。

Claude 负责：

```text
前端页面
表单
列表
字段展示
基础接口对接
接口测试
单元测试补充
文档同步
低风险 bug 修复
```

### 6.2 Claude 适合处理的任务

Claude 可处理 LOW / MEDIUM 任务：

```text
README 更新
页面字段展示
表单校验
列表筛选
分页展示
状态标签
按钮可见性
接口测试
mock 数据
文档格式整理
Excel 导出按钮
简单 Controller 测试
```

### 6.3 Claude 禁止擅自处理的任务

Claude 不得擅自修改：

```text
数据库核心结构
库存预占逻辑
库存释放逻辑
库存扣减逻辑
库存调整逻辑
工单状态机
submit / cancel / settle
adjust-charge-items
支付退款结算规则
官方结算统计
财务利润口径
权限核心逻辑
事务边界
charge_type 判断与库存操作绑定逻辑
```

### 6.4 Claude 遇到核心问题时的处理方式

Claude 如果发现核心逻辑问题，不要直接重构。

必须输出：

```text
发现的问题：
影响范围：
为什么属于核心逻辑：
建议交给 Codex 的原因：
建议测试场景：
```

然后等待 Owner / ChatGPT 决定。

### 6.5 Claude 开始任务前必须读取

Claude 每次任务前必须读取：

```text
README.md
AGENTS.md
CLAUDE.md
当前任务卡
```

如果涉及业务字段或页面展示，还应读取对应模块文档或接口说明。

---

## 7. Opus 角色

### 7.1 Opus 定位

Opus 是关键文档、架构、设计和高风险结果的复核 Agent。

Opus 不直接执行代码实现。  
Opus 主要用于独立审查：

```text
文档是否自洽
是否遗漏核心业务
是否过度设计
是否违反 MVP
是否存在返工风险
是否存在库存/支付/财务漏洞
```

### 7.2 需要 Opus 复核的内容

建议 Opus 复核：

```text
准备阶段 8 个文档
数据库 migration 初稿
库存模块实现总结
工单状态机实现总结
支付退款结算实现总结
财务统计实现总结
试运行前总审查
```

### 7.3 不需要 Opus 复核的内容

通常不需要 Opus 复核：

```text
页面文案
样式调整
字段顺序
简单表单校验
普通测试补充
README 小修
```

### 7.4 Opus 反馈处理规则

Opus 反馈分级：

```text
BLOCKER：必须阻塞并修正
HIGH：进入下一任务前修正或明确处理方案
MEDIUM：记录 caveat，可进入下一步但需排期
LOW：不阻塞，按需要修订
```

ChatGPT 负责判断 Opus 反馈是否合理，并说明：

```text
采纳
部分采纳
不采纳
需要 Owner 决定
```

---

## 8. 任务分流规则

### 8.1 默认交给 Codex 的任务

```text
数据库 migration
项目骨架
登录认证
权限系统
sequence_daily 编号
库存入库
库存调整
库存预占
库存释放
库存扣减
工单提交
工单取消
工单结算
已提交工单调整配件
支付记录
退款记录
官方结算
报销确认
财务统计
高风险 bug
架构重构
```

### 8.2 默认交给 Claude 的任务

```text
文档同步
页面 UI
表单
列表
分页
筛选
字段展示
状态标签
按钮显隐
接口测试
单元测试
mock 数据
低风险 bug
导出按钮
说明文案
```

### 8.3 先问 ChatGPT 的任务

遇到以下情况先问 ChatGPT：

```text
不知道是否属于 MVP
不知道该交给 Codex 还是 Claude
涉及库存/支付/财务但看起来很小
Opus 提出不同意见
Codex 和 Claude 输出互相矛盾
任务可能扩大范围
Owner 感觉流程变复杂
```

---

## 9. 标准工作流

### 9.1 准备阶段工作流

```text
ChatGPT 输出文档
  ↓
Owner 审阅
  ↓
Opus 复核
  ↓
Owner 贴回复核报告
  ↓
ChatGPT 判断并修订
  ↓
Owner 确认进入下一步
```

当前 8 个准备文档完成后，进入执行阶段。

---

### 9.2 执行阶段工作流

```text
Owner 提出当前阶段目标
  ↓
ChatGPT 生成任务卡
  ↓
Owner 复制给 Codex / Claude
  ↓
Agent 执行
  ↓
Agent 输出变更摘要
  ↓
Owner 贴回执行结果
  ↓
ChatGPT 审查
  ↓
必要时 Opus 复核
  ↓
Owner 本地运行和验收
  ↓
通过后进入下一任务
```

---

### 9.3 高风险任务工作流

高风险任务必须走完整流程：

```text
ChatGPT 任务卡
  ↓
Codex 执行
  ↓
Codex 自查
  ↓
ChatGPT 审查
  ↓
必要时 Opus 复核
  ↓
Owner 本地运行
  ↓
主链路验收
  ↓
通过后合并
```

高风险任务包括：

```text
数据库 migration
库存
工单状态机
支付退款
结算
官方结算
报销确认
财务统计
权限核心
```

---

### 9.4 低风险任务工作流

低风险任务可简化：

```text
ChatGPT / Owner 给 Claude 小任务
  ↓
Claude 执行
  ↓
Claude 输出修改文件和测试方式
  ↓
Owner 验收
```

例如：

```text
页面字段增加
表单校验
文档同步
简单测试
```

---

## 10. 任务卡制度

### 10.1 所有执行任务必须有任务卡

任务卡必须包含：

```text
任务名称
执行 Agent
风险等级
业务目标
当前阶段
涉及模块
涉及表
涉及接口
必须遵守的规则
允许修改范围
禁止修改范围
实现要求
测试要求
验收标准
输出要求
```

### 10.2 Codex 任务卡必须包含

```text
1. 当前必须读取的文档。
2. 涉及数据库表。
3. 涉及事务边界。
4. 是否需要 migration。
5. 是否涉及库存/支付/财务。
6. 不做范围。
7. 必须输出测试结果。
```

### 10.3 Claude 任务卡必须包含

```text
1. 允许修改文件范围。
2. 禁止修改核心逻辑。
3. 是否只做页面/测试/文档。
4. 验收方式。
5. 如发现核心问题应停止并报告。
```

---

## 11. Codex 输出格式

Codex 完成任务后必须输出：

```markdown
## 任务完成摘要

说明完成了什么。

## 修改文件

- ...

## 数据库变更

- 是否有 migration
- 是否新增表
- 是否改字段
- 是否改索引
- 是否有初始化数据

## 涉及业务规则

- 是否涉及库存
- 是否涉及工单状态
- 是否涉及支付退款
- 是否涉及官方结算
- 是否涉及报销
- 是否涉及财务

## 事务与一致性

说明哪些操作用了事务，如何防止部分成功。

## 权限控制

说明新增或使用了哪些权限点。

## 测试结果

- 已执行的测试
- 未执行的测试
- 手工验收建议

## 风险与注意事项

- ...

## 未做范围

- ...
```

Codex 如果没有测试，必须明确说：

```text
未执行测试，原因是 ...
```

不得假装测试已完成。

---

## 12. Claude 输出格式

Claude 完成任务后必须输出：

```markdown
## 完成内容

- ...

## 修改文件

- ...

## 是否涉及核心规则

否 / 是

## 测试方式

- ...

## 未做内容

- ...

## 需要 Owner 检查

- ...
```

如果 Claude 任务意外涉及核心逻辑，必须停止并报告，不得继续改。

---

## 13. Opus 复核报告格式

Opus 复核建议使用：

```markdown
# 审查结论

通过 / 有条件通过 / 不通过

# 核心结论

...

# 问题列表

| 编号 | 问题 | 风险等级 | 建议 |
|---|---|---|---|

# 必须修正

...

# 可选优化

...

# 是否建议进入下一阶段

是 / 否

# 备注

...
```

风险等级：

```text
BLOCKER
HIGH
MEDIUM
LOW
```

---

## 14. 风险分级规则

## 14.1 BLOCKER

必须阻塞，不能继续：

```text
库存可能对不上
库存流水缺失
工单结算条件错误
支付退款金额可能错
官方结算混入客户支付
报销未确认就计入成本
财务利润口径错误
数据库结构无法支撑核心流程
状态机绕过业务动作
```

处理方式：

```text
必须修正
修正后重新复核
Owner 不应放行
```

---

## 14.2 HIGH

高风险，必须认真处理：

```text
缺少事务
缺少幂等控制
缺少关键权限
重复提交风险
核心索引缺失
金额精度风险
重要异常流程未处理
```

处理方式：

```text
通常进入下一步前修正
如暂不修正，必须写入明确 caveat 和后续任务
```

---

## 14.3 MEDIUM

中等风险，可以不阻塞主线：

```text
测试覆盖不足
查询条件不完整
页面操作不够顺
非核心字段遗漏
错误提示不够友好
文档表述可优化
```

处理方式：

```text
可进入下一步
记录为后续任务
不要自动增加大审查关卡
```

---

## 14.4 LOW

低风险，不阻塞：

```text
文案
样式
字段顺序
说明措辞
轻微交互体验
```

处理方式：

```text
不阻塞
需要时由 Claude 修订
```

---

## 15. 放行规则

### 15.1 可以放行的情况

满足以下条件可以进入下一步：

```text
1. 无 BLOCKER。
2. HIGH 问题已修正，或有明确可接受的处理计划。
3. 当前阶段主验收链路通过。
4. 没有破坏 MVP 边界。
5. 没有引入未经批准的复杂基础设施。
6. Owner 可以本地运行或接受当前证据。
```

### 15.2 必须阻塞的情况

出现以下任一情况必须阻塞：

```text
库存数据可能错误
支付退款金额可能错误
结算条件实现错误
工单状态可被前端绕过
官方结算和客户支付混合
报销入账条件错误
财务报表口径错误
migration 无法执行
核心测试无法通过
Codex 擅自扩大范围
Claude 修改核心逻辑
```

### 15.3 不要过度阻塞的情况

以下情况默认不阻塞：

```text
页面不好看
字段展示顺序不理想
文案需要调整
LOW 级别复核建议
非核心查询筛选不完善
测试数量还可以补充但主链路已通过
```

---

## 16. 验收层级

验收分 4 层：

```text
1. 代码自检
2. 自动测试
3. 手工业务验收
4. 真实门店试运行
```

---

## 17. 自动测试要求

### 17.1 Codex 必须覆盖的自动测试

```text
sequence_daily 并发不重复
入库增加库存并写流水
库存调整写流水
提交工单预占库存（只对 PART 且 inventory_affecting=1）
取消工单释放库存（只对 PART 且 inventory_affecting=1）
结算工单扣减库存（只对 PART 且 inventory_affecting=1）
DRAFT 阶段添加 LABOR/OTHER 费用项目不影响库存
DRAFT 阶段添加 PART 费用项目不影响库存
重复提交不重复预占
重复取消不重复释放
重复结算不重复扣减
多次付款实收正确
混合付款实收正确
退款不能超过可退金额
实收不足不能结算
官方结算不进入客户支付
报销确认后才进成本
应收金额 = Σ charge_item.line_amount
配件收入 = Σ charge_item.line_amount WHERE charge_type=PART
人工费收入 = Σ charge_item.line_amount WHERE charge_type=LABOR
其他收入 = Σ charge_item.line_amount WHERE charge_type=OTHER
配件成本 = Σ charge_item.line_cost_amount WHERE charge_type=PART
```

### 17.2 Claude 可补充的自动测试

```text
Controller 参数校验
分页查询
列表筛选
表单字段校验
错误提示
导出接口权限
简单 API 集成测试
前端组件测试
```

---

## 18. Owner 手工验收主链路

每个版本都应尽量跑通以下主链路：

```text
1. 创建配件（后减震器）。
2. 入库 10 个。
3. 查看库存 actual=10 available=10 reserved=0。
4. 创建 DRAFT 工单，添加三类费用项目：
   - 后减震器 x2（PART，inventory_affecting=1）¥350
   - 前面板喷漆（LABOR，inventory_affecting=0）¥800
   - 检测费（OTHER，inventory_affecting=0）¥120
5. DRAFT 阶段库存不变。
6. DRAFT 阶段删除/修改 PART 明细，库存仍不变。
7. DRAFT 阶段添加/修改 LABOR / OTHER 明细，库存仍不变。
8. 提交工单。
9. 查看库存 actual=10 available=8 reserved=2（仅 PART 预占）。
10. 查看 RESERVE 库存流水（只有 PART 明细对应流水）。
11. 记录客户付款 200。
12. 应收 1270（350+800+120）时不能结算。
13. 再记录客户付款 1070。
14. 结算工单。
15. 查看库存 actual=8 available=8 reserved=0。
16. 查看 CONSUME 库存流水（只有 PART 明细对应流水）。
17. 记录退款 100。
18. 工单仍 SETTLED，库存不回滚。
19. 财务：配件收入 ¥350，人工费收入 ¥800，其他收入 ¥120。
20. 财务：客户支付净收入正确。
21. 录入官方结算金额。
22. 官方结算收入单独展示。
23. 员工提交报销。
24. 待确认报销不计入成本。
25. 老板确认报销。
26. 已确认报销计入成本。
27. 导出工单、库存、财务、报销 Excel。
```

---

## 19. 模块验收清单

## 19.1 登录权限验收

```text
用户可登录
停用用户不能登录
手机号登录可用
微信登录可绑定手机号
创建用户至少有一个登录标识
角色可分配权限
无权限接口被拒绝
当前用户 store_id 不依赖前端传入
```

---

## 19.2 配件库存验收

```text
官方配件可创建
第三方配件自动生成编码
条码可查询
配件可停用
停用配件不能新工单选择
入库增加 actual 和 available
入库不增加 reserved
入库生成 INBOUND 流水
库存调整生成 ADJUST 流水
无权限不能调整库存
```

---

## 19.3 工单验收

```text
创建工单为 DRAFT，支持三类费用项目（PART / LABOR / OTHER）
DRAFT 不预占库存
DRAFT 可添加/修改/删除三类费用项目
DRAFT 删除 PART 费用项目不生成库存流水
DRAFT 删除 LABOR / OTHER 费用项目不生成库存流水
提交后状态变 PENDING_ACCEPT
提交后只对 PART 且 inventory_affecting=1 的费用项目预占库存
LABOR / OTHER 费用项目提交后不影响库存
中间状态可推进
取消后只对 PART 且 inventory_affecting=1 的费用项目释放预占
已结算不能取消
已提交工单不能普通编辑费用项目
已提交工单调整费用项目必须同步 PART 类型库存差异
应收金额 = Σ charge_item.line_amount
```

---

## 19.4 支付退款结算验收

```text
一个工单可多次付款
一个工单可混合付款
支付不自动结算
退款不删除支付
退款不能超过可退金额
实收金额 = 支付总额 - 退款总额
实收不足不能结算
结算扣减库存
重复结算不重复扣库存
已结算后退款不回滚库存和状态
```

---

## 19.5 官方售后验收

```text
官方订单号可维护
官方结算金额可手动录入
官方结算状态可标记
官方结算不进入 payment_record
官方结算不影响客户实收
官方结算收入单独统计
```

---

## 19.6 报销验收

```text
员工可提交报销
新报销状态为 PENDING
PENDING 不计入成本
老板可确认报销
CONFIRMED 计入成本
老板可驳回/取消
REJECTED_OR_CANCELLED 不计入成本
```

---

## 19.7 财务报表验收

```text
客户支付净收入正确
官方结算收入正确
配件销售收入正确
人工费收入正确
其他收入正确
配件成本正确
报销成本正确
利润 = 收入 - 成本
支付时间口径和结算时间口径分开展示
退款影响客户支付净收入
```

---

## 19.8 导出验收

```text
工单列表可导出
库存报表可导出
库存流水可导出
财务报表可导出
利润报表可导出
报销台账可导出
导出受权限控制
导出结果与查询条件一致
```

---

## 20. 文档更新规则

### 20.1 什么时候必须更新文档

以下情况必须更新文档：

```text
MVP 范围变化
数据库表结构变化
API 风格变化
核心状态流转变化
库存规则变化
支付退款规则变化
官方结算口径变化
报销入账规则变化
财务统计口径变化
AI 分工规则变化
```

### 20.2 什么时候可以不更新核心文档

以下情况不需要更新核心文档：

```text
页面样式变化
文案微调
字段展示顺序变化
非核心查询条件增加
普通 bug 修复
测试补充
```

### 20.3 文档版本规则

建议：

```text
Draft v0.x       起草
Reviewed v0.x    复核通过
Final v1.0       执行基线
```

执行阶段如有变化，使用：

```text
v1.1
v1.2
```

并记录修订说明。

---

## 21. Git 与分支规则

### 21.1 分支建议

```text
main        稳定版本
dev         日常集成
feature/*   单任务开发
fix/*       bug 修复
docs/*      文档调整
```

### 21.2 合并规则

高风险任务合并前必须：

```text
1. 通过测试。
2. 通过 Owner 验收。
3. ChatGPT 审查无 BLOCKER。
4. 必要时 Opus 复核。
```

低风险任务合并前必须：

```text
1. 本地可运行。
2. 修改范围明确。
3. 不涉及核心逻辑。
```

---

## 22. 阶段 tag 建议

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

## 23. 每日 / 每阶段工作记录建议

每次完成一个任务后，建议记录：

```text
日期
任务名称
执行 Agent
修改内容
是否通过测试
是否通过验收
遗留问题
下一步
```

可以记录在：

```text
docs/progress/PROGRESS_LOG.md
```

MVP 不强制，但建议保留。

---

## 24. 执行阶段第一批任务建议

8 个准备文档完成后，建议第一批任务不要直接写业务，而是：

```text
1. 仓库文档落位。
2. 后端项目骨架。
3. 数据库 migration 初稿。
4. sequence_daily 编号服务。
5. 登录权限与字典。
```

第一批 Codex 任务顺序：

```text
Task 1：检查/创建项目结构并落位文档
Task 2：创建 Spring Boot 基础骨架
Task 3：基于 xiaoniu_05_DATABASE_DESIGN.md v0.3 生成 MySQL migration
Task 4：实现 sequence_daily 编号服务
Task 5：实现登录、权限、字典基础
```

第一批 Claude 任务顺序：

```text
Task A：整理 README 启动说明
Task B：补充基础接口测试模板
Task C：整理默认字典和权限点说明
```

---

## 25. 首个 Codex 任务卡模板

正式执行阶段第一个 Codex 任务建议：

```markdown
# Codex 任务：仓库准备与项目文档落位

## 执行 Agent

Codex

## 风险等级

MEDIUM

## 任务目标

检查当前仓库结构，建立项目基础目录，并将 8 个准备文档、README、AGENTS、CODEX、CLAUDE 放到正确位置。不要实现业务代码。

## 必须读取

- README.md
- AGENTS.md
- CODEX.md
- CLAUDE.md
- docs/01_MVP_SCOPE.md
- docs/02_BUSINESS_FLOWS.md
- docs/03_CORE_BUSINESS_RULES.md
- docs/04_DOMAIN_MODEL.md
- docs/05_DATABASE_DESIGN.md
- docs/06_ARCHITECTURE_AND_API_STYLE.md
- docs/07_IMPLEMENTATION_ROADMAP.md
- docs/08_AI_COLLABORATION_AND_ACCEPTANCE.md

## 允许修改范围

- 文档目录
- README
- AGENTS / CODEX / CLAUDE
- 基础目录结构

## 禁止修改范围

- 不写业务代码
- 不生成 migration
- 不创建数据库表
- 不实现库存/工单/支付/财务
- 不引入 Redis/MQ/微服务

## 输出要求

1. 当前仓库结构摘要
2. 新增/移动的文件
3. 是否发现文档缺失
4. 下一步建议
```

---

## 26. 首个数据库 Codex 任务卡模板

```markdown
# Codex 任务：基于数据库设计生成 MySQL migration 初稿

## 执行 Agent

Codex

## 风险等级

HIGH

## 任务目标

基于 `docs/xiaoniu_05_DATABASE_DESIGN.md v0.3` 生成 MySQL 8 migration SQL 初稿，并初始化默认门店、角色、权限、字典。

## 必须读取

- AGENTS.md
- CODEX.md
- docs/03_CORE_BUSINESS_RULES.md
- docs/04_DOMAIN_MODEL.md
- docs/05_DATABASE_DESIGN.md
- docs/06_ARCHITECTURE_AND_API_STYLE.md

## 必须创建

- store
- sys_user
- sys_role
- sys_permission
- sys_user_role
- sys_role_permission
- sequence_daily
- sys_dict_type
- sys_dict_item
- customer
- vehicle
- part
- part_barcode
- inventory_stock
- inventory_flow
- work_order
- work_order_charge_item
- work_order_status_log
- payment_record
- refund_record
- official_after_sales
- reimbursement

## 禁止创建

- inventory_operation
- official_settlement_record
- export_task
- purchase_order
- transfer_order
- stocktake_order
- attachment
- commission_rule
- reverse_settlement

## 关键要求

1. 所有金额字段使用 DECIMAL。
2. 所有核心业务表保留 store_id。
3. 所有必要索引和唯一约束按文档创建。
4. work_order 必须包含 DRAFT 状态。
5. work_order 必须有 idx_work_order_store_created(store_id, created_at)。
6. inventory_stock 和 inventory_flow 必须支撑库存预占、释放、消耗、调整。
7. payment_record 和 refund_record 独立。
8. official_after_sales 不得混入 payment_record。
9. reimbursement 支持 PENDING / CONFIRMED / REJECTED_OR_CANCELLED。
10. sequence_daily 用于业务编号生成。

## 输出要求

1. migration 文件路径
2. 初始化数据文件路径
3. 表清单
4. 索引清单
5. 与设计文档不一致之处
6. 自查风险
7. 测试或执行结果
```

---

## 27. 当前项目治理规则

当前阶段之后，默认采用以下治理规则：

```text
1. 不再重新讨论大方向，除非 Owner 提出重大变更。
2. 不生成无任务卡的执行任务。
3. 高风险任务默认 Codex。
4. 低风险任务默认 Claude。
5. ChatGPT 负责拆解和审查。
6. Opus 负责关键节点复核。
7. Owner 负责最终验收。
8. 发现风险不自动新增流程，先判断风险等级。
9. BLOCKER 才阻塞。
10. LOW 不阻塞。
11. MEDIUM 记录后可继续。
12. HIGH 视是否影响当前主链路决定是否立即修正。
```

---

## 28. 准备阶段完成判断

当以下 8 个文档完成并复核通过后，准备阶段完成：

```text
01_MVP_SCOPE.md
02_BUSINESS_FLOWS.md
03_CORE_BUSINESS_RULES.md
04_DOMAIN_MODEL.md
05_DATABASE_DESIGN.md
06_ARCHITECTURE_AND_API_STYLE.md
07_IMPLEMENTATION_ROADMAP.md
08_AI_COLLABORATION_AND_ACCEPTANCE.md
```

准备阶段完成后，不代表没有复杂问题，而是代表：

```text
方向问题已收敛
边界问题已收敛
核心规则已收敛
数据库方向已收敛
架构方向已收敛
执行路线已收敛
AI 分工已收敛
```

后续复杂度转为：

```text
按任务卡实现
按验收清单测试
按风险等级复核
```

---

## 29. 当前文档状态

本文档状态：

```text
Reviewed v0.1
已通过 Opus 局部复核
可进入执行阶段
```

复核重点：

```text
1. Owner / ChatGPT / Codex / Claude / Opus 分工是否清楚？
2. 任务分流规则是否可执行？
3. 高风险任务是否都交给 Codex？
4. Claude 禁止范围是否明确？
5. 风险分级是否合理？
6. 放行与阻塞规则是否能避免过度审查？
7. 验收清单是否覆盖核心业务？
8. 是否足够支撑正式进入执行阶段？
9. 工单费用项目模型升级后，Codex/Claude 禁止范围是否更新？
10. 自动测试和手工验收是否覆盖 PART/LABOR/OTHER 三类费用项目？
```

本文档确认后，准备阶段结束，可进入正式执行阶段。
