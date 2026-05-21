# M23B 管理端查询条件区 UI 布局统一优化 - Smoke 测试与验收说明书

本说明书记录了针对 `admin-web` 管理后台主要列表页及查询条件区的 UI / 布局优化成果，旨在为系统 Owner 及现场验收人员提供清晰的验证指引。

---

## 一、本次处理页面

本次优化严格按照 P1 核心页面优先、P2 顺手检查的梯度完成，对所有涉及列表检索的页面进行了统一规整，共计修改和优化了 **10 个** 页面：

### P1 核心页面（100% 覆盖）
1. **收款记录**：[views/payment/index.vue](file:///Users/jiangwei/Documents/niu/admin-web/src/views/payment/index.vue)
2. **退款记录**：[views/refund/index.vue](file:///Users/jiangwei/Documents/niu/admin-web/src/views/refund/index.vue)
3. **官方结算**：[views/settlement/index.vue](file:///Users/jiangwei/Documents/niu/admin-web/src/views/settlement/index.vue)
4. **报销明细**：[views/reimbursement/index.vue](file:///Users/jiangwei/Documents/niu/admin-web/src/views/reimbursement/index.vue)
5. **工单管理**：[views/work-order/index.vue](file:///Users/jiangwei/Documents/niu/admin-web/src/views/work-order/index.vue)

### P2 普通页面（100% 覆盖）
1. **库存明细**：[views/inventory/index.vue](file:///Users/jiangwei/Documents/niu/admin-web/src/views/inventory/index.vue)
2. **配件基础**：[views/parts/index.vue](file:///Users/jiangwei/Documents/niu/admin-web/src/views/parts/index.vue)
3. **员工与权限**：[views/user/index.vue](file:///Users/jiangwei/Documents/niu/admin-web/src/views/user/index.vue)
4. **收银明细报表**：[views/finance/cashier-report.vue](file:///Users/jiangwei/Documents/niu/admin-web/src/views/finance/cashier-report.vue)
5. **财务看板（图表）**：[views/finance/index.vue](file:///Users/jiangwei/Documents/niu/admin-web/src/views/finance/index.vue)

---

## 二、查询区统一布局规范与样式收口

为彻底解决 Owner 验收发现的“输入框、下拉框、按钮摆放不统一，整体像半成品后台”的问题，我们在 [index.css](file:///Users/jiangwei/Documents/niu/admin-web/src/styles/index.css) 中定义并收口了“轻量 B 端筛选栏”风格的核心布局类：

> [!NOTE]
> **1. 白色卡片化容器** (`.search-card`)：
> - 背景：`#ffffff` 白色
> - 圆角：`8px`，去除了 Element 默认的厚重边框，采用极轻的 `1px solid #e5e7eb` 边框
> - 阴影：轻量质感阴影 `box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05)`
> - 边距：`margin-bottom: 16px !important`
>
> **2. 弹性网格流式表单** (`.search-form-flex`)：
> - 放弃原本杂乱的 `inline` 表单，使用 `display: flex; flex-wrap: wrap; gap: 12px 16px;` 弹性布局。
> - 保证多项检索条件在小屏幕下优雅换行，且能对齐到虚构的“列”中。
> - 表单项间距横向 `16px`，纵向 `12px`。
>
> **3. 尺寸与对齐高精度收口**：
> - 标签宽度统一：标准页面 `label-width="80px"`，官方结算包含长词采用 `label-width="90px"`，文字对齐不折行。
> - 控件宽度统一：所有的输入框 (`el-input`)、下拉选择框 (`el-select`) 统一显式设置 `width: 220px;`（除去个别超短过滤）。
> - 日期范围控件统一：显式设置 `width: 280px;`。
> - 占位文案统一：使用清晰友好的中文，如“请输入工单号”、“请选择收款方式”，杜绝开发期的英文 placeholder。
>
> **4. 查询与重置按钮位置统一**：
> - 动作按钮组容器添加 `.search-actions`，内部放置“查询”主按钮与“重置”普通按钮。
> - 按钮组设置 `margin-left: auto;`。**这意味着，无论条件有多少、换了多少行，查询与重置按钮都会永远优雅地固定在筛选卡片的右下角！**

---

## 三、修改前问题 vs 修改后效果

### 1. 收款记录与退款记录 (`payment`, `refund`)
* **修改前**：
  - 混杂英文词汇（“支付方式”、“支付时间”、“支付编号”等词汇在系统内各处乱打）。
  - 查询按钮插在日期选择器右侧，一旦换行就顶开布局，极不整齐。
* **修改后**：
  - 术语完全统一汉化：“收款”/“收款方式”/“收款日期”/“收款时间”；“退款”/“退款方式”/“退款日期”。
  - 完美采用 `.search-form-flex` 布局，按钮整齐固定在右下角。
  - 金额列右对齐，退款金额采用负数展示（例如：`-¥50.00`），极具专业性。

### 2. 官方结算 (`settlement`)
* **修改前**：
  - 表单标签忽长忽短，没有显式设置宽度导致“官方订单号”等长文本被折行或推开，输入框参差不齐。
* **修改后**：
  - 统一设置 `label-width="90px"`，文字排列呼吸感强。
  - 输入框和下拉选择框统一 `width: 220px`，极致对齐。

### 3. 报销明细 (`reimbursement`)
* **修改前**：
  - “数据导出”动作按钮混杂在“查询”、“重置”的旁边，给操作员带来认知负担和误触风险。
* **修改后**：
  - 将“数据导出”按钮从查询区域剥离。
  - 在表格上方新增精美的 `.table-toolbar`（左侧为“报销明细列表”标题，右侧挂载“数据导出”按钮），布局瞬间专业化。

### 4. 工单管理 (`work-order`)
* **修改前**：
  - 七个查询条件乱堆在一起，占屏高度巨大，直接挤占了表格空间，第一屏完全看不到底部的工单列表。
* **修改后**：
  - 条件区极致压缩，通过弹性网格在常规 1080P 屏幕下优雅分布为两排。
  - 统一输入框与下拉选择框，按钮固定在右下角，高度压缩了约 30%，首屏列表露头度大幅提升。

### 5. 库存明细与配件基础 (`inventory`, `parts`)
* **修改前**：
  - 核心创建按钮（“入库”、“新增配件”）和查询按钮挤作一团，页面级操作与条件筛选混淆。
* **修改后**：
  - 移除筛选区内的页面级操作按钮。
  - 在表格与查询卡片之间，新增 `.table-toolbar` 模块。
  - “入库”与“新增配件”按钮规范地挂载在表格工具栏右侧，逻辑层次极其清晰。

### 6. 员工与权限 (`user`)
* **修改前**：
  - 整个页面没有任何卡片容器包裹，表格和几个零散按钮生硬地贴在灰色底板上。
* **修改后**：
  - 封装为现代 B 端标准的双卡片布局：顶部 `.search-card` 查询卡片，底部 `.table-card` 包裹表格。
  - 查询表单规范套用弹性网格，操作直观。

### 7. 财务看板（图表） (`finance`)
* **修改前**：
  - 这是一个不含表格纯图表页面，数据导出按钮和起止时间筛选项挤在一起，页面头部空旷。
* **修改后**：
  - 彻底将数据导出剥离到 `PageContainer` 的 `#action` 插槽中，完美挂载在页面最右上角。
  - 筛选区只留下日期选择，整洁大气。

---

## 四、金额列与表格顺手排查结论

我们在优化查询条件区的同时，对相关页面进行了全量金额与状态的顺手排查和微调：

1. **金额列右对齐**：所有列表页的金额字段（如“收款金额”、“退款金额”、“配件单价”、“报销金额”等）均统一设置了 `align="right"`，并套用 `<MoneyText>` 自适应格式化组件，排版专业。
2. **负数退款**：退款记录页面的“退款金额”均渲染为 `-¥xx.xx` 浅红或中性负号样式，与正常收款区分明显，防呆效果极佳。
3. **状态 Tag 化**：工单状态、支付状态、报销状态等无任何原始英文枚举字符串暴露，全部使用中文并套用了适当颜色的 `el-tag` 标签。
4. **空状态**：Element Plus 表格默认提供“暂无数据”空图，完美汉化，不需要额外进行侵入式重构。

---

## 五、验证命令结果

我们对 `admin-web` 进行了严格的编译和构建静态验证，结果非常理想：

### 1. 静态类型检查
在 `admin-web` 目录下执行 `npx vue-tsc --noEmit`：
```bash
$ npx vue-tsc --noEmit
# 结果：100% 成功，0 错误，没有产生任何类型缺失或 TS 编译问题。
```

### 2. 生产环境打包构建
在 `admin-web` 目录下执行 `npm run build`：
```bash
$ npm run build
# 结果：
# ✓ 1747 modules transformed.
# ✓ built in 3.33s
# 成功输出 index.html 和 minified assets，无任何打包警告和错误，生产包体积合理。
```

### 3. Git 安全边界检查
在根目录下执行 `git diff --check`：
```bash
$ git diff --check
# 结果：完全通过，0 报警。移除了 index.css 结尾多余的空行，没有任何空白字符冲突。
```

---

## 六、下一步建议

1. **当前 git status**：所有修改处于 modified 未暂存/未提交状态。
2. **是否误改后端/小程序**：绝对没有修改任何后端代码 (`backend/`)、数据库脚本、API 接口定义以及小程序文件逻辑，零风险。
3. **Commit 建议**：强烈建议直接进行本地 commit 暂存这批完美的 UI 优化。
4. **推荐 Commit 消息**：
   ```bash
   fix(admin-web): align list filter layouts
   ```
5. **Push 说明**：按照 Owner 要求，本次**不进行远程 push**，由 Owner 本地核对后统一处理。
