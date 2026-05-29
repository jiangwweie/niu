# M18B 条码 / 扫码能力端到端 Smoke

## 原则

- 扫码只是输入方式，不改变库存、工单、支付、退款、结算核心规则。
- 入库仍走后端入库接口，必须生成 `INBOUND` 库存流水。
- 工单草稿添加配件不预占库存，提交工单后才 `RESERVE`，结算后才 `CONSUME`。
- lookup 必须限制当前 `store_id`，停用或删除配件不能用于新业务。
- 不新增扫码专用权限，不引入新基础设施，不做复杂标签模板或批量打印。

## 后端 API

- `GET /api/staff/parts/lookup?code=xxx`
- `GET /api/admin/parts/lookup?code=xxx`
- staff lookup 使用现有库存查看 / 工单创建 / 工单更新权限。
- admin lookup 使用现有配件管理 / 库存查看权限。

返回字段包括 `partId`、`partCode`、`partName`、`officialPartNo`、`defaultBarcode`、`source`、`model`、`categoryCode`、`costPrice`、`enabled` 以及库存数量摘要。

## lookup 匹配顺序

1. `part_barcode.barcode` 精确匹配。
2. `part.part_code` 精确匹配。
3. `part.official_part_no` 精确匹配。
4. `part.default_barcode` 精确匹配。

空 `code` 返回 400；未找到、跨门店、停用、删除均返回明确业务错误。

## 入库 barcode/code 解析

- 请求带 `partId` 时保持原有入库路径。
- 未带 `partId` 但带 `barcode` 或 `code` 时，通过 lookup 解析配件。
- 命中后继续走原有入库服务，更新 `actual_qty` 和 `available_qty`，`reserved_qty` 不变。
- 未命中返回“未找到对应配件，请先新增配件”，不产生库存变化或库存流水。

## 工单扫码添加配件

- 工单添加收费项接口支持 `barcode` / `code`。
- 请求带 `partId` 时保持原有逻辑。
- 未带 `partId` 但带条码或编码时，解析后设置 `partId`。
- 仅在 DRAFT 工单中添加收费项，不预占库存。
- 提交工单后由现有提交逻辑预占库存。

## 临时配件闭环

员工在工单草稿扫码未命中时，可以最小化新增临时配件：

1. 创建配件主数据，`create_source=WORK_ORDER_TEMP`。
2. 写入配件来源、名称、型号、成本价、默认条码等字段。
3. 同步 `part_barcode` 主码。
4. 按本次使用数量执行临时入库。
5. 生成 `INBOUND` 库存流水。
6. 将新配件加入当前工单收费项。

以上操作在事务内执行，失败则整体回滚。

## 小程序扫码入口

- 库存页：扫码 lookup，命中后展示配件和库存摘要；未命中提示未找到，不伪造数据。
- 入库页：扫码或手动输入条码后识别配件，命中后带入入库表单；未命中提示手动选择或新增配件后再入库。
- 工单草稿：扫码添加已有配件，未命中时引导临时新增配件。
- `wx.scanCode` 用户取消与真实未命中分开提示。
- 未修改 `mini-program/src/utils/config.ts`。

## admin-web 条码能力

- 配件列表展示默认条码。
- 创建和编辑配件支持 `defaultBarcode`。
- 配件详情展示默认条码、官方品号、配件编码。
- 提供复制条码按钮，复制成功提示“已复制”。
- 提供单个配件最简打印入口，仅打印配件名称、配件编码和条码文本。
- 不引入条码渲染重依赖，不做批量打印或复杂标签模板。

## 权限说明

- 库存扫码查询沿用 `INVENTORY_VIEW`。
- 扫码入库沿用 `INVENTORY_INBOUND`。
- 工单扫码添加沿用 `WORK_ORDER_CREATE` / `WORK_ORDER_UPDATE`。
- admin 配件 lookup 沿用 `PART_MANAGE` / `INVENTORY_VIEW`。

## 自动化验证

建议执行：

```bash
mvn -f backend/pom.xml test
npm --prefix admin-web run lint
npm --prefix admin-web run build
cd mini-program && npx tsc --noEmit
git diff --check
```

## 手工 smoke 步骤

1. staff 按条码 lookup 命中。
2. staff 按配件编码 lookup 命中。
3. staff 按官方品号 lookup 命中。
4. admin 按条码 lookup 命中。
5. 入库只传 `barcode` 成功并生成 `INBOUND`。
6. 入库 `barcode` 未命中失败，库存不变。
7. 小程序库存页扫码命中。
8. 小程序库存页扫码未命中。
9. 小程序入库页扫码命中并提交入库。
10. 小程序工单草稿扫码添加已有配件。
11. 小程序工单草稿扫码未命中后临时新增配件。
12. 临时新增配件生成 `INBOUND`。
13. 临时配件提交工单后 `RESERVE`。
14. 临时配件结算后 `CONSUME`。
15. admin-web 创建配件填写 `defaultBarcode`。
16. admin-web 复制条码。
17. admin-web 简易打印。
18. 401 / 403 行为不变。
19. 确认 `mini-program/src/utils/config.ts` 未进入提交。
20. 确认 `docs/dev/local-startup-guide.md` 未进入提交。

## 未处理问题

- 未接入条码图形渲染库，当前打印为文本版最小能力。
- 微信开发者工具内的扫码链路仍需真机或开发者工具手工 smoke。
