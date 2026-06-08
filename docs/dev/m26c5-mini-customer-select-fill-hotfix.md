# M26C-5：小程序创建工单客户搜索结果选择后回填异常修复

## 问题现象

- 搜索已有客户能正常返回结果
- 但点击搜索结果后，客户姓名未正确回填到创建工单表单
- 手机号和客户 ID 正常回填，仅客户姓名缺失

## 根因

**前端接口字段名与后端 JSON 序列化字段名不一致。**

后端 `StaffCustomerController.java` 返回：
```java
public record CustomerSearchResult(Long id, String customerName, String phone) {}
```
JSON 字段为 `{ id, customerName, phone }`。

前端 `src/api/customer.ts` 接口定义使用了 `name` 而非 `customerName`：
```ts
export interface CustomerSearchResult {
  id: number;
  name: string;      // 错误，后端字段是 customerName
  phone: string;
}
```

导致：
- WXML `{{item.name}}` 绑定值为 `undefined`，搜索结果列表客户姓名不显示
- `selectCustomer` 中 `customer.name` 为 `undefined`，回填到表单的 `customerNameSnapshot` 为空

`id` 和 `phone` 字段前后端一致，未受影响。

## 修复方式

1. `src/api/customer.ts`：`CustomerSearchResult` 接口 `name` → `customerName`
2. `index.wxml`：搜索结果模板 `{{item.name}}` → `{{item.customerName}}`
3. `index.ts`：`selectCustomer` 方法 `customer.name` → `customer.customerName`

## 是否涉及车辆回填

车辆搜索后端返回字段（`frameNo`、`model`、`customerName`、`customerPhone`）与前端 `VehicleSearchResult` 接口一致，无需修改。

## 是否改 backend

否。

## 是否改数据库

否。

## 是否删除历史数据

否。

## 验证步骤

1. 打开创建工单页面
2. 点击"搜索已有客户"
3. 输入客户姓名或手机号搜索
4. 确认搜索结果正确显示客户姓名和手机号
5. 点击某个客户结果
6. 确认弹层关闭，页面显示客户姓名和手机号
7. 确认客户姓名和手机号填入对应表单字段
8. 保存草稿后确认工单使用已选客户 ID（非新建客户）

## 小程序体验版上传建议

- 版本号：0.26.5-exp.1
- 版本描述：修复创建工单客户搜索结果选择后姓名回填异常
- 不提交审核
