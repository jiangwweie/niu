# 小程序 UI 与组件约束

## 1. 依据来源

本约束只参考有机构背书的官方设计体系，不参考个人自媒体。

| 来源 | 采用点 | 本项目落地 |
|---|---|---|
| Apple Human Interface Guidelines - Color / Typography | 颜色和字体应服务于层级、可读性与语义 | 颜色只按主操作、成功、警告、危险、文本层级使用 |
| Material Design 3 - Color roles / Typography | 使用语义化颜色角色和清晰字体层级 | 不按页面临时取色，使用固定语义 token |
| Ant Design - Design Values | 企业级产品强调确定性、自然、效率 | 小程序是工作入口，不做装饰型 UI，优先高频操作效率 |
| 微信小程序 picker / TDesign MiniProgram Picker | 普通短枚举用选择器，长列表用搜索面板 | 车型、原因、分类用 picker；客户、车辆、配件搜索用弹层 |

参考链接：

- https://developer.apple.com/design/human-interface-guidelines/color
- https://developer.apple.com/design/human-interface-guidelines/typography
- https://m3.material.io/styles/color/roles
- https://m3.material.io/styles/typography/overview
- https://ant.design/docs/spec/values/
- https://tdesign.tencent.com/miniprogram/components/picker

## 2. 产品定位

### 2.1 小程序定位

小程序是门店员工的体验入口，不是宣传页。界面应以快速录入、快速查找、低误操作为目标。

### 2.2 设计原则

1. 页面第一屏优先呈现当前任务和主操作。
2. 高频表单字段必须可直接输入，不强迫用户先选择。
3. 短枚举用 picker，不用大弹层。
4. 长列表搜索用底部弹层或搜索页。
5. 危险操作不和主操作同视觉权重。
6. 页面说明文案只保留影响业务结果的风险提示。

## 3. 颜色约束

| Token | 值 | 用途 |
|---|---:|---|
| primary | `#0052d9` | 主操作、可点击文本、选中态 |
| brand | `#008d9e` | 品牌识别、登录页主标题、少量品牌强调 |
| success | `#2ba471` | 成功、已完成、已收齐 |
| warning | `#ed7b2f` | 待处理、库存预警、注意提示 |
| danger | `#d54941` | 删除、取消、退款、失败 |
| page | `#f5f6f8` | 页面背景 |
| surface | `#ffffff` | 卡片、表单、弹窗 |
| text-primary | `#1f2933` | 标题和主文本 |
| text-secondary | `#667085` | 辅助信息 |
| text-placeholder | `#98a2b3` | 占位、弱提示 |
| border | `#e6e9ef` | 分割线、描边 |

禁止：

1. 单页临时新增接近但不同的蓝色、灰色、红色。
2. 大面积渐变、装饰色块。
3. 用颜色表达业务状态但没有文字标签。

## 4. 间距与圆角

| Token | 值 | 用途 |
|---|---:|---|
| page-padding | `24rpx` 或 `32rpx` | 页面左右留白 |
| section-gap | `24rpx` | 卡片之间 |
| section-padding | `28rpx 32rpx` | 卡片内边距 |
| row-height | `96rpx` | 普通表单行 |
| action-height | `88rpx` | 主要按钮和操作项 |
| radius-card | `16rpx` | 普通卡片 |
| radius-control | `12rpx` | 输入框、选择项、按钮容器 |
| hairline | `1rpx` | 分割线 |

禁止：

1. `px` 和 `rpx` 在同一页面随意混用。
2. 页面卡片圆角超过 `20rpx`，除弹窗顶部圆角。
3. 表单行高度随内容跳动。

## 5. 字体层级

| 层级 | 字号 | 字重 | 用途 |
|---|---:|---:|---|
| page-title | `36rpx` | 700 | 页面核心标题，慎用 |
| section-title | `30rpx` | 700 | 卡片标题 |
| body | `28rpx` | 400 | 普通正文 |
| form-label | `30rpx` | 400 | 表单 label |
| value | `30rpx` | 400/600 | 表单值、列表主值 |
| helper | `24rpx` | 400 | 辅助说明 |
| badge | `22rpx` | 500 | 状态标签 |

禁止：

1. 卡片内部使用 hero 级大标题。
2. 负字距。
3. 长提示文案塞进按钮或标签。

## 6. 组件选型

| 场景 | 组件模式 | 适用字段 |
|---|---|---|
| 短枚举，仍允许手输 | 输入框 + 右侧 picker 入口 | 车型、维修项目、配件分类、入库原因、退款原因、取消原因 |
| 固定少量单选 | 分段按钮/标签按钮 | 收款方式、退款方式、配件来源 |
| 长列表搜索 | 搜索弹层 | 客户、车辆、配件 |
| 扫码 + 手输 | 表单行 + 中间输入 + 右侧扫码 | 车架号、电池号、配件条码 |
| 主操作 | 固定底部或页面末尾大按钮 | 保存、提交、确认入库 |
| 危险操作 | danger outline 或更多操作区 | 删除、取消工单、退款 |

## 7. 表单行规范

标准表单行：

```text
label        输入/展示值        操作
```

约束：

1. label 固定宽度，通常 `132rpx`。
2. 右侧操作固定贴右。
3. 中间输入区 `flex: 1`，不得挤压右侧操作。
4. placeholder 使用 `text-placeholder`。
5. 选择和扫码入口必须是独立点击区，不藏在说明文案中。

## 8. 页面布局规范

### 8.1 录入页

结构：

```text
页面说明/当前对象摘要
客户/车辆/业务表单卡片
结果提示
主操作
```

### 8.2 列表页

结构：

```text
搜索 / 筛选 / 新建入口
状态筛选
列表
空态 + 下一步动作
```

### 8.3 详情页

结构：

```text
对象摘要
关键状态
业务信息
金额/库存等结果信息
主操作
次操作 / 危险操作
```

## 9. 弹窗规范

只在以下场景使用底部弹窗：

1. 搜索长列表。
2. 编辑一组字段。
3. 需要确认或补充备注的业务动作。

短枚举不使用大弹窗。

## 10. 验收规则

每次 UI 改造至少检查：

1. 小屏 iPhone 模拟器无重叠。
2. 表单行右侧操作对齐。
3. 字典为空时仍可手输。
4. 字典有值时 picker 可选。
5. 主操作和危险操作视觉权重不同。
6. TypeScript 检查通过。
7. 微信开发者工具 `build-npm` 无 warning。
