import { OfficialSettlementRecord } from '@/types/officialSettlement';

export const mockOfficialSettlements: OfficialSettlementRecord[] = [
  // 未录入官方结算金额
  {
    id: 'SET001',
    orderNo: 'WO-20231024-001',
    customerName: '张先生',
    phone: '13800138000',
    scooterModel: 'NQi GT',
    vin: 'VIN1234567890NQI',
    officialOrderNo: 'NIU-OFF-001',
    orderStatus: 'SETTLED',
    receivableAmount: 0,
    customerActualPaid: 0,
    customerActualRefund: 0,
    settlementStatus: 'NOT_RECORDED',
    createdAt: '2023-10-24 10:00:00'
  },
  // 已录入未结算
  {
    id: 'SET002',
    orderNo: 'WO-20231024-002',
    customerName: '李小姐',
    phone: '13900139000',
    scooterModel: 'MQiL',
    vin: 'VIN1234567890MQL',
    officialOrderNo: 'NIU-OFF-002',
    orderStatus: 'PART_ARRIVED',
    receivableAmount: 0,
    customerActualPaid: 0,
    customerActualRefund: 0,
    settlementStatus: 'PENDING',
    settlementAmount: 150.00,
    remark: '后视镜索赔',
    createdAt: '2023-10-24 11:30:00'
  },
  // 已结算
  {
    id: 'SET003',
    orderNo: 'WO-20231024-003',
    customerName: '王大爷',
    phone: '13700137000',
    scooterModel: 'UQi+',
    vin: 'VIN1234567890UQI',
    officialOrderNo: 'NIU-OFF-003',
    orderStatus: 'SETTLED',
    receivableAmount: 0,
    customerActualPaid: 0,
    customerActualRefund: 0,
    settlementStatus: 'SETTLED',
    settlementAmount: 45.00,
    settlementTime: '2023-10-25 09:15:00',
    remark: '前刹车片索赔，总部已通过打款',
    createdAt: '2023-10-24 14:00:00'
  },
  // 无需结算
  {
    id: 'SET004',
    orderNo: 'WO-20231024-004',
    customerName: '赵老板',
    phone: '13600136000',
    scooterModel: 'GOVA G0',
    vin: 'VIN1234567890GVA',
    orderStatus: 'ACCEPTED',
    receivableAmount: 680,
    customerActualPaid: 0,
    customerActualRefund: 0,
    settlementStatus: 'NOT_REQUIRED',
    remark: '普通维修单，无需官方结算',
    createdAt: '2023-10-24 16:20:00'
  },
  // 客户支付与官方结算同时存在
  {
    id: 'SET005',
    orderNo: 'WO-20231024-005',
    customerName: '孙同学',
    phone: '13500135000',
    scooterModel: 'NQi Sport',
    vin: 'VIN1234567890NQS',
    officialOrderNo: 'NIU-OFF-005',
    orderStatus: 'SETTLED',
    receivableAmount: 320,
    customerActualPaid: 320,
    customerActualRefund: 0,
    settlementStatus: 'SETTLED',
    settlementAmount: 120.00,
    settlementTime: '2023-10-25 10:00:00',
    remark: '电机保修免费，另收灯泡工时与材料费',
    createdAt: '2023-10-24 18:45:00'
  },
  // 客户未支付但官方有结算记录
  {
    id: 'SET006',
    orderNo: 'WO-20231025-001',
    customerName: '周女士',
    phone: '13400134000',
    scooterModel: 'UQi Sport',
    vin: 'VIN1234567890UQS',
    officialOrderNo: 'NIU-OFF-006',
    orderStatus: 'SETTLED',
    receivableAmount: 0,
    customerActualPaid: 0,
    customerActualRefund: 0,
    settlementStatus: 'PENDING',
    settlementAmount: 260.00,
    remark: '电池检测服务补偿',
    createdAt: '2023-10-25 09:30:00'
  },
  // 官方订单号完整
  {
    id: 'SET007',
    orderNo: 'WO-20231025-002',
    customerName: '吴先生',
    phone: '13300133000',
    scooterModel: 'MQi2',
    vin: 'VIN1234567890MQ2',
    officialOrderNo: 'NIU-OFF-4892A-X',
    orderStatus: 'SETTLED',
    receivableAmount: 0,
    customerActualPaid: 0,
    customerActualRefund: 0,
    settlementStatus: 'SETTLED',
    settlementAmount: 18.00,
    settlementTime: '2023-10-26 11:20:00',
    remark: '垫片索赔',
    createdAt: '2023-10-25 11:20:00'
  },
  // 综合情况
  {
    id: 'SET008',
    orderNo: 'WO-20231025-003',
    customerName: '郑师傅',
    phone: '13200132000',
    scooterModel: 'NQi GT',
    vin: 'VIN1234567890NQG',
    officialOrderNo: 'NIU-OFF-008',
    orderStatus: 'ACCEPTED',
    receivableAmount: 1200,
    customerActualPaid: 1000,
    customerActualRefund: 0,
    settlementStatus: 'PENDING',
    settlementAmount: 50.00,
    createdAt: '2023-10-25 14:10:00'
  }
];
