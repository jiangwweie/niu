# M27B-1 小程序高频模糊搜索增强 - 冒烟测试

## 1. 本轮搜索范围

小程序现场高频搜索入口增强，使员工能通过模糊关键词快速找到客户、车辆、配件、工单、库存。

## 2. 修改文件

### Backend (9 files)
- `backend/.../customer/service/impl/CustomerServiceImpl.java` — keyword 增加 remark 字段
- `backend/.../customer/service/impl/VehicleServiceImpl.java` — keyword 增加 customerName 子查询
- `backend/.../inventory/service/InventoryService.java` — 新增 keyword 参数重载方法
- `backend/.../inventory/service/impl/InventoryServiceImpl.java` — keyword 多字段模糊搜索实现
- `backend/.../part/dto/PartQueryRequest.java` — 新增 keyword 字段
- `backend/.../part/service/impl/PartServiceImpl.java` — keyword 多字段模糊搜索实现
- `backend/.../staff/controller/StaffInventoryController.java` — 接受 keyword 参数
- `backend/.../staff/controller/StaffPartController.java` — 接受 keyword 参数
- `backend/.../workorder/service/impl/WorkOrderServiceImpl.java` — keyword 增加 battery_no_snapshot

### Mini-program (7 files)
- `mini-program/src/pages/create-work-order-placeholder/index.ts` — 配件本地筛选扩展字段
- `mini-program/src/pages/create-work-order-placeholder/index.wxml` — 配件/车辆搜索提示文案
- `mini-program/src/pages/inbound-placeholder/index.ts` — 配件本地筛选扩展字段
- `mini-program/src/pages/inbound-placeholder/index.wxml` — 配件搜索提示文案
- `mini-program/src/pages/inventory/index.wxml` — 库存搜索提示文案
- `mini-program/src/pages/parts/index.wxml` — 配件列表提示文案
- `mini-program/src/pages/work-orders/index.wxml` — 工单列表提示文案

## 3. 客户搜索增强说明

| 改造内容 | 说明 |
|---------|------|
| 改造前 | keyword 匹配 customerName + phone |
| 改造后 | keyword 匹配 customerName + phone + **remark** |
| 接口 | `GET /api/staff/customers/search?keyword=xxx` |
| 前端改动 | 无（keyword 已透传） |

## 4. 车辆搜索增强说明

| 改造内容 | 说明 |
|---------|------|
| 改造前 | keyword 匹配 frameNo + model + batteryNo；二次查询匹配 customerPhone |
| 改造后 | keyword 匹配 frameNo + model + batteryNo + **customerName 子查询**；二次查询匹配 customerPhone |
| 接口 | `GET /api/staff/vehicles/search?keyword=xxx` |
| 前端改动 | 提示文案更新为 `车架号、车型或客户手机号` |

## 5. 配件搜索增强说明

| 改造内容 | 说明 |
|---------|------|
| 改造前（本地） | includes 匹配 partName + partCode |
| 改造后（本地） | includes 匹配 partName + partCode + **officialPartNo + defaultBarcode + model** |
| 改造前（后端keyword） | 不存在 |
| 改造后（后端keyword） | keyword 匹配 partCode + partName + officialPartNo + defaultBarcode + model + locationRemark |
| 接口 | `GET /api/staff/parts?keyword=xxx` |
| 前端改动 | 本地筛选扩展 + 提示文案更新为 `搜索配件编码、名称、官方品号` |

## 6. 工单列表搜索增强说明

| 改造内容 | 说明 |
|---------|------|
| 改造前 | keyword 匹配 workOrderNo + customerNameSnapshot + customerPhoneSnapshot + frameNoSnapshot + vehicleModelSnapshot + officialOrderNo 子查询 |
| 改造后 | 同上 + **batteryNoSnapshot** |
| 接口 | `GET /api/staff/work-orders?keyword=xxx&status=xxx` |
| 前端改动 | 提示文案更新为 `搜索工单号、客户名、手机号、车架号` |

## 7. 库存/配件搜索增强说明

| 改造内容 | 说明 |
|---------|------|
| 改造前（后端） | 库存：partCode 精确 + partName LIKE |
| 改造后（后端） | keyword 模式：partCode LIKE + partName LIKE + officialPartNo LIKE + defaultBarcode LIKE + model LIKE + locationRemark LIKE |
| 接口 | `GET /api/staff/inventory/stocks?keyword=xxx` |
| 前端改动 | 提示文案更新为 `搜索配件编码、名称、官方品号` |

## 8. 是否修改 backend

是。9 个文件。

## 9. 是否修改数据库 / migration

否。

## 10. 是否删除历史数据

否。

## 11. 是否影响核心业务规则

否。仅修改查询条件，不改库存/工单状态机/支付/结算规则。

## 12. LIKE 安全处理说明

本轮新增的 keyword 搜索均使用 MyBatis-Plus `.like()` 方法，keyword 已在 Controller/Service 层做 `.trim()` 处理。
LIKE 通配符转义 (`%`/`_`) 未在本轮做全站治理，将作为后续 M27B-3 独立治理项。

## 13. 小程序体验版验证步骤

1. 创建工单 → 客户搜索 → 输入手机号后 4 位 → 应能找到客户
2. 创建工单 → 客户搜索 → 输入备注关键词 → 应能找到客户
3. 创建工单 → 车辆搜索 → 输入车架号后 6 位 → 应能找到车辆
4. 创建工单 → 车辆搜索 → 输入客户姓名 → 应能找到该客户的车辆
5. 创建工单 → 添加配件 → 输入官方品号 → 应能找到配件
6. 创建工单 → 添加配件 → 输入条码 → 应能找到配件
7. 工单列表 → 输入手机号后 4 位 → 应能搜到相关工单
8. 工单列表 → 输入车架号后 6 位 + 选择状态 → 两个条件同时生效
9. 库存查询 → 输入官方品号 → 应能搜到对应库存
10. 配件列表 → 输入部分配件编码 → 应能搜到对应配件
11. 各搜索框提示文案应显示"搜索配件编码、名称、官方品号"等新文案

## 14. 部署步骤

1. 后端部署：`scripts/deploy-prod-release.sh --restart`
2. 小程序上传体验版：版本号 `0.27.0-exp.1`，描述 `小程序模糊搜索增强`
