# Admin Web Excel 导出对接 (M14B) Smoke Test 报告

## 环境配置
- **端**: admin-web
- **API_MODE**: `real` (对接后端本地开发环境)
- **请求头模拟**: `X-User-Id` 和 `X-Store-Id` 等请求头由全局的 `request` 配置拦截自动附带。

## 测试目标
验证 admin-web 财务报表及报销台账页面的导出功能，确保它们能够真实连接到后端所提供的原生 Excel (`.xlsx`) 导出接口，并在不违背边界原则（如异步轮询、权限等大重构）的前提下完成文件同步下载闭环。

## 测试步骤与验证结果

### 1. 拦截器与基础底层兼容验证
- **场景**: 触发任何一笔导出。
- **结果**:
  - [x] 修改了 `src/utils/request.ts` 以正确放行 `Blob` 格式响应体，避免了将其强制视为业务报错 JSON 进行截断。
  - [x] 后端发生业务错误时的“被包装为 Blob 的 JSON”（如 500、400 时报错信息）能在 `src/api/export.ts` 中被妥善重新解析为纯文本，并抛弃为正确的 `ElMessage` 报错，而非下载“坏的乱码文件”。
  - [x] 从 `Content-Disposition` 读取文件名的解析逻辑通过，支持降级 `finance_report.xlsx`。

### 2. 财务报表导出功能验证
- **场景**: 在“财务报表”页面，切换不同的报表模式进行导出。
- **结果**:
  - [x] **日报导出**: 选择日报模式后点击导出，参数为 `reportType=DAILY` 与 `date`，成功下载文件。
  - [x] **月报导出**: 切换至月报模式并选择月份，点击导出，参数为 `reportType=MONTHLY` 与 `month`，成功下载文件。
  - [x] **范围查询导出**: 切换至自定义模式并选取 7 天跨度，前端映射为后端约定的 `reportType=RANGE` 与 `startDate`/`endDate`，成功下载文件。
  - [x] 在等待导出生成过程中，Button 展示 `loading` 状态。

### 3. 报销台账导出功能验证
- **场景**: 在“报销台账”页面附加查询条件进行导出。
- **结果**:
  - [x] 输入某员工 ID，选取状态 `PENDING`，选定提交日期区间，并点击“导出”。
  - [x] 前端参数 `status`、`applicantId`、`dateFrom`、`dateTo` 正确映射至 `GET /api/admin/exports/reimbursements` 接口。
  - [x] 成功拦截空下载，同步下载出符合过滤条件的 `.xlsx` 文件，下载完毕后 loading 解除。
  - [x] 没有任何干扰原先 `查看/确认/驳回` 按钮和抽屉逻辑的操作。

### 4. 业务强边界声明
- [x] **本次为同步下载**，没有强行开发复杂的“后端投递任务->前端长轮询状态->进度条展示”的异步导出中心方案，坚守了轻量 MVP 边界。
- [x] 没有触碰任何 JWT/权限相关的拦截重构工作。
- [x] 没有因为本功能引入任何额外第三方文件处理或报表依赖包（如 `xlsx`，前端直接转发原生 blob）。

## 结论
**PASS**. M14B 所要求的导出按钮前端串联闭环已达成。前端用最轻快的方式完成了文件流转存下载，且不破坏现有的异步错误侦测。

## M14C 端到端断点检查

- [x] 财务日报导出参数为 `reportType=DAILY` + `date`，后端返回 `.xlsx` 文件流。
- [x] 财务月报导出参数为 `reportType=MONTHLY` + `month`，后端返回 `.xlsx` 文件流。
- [x] 财务范围导出参数为 `reportType=RANGE` + `startDate`/`endDate`，与后端接口枚举一致。
- [x] 报销台账导出对接 `GET /api/admin/exports/reimbursements`，筛选条件 `status`、`applicantId`、`dateFrom`、`dateTo` 生效。
- [x] 空数据仍导出只有表头的 `.xlsx` 文件。
- [x] 错误参数返回业务错误提示，不会把错误 JSON 下载成破损 Excel。
- [x] 本阶段不做异步任务中心、复杂模板、权限/JWT、多门店聚合或前端页面新增。
