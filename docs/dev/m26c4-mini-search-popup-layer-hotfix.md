# M26C-4：小程序创建工单客户/车辆搜索弹层无法输入热修复

## 客户反馈

- 小程序创建工单页面，点击"搜索已有客户"或"搜索已有车辆"后，弹出搜索面板
- 输入框无法输入，无法聚焦，无法搜索
- Owner 截图显示：弹出的搜索面板本身也被灰色 mask 遮罩压暗

## 截图现象判断

popup 内容被 overlay mask 压暗，说明 popup content 的 z-index 低于或等于 overlay z-index。

## 实际根因

**z-index 值等于 overlay 默认值，导致被覆盖。**

t-design-miniprogram v1.14.0 的 t-popup 组件内部结构：

```
<t-overlay z-index="11000" />   ← overlay 默认 z-index 11000
<view z-index="{zIndex prop}">  ← popup content，默认 11500
```

客户搜索和车辆搜索弹层原代码：
```html
<t-popup z-index="11000" ...>
```

`z-index="11000"` 等于 overlay 的默认值 11000。由于 overlay 在 DOM 中位于 popup content 之后，同值 z-index 下后元素覆盖前元素，因此 overlay 直接盖住了 popup 内容，导致输入框无法聚焦和输入。

## 修复方式

去掉客户搜索和车辆搜索弹层上显式设置的 `z-index="11000"`，让 t-popup 使用组件默认 zIndex=11500（高于 overlay 的 11000）。

- 未改为自定义 modal
- 未去掉遮罩
- 仅修正 z-index 值

## 修改文件

- `mini-program/src/pages/create-work-order-placeholder/index.wxml`
  - 客户搜索弹层：移除 `z-index="11000"`
  - 车辆搜索弹层：移除 `z-index="11000"`

## 客户搜索验证步骤

1. 打开创建工单页面
2. 点击"搜索已有客户"
3. 确认弹层清晰显示在遮罩上方
4. 点击客户搜索输入框
5. 确认软键盘弹出
6. 连续输入 3 个以上字符
7. 确认搜索请求触发并展示结果
8. 点击客户后正确回填

## 车辆搜索验证步骤

1. 点击"搜索已有车辆"
2. 确认弹层清晰显示在遮罩上方
3. 点击车辆搜索输入框
4. 确认软键盘弹出
5. 连续输入车架号/车型/手机号
6. 确认搜索结果显示
7. 点击车辆后正确回填
8. 关闭弹层后 mask 消失
9. 再次打开仍正常

## 边界确认

- [x] 未修改 backend
- [x] 未修改数据库
- [x] 未删除历史数据
- [x] 未修改库存/工单状态机/支付/退款/结算/财务核心规则
- [x] 未 push
- [x] 仅修改 mini-program

## tsc 结果

`npx tsc --noEmit` 报错均为历史 tdesign-miniprogram / miniprogram-api-typings 类型问题，与本次修改无关，不影响微信开发者工具编译。

## 小程序体验版上传建议

- 版本号：0.26.5-exp.1
- 版本描述：修复创建工单客户/车辆搜索弹层无法输入问题
- 不提交审核
