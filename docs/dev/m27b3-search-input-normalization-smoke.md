# M27B-3 搜索输入规范与 LIKE 通配符安全治理

## 1. 背景

M27B-1、M27B-2、M27B-2A 已完成小程序和管理端的模糊搜索增强，并随 M28 一起部署生产。剩余技术债集中在搜索输入规范和 LIKE 通配符安全：用户输入 `%`、`_`、`\` 时不能被数据库当作通配符或转义字符处理，空字符串和纯空格也不应参与搜索过滤。

本轮只治理搜索输入和 LIKE 查询安全，不新增搜索字段，不调整接口返回结构，不修改库存、工单状态机、支付、退款、结算、官方售后结算、财务规则，不处理 `cashierStatus`。

## 2. Normalize 规则

- `null -> null`
- `"" -> null`
- `"   " -> null`
- `" abc " -> "abc"`
- 中文、手机号、车架号、编码、条码、混合大小写文本保留原内容，仅去除前后空格。

## 3. LIKE 转义策略

后端复用既有 `SearchKeywordUtils`，统一处理文本搜索：

- `normalize(String value)`：空值和 blank 转为 `null`，非空文本 trim。
- `escapeLike(String value)`：对 `%`、`_`、`\` 和 escape 字符 `!` 前置 `!`。
- `buildContainsPattern(String value)`：生成包含匹配 pattern。
- `containsCondition(String columnName)`：生成固定列名条件 `column LIKE {0} ESCAPE '!'`。

用户输入通过 MyBatis-Plus 参数绑定传递，列名由代码固定传入，不拼接用户输入。

示例：

- `abc -> %abc%`
- `% -> %!%%`
- `_ -> %!_%`
- `\ -> %!\%`

## 4. 后端覆盖范围

- 客户列表：`keyword`、`phone`、`customerName`。
- 车辆列表：`keyword`、`vin`、`model`、`batteryNo`、`customerPhone`、`customerName`。
- 工单列表：`keyword`、`workOrderNo`、`customerName`、`customerPhone`、`vehicleFrameNo`、`scooterModel`、官方订单号子查询。
- 配件列表：`keyword`、`partCode`、`partName`、`officialPartNo`、`barcode`、`model`。
- 库存列表和库存流水：`keyword`、`partCode`、`partName`。
- 报销列表和报销导出：`reimbursementNo`、`applicantName`。
- 员工与权限用户列表：`username`、`realName`、`phone`。
- 支付记录列表：`workOrderNo`、`customerName`。
- 退款记录列表：`workOrderNo`、`customerName`。
- 官方售后结算列表：`workOrderNo`、`officialOrderNo`。
- 小程序 staff 客户/车辆搜索：blank keyword 直接返回空列表，非空 keyword 进入统一后端搜索。
- 小程序 staff 配件/库存/工单搜索：页面和 API 参数层已 trim，blank 不作为查询条件传递。

## 5. 前端覆盖范围

Admin-web：

- 客户、车辆、工单、配件、库存、报销、导出、员工与权限列表已通过 `trimSearchFields` 或 `setSearchParam` 规范文本搜索参数。
- 支付、退款、官方售后结算列表补齐 `setSearchParam`，空字符串不传。
- 库存流水 `partCode`、`partName` 补齐 `setSearchParam`，避免纯空格透传。
- 翻页保留当前筛选条件，搜索按钮重置 `pageNo = 1`。

Mini-program：

- 客户/车辆搜索输入在 debounce 和实际请求前统一 normalize。
- `searchCustomers`、`searchVehicles` 对 blank keyword 直接返回空结果，不再发送 `keyword=`。
- 配件、库存、工单搜索继续使用 `normalizeSearchParam`，blank 不传查询条件。
- `tsconfig.json` 补齐小程序类型检查所需的 ES lib、第三方声明跳过和 TDesign 生成目录路径映射；不改变运行时代码。

## 6. 测试覆盖

新增或补充后端测试：

- 客户列表输入 `%` 不匹配全部记录。
- 小程序 staff 客户搜索 blank 返回空列表。
- 小程序 staff 车辆搜索 blank 返回空列表。
- 支付记录按客户名搜索 `%` 不匹配全部工单。
- 退款记录按客户名搜索 `%` 不匹配全部工单。
- 官方售后结算按官方订单号搜索 `%` 不匹配全部记录。

既有测试继续覆盖 `SearchKeywordUtils.normalize`、`escapeLike`、`buildContainsPattern`、`containsCondition`。

## 7. 边界说明

- 未新增数据库 migration。
- 未修改生产数据库。
- 未删除历史数据。
- 未修改接口返回结构。
- 未新增搜索字段。
- 未修改库存预占、正式扣减、释放逻辑。
- 未修改工单状态机。
- 未修改支付、退款、结算、官方售后结算、财务利润计算口径。
- 未修改微信登录主链路。
- 未输出 DB、JWT、微信、token、hash 等敏感信息。

## 8. 验证命令

```bash
cd backend && JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH mvn test
cd admin-web && npx vue-tsc --noEmit
cd admin-web && npm run build
cd mini-program && npx tsc --noEmit
git diff --check
git status --short
```

## 9. 验证结果

- `backend`：`mvn test` 通过，736 tests，0 failures，0 errors，0 skipped。
- `admin-web`：`npx vue-tsc --noEmit` 通过。
- `admin-web`：`npm run build` 通过；仅保留既有 Rollup 注释和大 chunk warning。
- `mini-program`：`npx tsc --noEmit` 通过。
- `git diff --check`：通过。

## 10. 后续部署建议

本轮只做本地修复、测试和提交，不部署、不 push。建议后续由 Owner 确认小程序真机搜索 smoke 后，再统一评估部署窗口。
