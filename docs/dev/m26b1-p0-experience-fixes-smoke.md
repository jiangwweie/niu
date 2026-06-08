# M26B-1 交付前 P0 体验风险修复 Smoke Report

## 1. 基本信息

- **修复日期**：2026-06-08
- **分支**：`feature/m18-delivery-readiness`
- **基础提交**：`a8fd9f5 fix(m25f): restore draft work order editing`
- **执行者**：Antigravity AI (Claude Sonnet 4.6 Thinking)
- **修复范围**：P0 级别体验风险，共 4 项

---

## 2. 修复的 4 个 P0

### P0-03：clearAuth 存储键不匹配（有数据安全风险）

**问题**：`stores/auth.ts` 的 `clearAuth()` 调用 `wx.removeStorageSync('user')`，而 `storage.setUser()` 实际写入的键是 `auth_user`（定义在 `utils/storage.ts` 的 `KEYS.USER`）。两者键不匹配，导致退出后旧用户数据残留。

**修复**：改为 `storage.clearUser()`，与 `storage.setUser` 使用相同键。

**涉及文件**：`mini-program/src/stores/auth.ts`

---

### P0-02：小程序退出登录无二次确认

**问题**：`pages/mine/index.ts` 的 `handleLogout` 直接执行退出，无确认弹窗。

**修复**：增加 `wx.showModal` 弹窗确认。点取消 → return 不执行；点退出 → 执行 clearAuth 并跳转登录页。

**涉及文件**：`mini-program/src/pages/mine/index.ts`

---

### P0-04：管理端客户/车辆详情工单查看跳转断裂

**问题**：`work-order/index.vue` 的 `onMounted` 只读取了 `route.query.partId`，未处理 `route.query.id`，从客户/车辆详情跳过来的工单无法自动打开。

**修复**：`onMounted` 改为 async，await fetchData 后读取 `route.query.id`，在 tableData 中找到对应工单（id 均为 string）后自动调用 handleView 打开详情抽屉；找不到则提示用户手动搜索。

**涉及文件**：`admin-web/src/views/work-order/index.vue`

---

### P0-01：小程序工作台「待办与提醒」静态误导

**问题**：首页永远显示「当前无待办工单」，误导员工以为无工单需处理。

**不做后端统计接口的原因**：体验版已上线，新接口需开发联调测试，风险不可控；P0 核心是消除误导，非实现完整功能。

**修复**：移除静态「当前无待办工单」区域，改为中性「工单提醒」入口卡片，说明「请前往工单列表查看当前待处理工单」，并提供「查看全部工单 →」跳转按钮。

**涉及文件**：
- `mini-program/src/pages/dashboard/index.wxml`
- `mini-program/src/pages/dashboard/index.ts`（添加 goToWorkOrders）
- `mini-program/src/pages/dashboard/index.wxss`（添加 notice-card 样式）

---

## 3. 不做内容说明

| 未做事项 | 原因 |
|----------|------|
| 后端待办统计接口 | 体验版上线期间不引入新接口 |
| P1/P2 问题修复 | 本轮只修 P0 |
| placeholder 页面重构 | 上线前夕不引入重构风险 |
| 历史旧状态数据清理 | 生产数据不在本轮范围 |
| push | 遵守本轮约束 |
| 小程序提交审核 | 由 Owner 决定上传时机 |

---

## 4. 修改文件清单

| 文件 | 修改内容 |
|------|----------|
| `mini-program/src/stores/auth.ts` | P0-03: clearAuth 改用 storage.clearUser() |
| `mini-program/src/pages/mine/index.ts` | P0-02: handleLogout 增加 wx.showModal 二次确认 |
| `mini-program/src/pages/dashboard/index.wxml` | P0-01: 移除静态待办区，添加工单提醒入口 |
| `mini-program/src/pages/dashboard/index.ts` | P0-01: 添加 goToWorkOrders 方法 |
| `mini-program/src/pages/dashboard/index.wxss` | P0-01: 添加 notice-card 相关样式 |
| `admin-web/src/views/work-order/index.vue` | P0-04: onMounted 增加 route.query.id 处理 |

不涉及：backend / 数据库 / 历史数据 / 库存 / 工单状态机 / 支付 / 结算逻辑

---

## 5. 验证步骤

### 5.1 clearAuth 存储键验证（P0-03）

1. 登录小程序（账号密码方式）
2. 微信开发者工具 → Storage，确认 `auth_user` 有数据
3. 点击「退出登录」→ 点确认
4. 检查 `auth_user` → **应为空**，`accessToken` → **应为空**

### 5.2 退出登录二次确认验证（P0-02）

1. 进入「我的」页面，点击「退出登录」
2. **预期**：弹出对话框「确定退出登录？退出后需重新输入账号密码。」
3. 点「取消」→ **预期**：留在当前页，不退出
4. 再次点「退出登录」→ 点「退出」→ **预期**：Toast「已退出登录」，跳转登录页

### 5.3 管理端工单跳转验证（P0-04）

1. 打开「客户管理」，找有维修历史的客户，点「查看详情」
2. 在维修历史表格点「查看」
3. **预期**：跳转工单管理页，自动弹出对应工单详情抽屉
4. 在「车辆档案」重复测试
5. **边界**：工单不在首页 → 提示「未找到该工单，可能已超出当前筛选范围，请手动搜索」

### 5.4 工作台待办区验证（P0-01）

1. 打开小程序首页
2. **预期**：不显示「当前无待办工单」；显示「工单提醒」区块，有「查看全部工单 →」入口
3. 点击「查看全部工单 →」→ **预期**：跳转工单列表标签页

---

## 6. 验证结果

### admin-web
- `npx vue-tsc --noEmit`：✅ 无错误
- `npm run build`：✅ 构建成功（built in 4.17s）

### mini-program
- `npx tsc --noEmit`（过滤本次修改文件）：✅ mine / dashboard / stores/auth 无错误
- 历史遗留错误说明：tdesign-miniprogram / miniprogram-api-typings 的类型声明冲突（HTMLCanvasElement 重复声明等）为历史存量问题，本次修改文件不在错误列表中

### git diff --check
```
PASS: no whitespace errors
```

---

## 7. 部署和体验版上传步骤（Owner 决定后执行）

### 小程序体验版更新
1. 微信开发者工具打开 `mini-program` 目录
2. 确认 `appid` 为生产 AppID
3. 点「上传」，版本号递增，描述：`fix: P0 退出确认、auth缓存修复、工作台误导文案`
4. 微信公众平台 → 版本管理 → 设为体验版
5. 通知体验用户刷新小程序

### 管理端部署
1. `git push origin feature/m18-delivery-readiness`
2. CI/CD 自动构建部署，或手动上传 dist

---

## 8. Owner 注意事项

1. **P0-04 的 ID 类型**：`WorkOrderRecord.id` 为 string，customer/vehicle 页面传入 number 经 router 序列化为字符串，比较正确。若后端 id 格式有变需同步检查。

2. **P0-01 工单提醒区**：目前是静态引导文案。若后续需展示真实待办数量，需后端统计接口，建议作为 M27 任务。

3. **建议尽快上传体验版**：4 个 P0 均已修复，建议上传体验版让客户验证退出确认和首页文案。
