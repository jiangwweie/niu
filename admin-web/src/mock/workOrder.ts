import { WorkOrderRecord } from '@/types';

export const mockWorkOrders: WorkOrderRecord[] = [
  {
    id: 'WO1001',
    orderNo: 'WO-20231024-001',
    customerName: '张三',
    phone: '13800138000',
    scooterModel: 'NQi GT',
    vin: 'VIN1234567890NQI',
    batteryNo: 'BAT12345NQI',
    status: 'DRAFT',
    receivableAmount: 0,
    paidAmount: 0,
    refundedAmount: 0,
    actualAmount: 0,
    isOfficial: false,
    createdAt: '2023-10-24 09:30:00',
    remark: '客户报修前轮异响，等待提交后检查。',
    chargeItems: []
  },
  {
    id: 'WO1002',
    orderNo: 'WO-20231024-002',
    customerName: '李四',
    phone: '13900139000',
    scooterModel: 'MQiL',
    vin: 'VIN1234567890MQL',
    status: 'REPAIRING',
    receivableAmount: 180,
    paidAmount: 0,
    refundedAmount: 0,
    actualAmount: 0,
    isOfficial: true,
    officialOrderNo: 'OFF-2023-88801',
    officialSettlementStatus: 'unsettled',
    officialSettlementAmount: 120,
    createdAt: '2023-10-24 10:15:00',
    chargeItems: [
      {
        id: 'CI001',
        type: 'LABOR',
        itemName: '全车电路检测',
        quantity: 1,
        unitPrice: 180,
        lineAmount: 180,
        affectsInventory: false
      }
    ]
  },
  {
    id: 'WO1003',
    orderNo: 'WO-20231024-003',
    customerName: '王五',
    phone: '13700137000',
    scooterModel: 'UQi+',
    vin: 'VIN1234567890UQI',
    status: 'REPAIRING',
    receivableAmount: 450,
    paidAmount: 200,
    refundedAmount: 0,
    actualAmount: 200,
    isOfficial: false,
    createdAt: '2023-10-24 11:20:00',
    remark: '订购定制坐垫，已收部分定金。',
    chargeItems: [
      {
        id: 'CI002',
        type: 'PART',
        itemName: 'UQi+ 定制真皮坐垫',
        partCode: 'PT-SEAT-001',
        quantity: 1,
        unitPrice: 400,
        lineAmount: 400,
        costAmount: 280,
        affectsInventory: true
      },
      {
        id: 'CI003',
        type: 'LABOR',
        itemName: '坐垫更换工时',
        quantity: 1,
        unitPrice: 50,
        lineAmount: 50,
        affectsInventory: false
      }
    ]
  },
  {
    id: 'WO1004',
    orderNo: 'WO-20231024-004',
    customerName: '赵六',
    phone: '13600136000',
    scooterModel: 'GOVA G0',
    vin: 'VIN1234567890GVA',
    status: 'REPAIRING',
    receivableAmount: 680,
    paidAmount: 0,
    refundedAmount: 0,
    actualAmount: 0,
    isOfficial: true,
    officialOrderNo: 'OFF-2023-88802',
    officialSettlementStatus: 'unsettled',
    officialSettlementAmount: 500,
    createdAt: '2023-10-24 13:00:00',
    remark: '保内电机更换，电机已到店。',
    chargeItems: [
      {
        id: 'CI004',
        type: 'PART',
        itemName: 'G0 驱动电机',
        partCode: 'PT-MT-002',
        quantity: 1,
        unitPrice: 580,
        lineAmount: 580,
        costAmount: 400,
        affectsInventory: true
      },
      {
        id: 'CI005',
        type: 'LABOR',
        itemName: '电机更换工时',
        quantity: 1,
        unitPrice: 100,
        lineAmount: 100,
        affectsInventory: false
      }
    ]
  },
  {
    id: 'WO1005',
    orderNo: 'WO-20231024-005',
    customerName: '孙七',
    phone: '13500135000',
    scooterModel: 'NQi Sport',
    vin: 'VIN1234567890NQS',
    status: 'DELIVERED',
    receivableAmount: 320,
    paidAmount: 320,
    refundedAmount: 0,
    actualAmount: 320,
    isOfficial: false,
    createdAt: '2023-10-24 14:45:00',
    chargeItems: [
      {
        id: 'CI006',
        type: 'PART',
        itemName: 'NQi 前大灯总成',
        partCode: 'PT-LT-015',
        quantity: 1,
        unitPrice: 280,
        lineAmount: 280,
        costAmount: 180,
        affectsInventory: true
      },
      {
        id: 'CI007',
        type: 'LABOR',
        itemName: '大灯更换工时',
        quantity: 1,
        unitPrice: 40,
        lineAmount: 40,
        affectsInventory: false
      }
    ]
  },
  {
    id: 'WO1006',
    orderNo: 'WO-20231024-006',
    customerName: '周八',
    phone: '13400134000',
    scooterModel: 'UQi Sport',
    vin: 'VIN1234567890UQS',
    status: 'CANCELLED',
    receivableAmount: 150,
    paidAmount: 150,
    refundedAmount: 150,
    actualAmount: 0,
    isOfficial: false,
    createdAt: '2023-10-24 15:30:00',
    remark: '客户觉得维修贵，已全额退款并取消。',
    chargeItems: [
      {
        id: 'CI008',
        type: 'OTHER',
        itemName: '上门拖车费',
        quantity: 1,
        unitPrice: 150,
        lineAmount: 150,
        affectsInventory: false
      }
    ]
  },
  {
    id: 'WO1007',
    orderNo: 'WO-20231024-007',
    customerName: '吴九',
    phone: '13300133000',
    scooterModel: 'MQi2',
    vin: 'VIN1234567890MQ2',
    status: 'DELIVERED',
    receivableAmount: 0,
    paidAmount: 0,
    refundedAmount: 0,
    actualAmount: 0,
    isOfficial: true,
    officialOrderNo: 'OFF-2023-88803',
    officialSettlementStatus: 'settled',
    officialSettlementAmount: 85,
    createdAt: '2023-10-24 16:10:00',
    remark: '官方首保，免收客户费用，与总部已结算。',
    chargeItems: [
      {
        id: 'CI009',
        type: 'OTHER',
        itemName: '官方首次保养',
        quantity: 1,
        unitPrice: 0,
        lineAmount: 0,
        affectsInventory: false
      }
    ]
  },
  {
    id: 'WO1008',
    orderNo: 'WO-20231024-008',
    customerName: '郑十',
    phone: '13200132000',
    scooterModel: 'NQi GT',
    vin: 'VIN1234567890NQG',
    status: 'REPAIR_DONE',
    receivableAmount: 1200,
    paidAmount: 1000,
    refundedAmount: 0,
    actualAmount: 1000,
    isOfficial: false,
    createdAt: '2023-10-24 17:05:00',
    remark: '大修，实收金额不足，由于是老客户先提车。后续需补尾款。',
    chargeItems: [
      {
        id: 'CI010',
        type: 'PART',
        itemName: 'NQi GT 控制器总成',
        partCode: 'PT-CT-005',
        quantity: 1,
        unitPrice: 900,
        lineAmount: 900,
        costAmount: 650,
        affectsInventory: true
      },
      {
        id: 'CI011',
        type: 'LABOR',
        itemName: '线路重排与更换工时',
        quantity: 1,
        unitPrice: 300,
        lineAmount: 300,
        affectsInventory: false
      }
    ]
  }
];
