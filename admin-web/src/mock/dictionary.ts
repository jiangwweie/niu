export const mockDictionaryTypes = [
  { code: 'WORK_ORDER_STATUS', label: '工单状态' },
  { code: 'PAYMENT_METHOD', label: '收款方式' },
  { code: 'PART_SOURCE', label: '配件来源' },
  { code: 'PART_CATEGORY', label: '配件分类' },
  { code: 'REIMBURSEMENT_STATUS', label: '报销状态' },
  { code: 'INVENTORY_FLOW_TYPE', label: '库存流水类型' },
  { code: 'OFFICIAL_SETTLEMENT_STATUS', label: '官方结算状态' }
];

interface MockDictItem {
  id: string;
  typeCode: string;
  typeLabel: string;
  dictCode: string;
  dictLabel: string;
  sort: number;
  enabled: boolean;
  isSystem: boolean;
  remark?: string;
}

export const mockDictionaryData: MockDictItem[] = [
  // 工单状态
  { id: '1', typeCode: 'WORK_ORDER_STATUS', typeLabel: '工单状态', dictCode: 'DRAFT', dictLabel: '新建中', sort: 1, enabled: true, isSystem: true, remark: '工单录入中' },
  { id: '2', typeCode: 'WORK_ORDER_STATUS', typeLabel: '工单状态', dictCode: 'REPAIRING', dictLabel: '维修中', sort: 2, enabled: true, isSystem: true },
  { id: '3', typeCode: 'WORK_ORDER_STATUS', typeLabel: '工单状态', dictCode: 'REPAIR_DONE', dictLabel: '维修完成', sort: 3, enabled: true, isSystem: true },
  { id: '4', typeCode: 'WORK_ORDER_STATUS', typeLabel: '工单状态', dictCode: 'DELIVERED', dictLabel: '已交付', sort: 4, enabled: true, isSystem: true },
  { id: '25', typeCode: 'WORK_ORDER_STATUS', typeLabel: '工单状态', dictCode: 'CANCELLED', dictLabel: '已取消', sort: 5, enabled: true, isSystem: true },
  
  // 收款方式
  { id: '5', typeCode: 'PAYMENT_METHOD', typeLabel: '收款方式', dictCode: 'WEIXIN', dictLabel: '微信', sort: 1, enabled: true, isSystem: true },
  { id: '6', typeCode: 'PAYMENT_METHOD', typeLabel: '收款方式', dictCode: 'ALIPAY', dictLabel: '支付宝', sort: 2, enabled: true, isSystem: true },
  { id: '7', typeCode: 'PAYMENT_METHOD', typeLabel: '收款方式', dictCode: 'CASH', dictLabel: '现金', sort: 3, enabled: true, isSystem: false },
  { id: '8', typeCode: 'PAYMENT_METHOD', typeLabel: '收款方式', dictCode: 'POS', dictLabel: 'POS机刷卡', sort: 4, enabled: false, isSystem: false, remark: '使用较少，已停用' },

  // 配件来源
  { id: '9', typeCode: 'PART_SOURCE', typeLabel: '配件来源', dictCode: 'OFFICIAL', dictLabel: '官方商城供货', sort: 1, enabled: true, isSystem: true },
  { id: '10', typeCode: 'PART_SOURCE', typeLabel: '配件来源', dictCode: 'THIRD_PARTY', dictLabel: '第三方采购', sort: 2, enabled: true, isSystem: false },
  { id: '11', typeCode: 'PART_SOURCE', typeLabel: '配件来源', dictCode: 'SECOND_HAND', dictLabel: '二手拆车件', sort: 3, enabled: true, isSystem: false },

  // 配件分类
  { id: '12', typeCode: 'PART_CATEGORY', typeLabel: '配件分类', dictCode: 'BATTERY', dictLabel: '电池类', sort: 1, enabled: true, isSystem: true },
  { id: '13', typeCode: 'PART_CATEGORY', typeLabel: '配件分类', dictCode: 'MOTOR', dictLabel: '电机类', sort: 2, enabled: true, isSystem: true },
  { id: '14', typeCode: 'PART_CATEGORY', typeLabel: '配件分类', dictCode: 'SHELL', dictLabel: '外壳类', sort: 3, enabled: true, isSystem: true },
  { id: '15', typeCode: 'PART_CATEGORY', typeLabel: '配件分类', dictCode: 'TIRE', dictLabel: '轮胎类', sort: 4, enabled: true, isSystem: true },

  // 报销状态
  { id: '16', typeCode: 'REIMBURSEMENT_STATUS', typeLabel: '报销状态', dictCode: 'PENDING', dictLabel: '待确认', sort: 1, enabled: true, isSystem: true },
  { id: '17', typeCode: 'REIMBURSEMENT_STATUS', typeLabel: '报销状态', dictCode: 'CONFIRMED', dictLabel: '已确认', sort: 2, enabled: true, isSystem: true },
  { id: '18', typeCode: 'REIMBURSEMENT_STATUS', typeLabel: '报销状态', dictCode: 'REJECTED', dictLabel: '已驳回', sort: 3, enabled: true, isSystem: true },
  { id: '19', typeCode: 'REIMBURSEMENT_STATUS', typeLabel: '报销状态', dictCode: 'CANCELLED', dictLabel: '已取消', sort: 4, enabled: true, isSystem: true },

  // 库存流水类型
  { id: '20', typeCode: 'INVENTORY_FLOW_TYPE', typeLabel: '库存流水类型', dictCode: 'INBOUND', dictLabel: '入库', sort: 1, enabled: true, isSystem: true },
  { id: '21', typeCode: 'INVENTORY_FLOW_TYPE', typeLabel: '库存流水类型', dictCode: 'OUTBOUND', dictLabel: '出库', sort: 2, enabled: true, isSystem: true },
  { id: '22', typeCode: 'INVENTORY_FLOW_TYPE', typeLabel: '库存流水类型', dictCode: 'ADJUSTMENT', dictLabel: '盘点调整', sort: 3, enabled: true, isSystem: true },

  // 官方结算状态
  { id: '23', typeCode: 'OFFICIAL_SETTLEMENT_STATUS', typeLabel: '官方结算状态', dictCode: 'UNSETTLED', dictLabel: '未结算', sort: 1, enabled: true, isSystem: true },
  { id: '24', typeCode: 'OFFICIAL_SETTLEMENT_STATUS', typeLabel: '官方结算状态', dictCode: 'SETTLED', dictLabel: '已结算', sort: 2, enabled: true, isSystem: true },
];
