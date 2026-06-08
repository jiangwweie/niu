# M27B-3 搜索输入规范与 LIKE 通配符安全治理

## 1. 修复背景

M27B-1、M27B-2、M27B-2A 逐步扩大了小程序和管理端的模糊搜索范围，覆盖客户、车辆、工单、配件、库存、报销、员工等列表。审查发现新增或修改的 LIKE 查询缺少统一输入规范，用户输入 `%`、`_`、`\` 时可能被数据库当作 LIKE 通配符或转义字符处理。

## 2. 为什么 M27B-3 单独治理

本轮只做搜索输入规范和 LIKE 安全收口，不新增业务搜索字段，不引入全文搜索、ES、Redis、MQ，不改变库存、工单状态机、支付、退款、结算、财务规则。

## 3. normalize 规则

- `null -> null`
- `"" -> null`
- `"   " -> null`
- `" abc " -> "abc"`
- 中文、手机号、车架号、编码、条码、混合大小写文本保留原内容，仅去除前后空格。

## 4. 空字符串不查询规则

后端所有本轮覆盖的文本搜索字段先 normalize，结果为 `null` 时不追加查询条件。前端提交前 trim 文本字段，API 层兜底把空字符串或纯空格转为不传。

## 5. LIKE 通配符处理方式

后端新增统一工具方法，对 `%`、`_`、`\` 和 escape 字符 `!` 做转义，并使用参数绑定构造包含匹配：

```text
column LIKE {0} ESCAPE '!'
```

示例：

- `abc -> %abc%`
- `% -> %!%%`
- `_ -> %!_%`
- `\ -> %!\%`

## 6. 后端工具方法说明

新增 `SearchKeywordUtils`：

- `normalize(String value)`
- `escapeLike(String value)`
- `buildContainsPattern(String value)`
- `containsCondition(String columnName)`

工具方法只处理搜索输入和 LIKE pattern，不拼接用户输入。列名在代码中固定传入，用户输入通过 MyBatis-Plus 参数绑定传递。

## 7. 覆盖的查询接口

- 管理端客户列表：客户姓名、手机号、keyword。
- 管理端车辆列表：车架号、车型、电池号、客户手机号、客户姓名、keyword。
- 管理端工单列表：工单号、客户姓名、手机号、车架号、车型、keyword、官方单号子查询。
- 管理端配件列表：配件编码、配件名称、官方品号、条码、型号、keyword。
- 管理端库存列表：keyword、配件编码、配件名称。
- 管理端报销列表：报销编号、报销人姓名。
- 管理端报销导出：报销编号、报销人姓名与列表筛选保持一致。
- 管理端员工列表：账号、姓名、手机号。
- 小程序 staff 配件列表：keyword。
- 小程序 staff 库存列表：keyword、配件编码、配件名称。
- 小程序 staff 工单列表：keyword。

## 8. 前端参数规范

Admin-web：

- 搜索提交前 trim 文本字段。
- API 层空文本不传。
- 搜索按钮重置 `pageNo = 1`。
- 翻页保留当前筛选条件。
- 重置按钮清空条件并刷新。
- 状态、日期、来源、角色等结构化筛选不改变语义。

Mini-program：

- keyword 发送前 trim。
- 空 keyword 不传。
- 搜索 debounce 保留。
- status / keyword / pageNo / pageSize 组合保留。
- 配件选择弹窗改为后端搜索，不再只过滤本地默认第一页。

## 9. locationRemark 结论

审查确认 `PartQueryResponse` 已有 `locationRemark`，但 `StaffPartListItem` 原先未返回该字段，导致小程序类型和本地过滤里的 `locationRemark` 没有真实数据来源。

本轮在 Owner 同意修复建议后，低风险补充 `StaffPartListItem.locationRemark` 映射，使小程序配件搜索可实际按后端已覆盖的库位备注命中。该变更为响应字段追加，不涉及数据库结构。

## 10. 未改数据库说明

本轮未新增、删除或修改数据库表结构，未新增 migration，未修改生产数据库。

## 11. 未删除历史数据说明

本轮未删除任何历史数据，也未执行数据清理脚本。

## 12. 未改核心业务规则说明

本轮未修改库存预占、正式扣减、释放逻辑，未修改工单状态机，未修改支付、退款、结算、官方售后结算、财务利润计算口径。

## 13. 验证命令

```bash
cd backend && JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH mvn test
cd admin-web && npx vue-tsc --noEmit
cd admin-web && npm run build
cd mini-program && npx tsc --noEmit
git diff --check
git status --short
```

## 14. 后续统一部署建议

本轮只做本地修复、测试和提交，不部署、不 push。建议后续由 Owner 统一评估 M27B-1、M27B-2、M27B-2A、M27B-3 后再决定部署窗口。
