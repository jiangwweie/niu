import { DashboardData } from '@/types/dashboard';

export const mockDashboardData: DashboardData = {
  stats: {
    todayOrders: 24,
    todayCustomerIncome: 4580.50,
    todayOfficialIncome: 1200.00,
    pendingReimbursement: 3,
    inventoryWarnings: 5,
    unsettledOrders: 8
  },
  incomeSplit: {
    customerIncome: {
      parts: 3200.00,
      labor: 1100.50,
      other: 280.00
    },
    officialIncome: {
      amount: 1200.00
    }
  },
  todos: [
    { id: '1', type: 'reimbursement', content: '待确认报销记录', count: 3 },
    { id: '2', type: 'order', content: '未结算工单（需财务审核）', count: 8 },
    { id: '3', type: 'inventory', content: '库存低于预警线', count: 5 },
    { id: '4', type: 'official', content: '待录入官方结算单据', count: 2 }
  ],
  recentOrders: [
    { id: '101', orderNo: 'WO-20231024-001', customerName: '张三', scooterModel: 'NQi GT', status: 'completed', receivableAmount: 450, actualAmount: 450, isOfficial: false, createdAt: '2023-10-24 09:30:00' },
    { id: '102', orderNo: 'WO-20231024-002', customerName: '李四', scooterModel: 'MQiL', status: 'pending', receivableAmount: 120, actualAmount: 0, isOfficial: true, createdAt: '2023-10-24 10:15:00' },
    { id: '103', orderNo: 'WO-20231024-003', customerName: '王五', scooterModel: 'UQi+', status: 'paid', receivableAmount: 880, actualAmount: 880, isOfficial: false, createdAt: '2023-10-24 11:20:00' },
    { id: '104', orderNo: 'WO-20231024-004', customerName: '赵六', scooterModel: 'GOVA G0', status: 'closed', receivableAmount: 0, actualAmount: 0, isOfficial: false, createdAt: '2023-10-24 13:00:00' },
    { id: '105', orderNo: 'WO-20231024-005', customerName: '孙七', scooterModel: 'NQi Sport', status: 'paid', receivableAmount: 320, actualAmount: 320, isOfficial: true, createdAt: '2023-10-24 14:45:00' }
  ],
  inventoryWarnings: [
    { id: 'P001', partCode: 'PT-BR-001', partName: '前刹车片组件', availableStock: 2, warningThreshold: 10, location: 'A区-02架' },
    { id: 'P002', partCode: 'PT-TY-012', partName: '90/90-12 真空胎', availableStock: 1, warningThreshold: 5, location: 'B区-大件存放区' },
    { id: 'P003', partCode: 'PT-BT-005', partName: '60V26Ah 锂电池', availableStock: 0, warningThreshold: 2, location: 'C区-电池柜' },
    { id: 'P004', partCode: 'PT-LT-008', partName: 'LED 转向灯', availableStock: 3, warningThreshold: 15, location: 'A区-05架' },
    { id: 'P005', partCode: 'PT-CT-002', partName: 'NQi 中控控制器', availableStock: 1, warningThreshold: 3, location: '特殊件库-01架' }
  ]
};
