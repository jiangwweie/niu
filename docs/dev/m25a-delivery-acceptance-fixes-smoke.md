# M25A 交付验收问题修复包 Smoke

## 1. 修复问题列表

- 库存流水审计字段：操作人从裸 ID 改为姓名/账号优先展示，操作时间统一使用后端流水时间字段。
- 逻辑删除入口：客户、车辆、配件、草稿工单统一使用逻辑删除，不做物理删除。
- 工单录入档案关联：新建/编辑草稿支持客户、车辆档案 ID，并由后端校验门店和归属关系后回填快照。
- 门店管理员员工管理：STORE_ADMIN 可以创建和管理本门店员工，但不能越权创建平台账号、跨店账号或 SUPER_ADMIN。
- 登录验证码：admin-web 和小程序账号密码登录增加后端校验的一次性算术验证码；微信快捷登录不要求验证码。

## 2. 库存流水审计字段验收

- 后端库存流水查询返回 `operatorName`，由 `operator_id` 关联 `sys_user`。
- 前端展示优先使用 `operatorName`；若后端仅返回 `operatorId`，展示为 `员工 #<id>`，不直接显示裸数字。
- 入库、提交工单预占、维修完成扣减、取消释放均继续写入 `operator_id` 和流水时间字段。
- admin-web 库存流水列表统一格式化操作时间。

## 3. 逻辑删除规则

- 客户：`DELETE /api/admin/customers/{id}`，删除后客户列表和新建工单搜索默认不返回，历史工单快照不受影响。
- 车辆：`DELETE /api/admin/vehicles/{id}`，删除后车辆列表、客户详情车辆列表和新建工单车辆搜索默认不返回，历史快照保留。
- 配件：`DELETE /api/admin/parts/{partId}`，有 `actualQty`、`availableQty`、`reservedQty` 任一大于 0 时拒绝删除。
- 工单：`DELETE /api/admin/work-orders/{workOrderId}` 仅允许 `DRAFT` 草稿逻辑删除；其他状态拒绝，应走既有业务动作。

## 4. 客户/车辆关联回填验收

- 选择客户档案时，后端按当前 `storeId` 校验客户未删除，并回填客户姓名、手机号快照。
- 选择车辆档案时，后端按当前 `storeId` 校验车辆未删除，并回填车型、车架号、电池号及车主快照。
- 同时传入 `customerId` 和 `vehicleId` 时必须归属一致，不一致拒绝。
- 不选择档案时仍允许手工填写快照字段。
- staff 车辆搜索支持按客户过滤，并返回车主手机号、电池号，便于小程序自动填充。

## 5. 门店管理员新建员工验收

- SUPER_ADMIN 保留跨门店和门店管理员账号管理能力。
- STORE_ADMIN 创建账号时后端强制使用当前登录用户 `storeId`，账号类型固定为 `STORE`。
- STORE_ADMIN 不能创建平台账号、不能跨门店创建、不能授予 SUPER_ADMIN 或其他门店角色。
- 非 STORE_ADMIN 的普通门店角色默认不能创建员工。
- STORE_ADMIN 第一版禁止修改自己的角色，避免自我移除管理能力。

## 6. 登录验证码验收

- `GET /api/auth/captcha` 返回 `captchaId`、算术题文本和过期秒数。
- `POST /api/auth/login/password` 必须传 `captchaId` 和 `captchaCode`。
- 验证码由后端校验，一次性使用，5 分钟过期。
- 登录成功或失败后验证码均失效。
- 验证码缺失、错误、过期返回中文友好错误码；微信快捷登录不需要验证码。

## 7. 权限边界

- 客户/车辆删除：`CUSTOMER_MANAGE`。
- 配件删除：`PART_MANAGE`。
- 草稿工单删除：`WORK_ORDER_UPDATE`，且服务层二次校验状态。
- 员工创建：`USER_MANAGE`，服务层执行门店边界和角色边界。

## 8. 测试结果

- 后端 `mvn test`：通过，687 tests，0 failures，0 errors。
- 后端重点回归 `mvn -Dtest=AuthControllerTest,CustomerVehicleControllerTest,PartServiceTest,WorkOrderControllerTest test`：通过，111 tests，0 failures，0 errors。
- admin-web `npx vue-tsc --noEmit`：通过。
- admin-web `npm run build`：通过；仅有既有 chunk size / Rollup PURE 注释告警。
- mini-program `npx tsc --noEmit`：未通过，失败点为历史依赖声明和 TDesign 声明缺失，包括 `miniprogram-api-typings` 与 DOM 类型重复、`tdesign-miniprogram` 声明缺失等；本次业务改动未出现新增 TypeScript 业务代码错误。
- `git diff --check`：通过。

## 9. 未做事项

- 未增加 Redis 或外部验证码存储，第一版使用进程内存保存。
- 未做图形验证码，第一版使用算术验证码。
- 未实现已取消工单删除。
- 未改动库存预占、扣减、释放触发规则。
- 未改动支付、退款、交付关闭核心语义。

## 10. 风险项

- 内存验证码在多实例部署下不共享；正式多实例前需要迁移到共享缓存。
- 删除入口复用现有 `deleted` 字段，无新增数据库迁移；若生产历史数据存在脏数据，需要独立数据核查。
- 小程序历史 TypeScript 基线可能包含非本次业务代码错误，验收时需要区分新增影响。
