import { PaymentRecord } from '@/types/payment';

export const mockPayments: PaymentRecord[] = [
  {
    id: 'PAY001',
    paymentNo: 'PAY-20231024-001',
    orderNo: 'WO-20231024-001',
    customerName: '张先生',
    amount: 150.00,
    method: 'wechat',
    paymentTime: '2023-10-24 10:15:00',
    payee: '前台小李',
    createdAt: '2023-10-24 10:15:00',
    remark: '后轮补胎及工时费'
  },
  {
    id: 'PAY002',
    paymentNo: 'PAY-20231024-002',
    orderNo: 'WO-20231024-002',
    customerName: '李大爷',
    amount: 50.00,
    method: 'cash',
    paymentTime: '2023-10-24 11:30:00',
    payee: '店长',
    createdAt: '2023-10-24 11:30:00' // 无备注
  },
  {
    id: 'PAY003',
    paymentNo: 'PAY-20231024-003',
    orderNo: 'WO-20231024-003',
    customerName: '王女士',
    amount: 280.00,
    method: 'alipay',
    paymentTime: '2023-10-24 14:20:00',
    payee: '前台小李',
    createdAt: '2023-10-24 14:20:00',
    remark: '保养及换机油'
  },
  {
    id: 'PAY004',
    paymentNo: 'PAY-20231024-004',
    orderNo: 'WO-20231024-004',
    customerName: '赵老板',
    amount: 1200.00,
    method: 'unionpay',
    paymentTime: '2023-10-24 15:45:00',
    payee: '店长',
    createdAt: '2023-10-24 15:45:00',
    remark: '大修，刷卡支付'
  },
  // 同一工单多次付款 / 混合付款
  {
    id: 'PAY005',
    paymentNo: 'PAY-20231024-005',
    orderNo: 'WO-20231024-005',
    customerName: '孙小姐',
    amount: 200.00,
    method: 'wechat',
    paymentTime: '2023-10-24 16:00:00',
    payee: '前台小李',
    createdAt: '2023-10-24 16:00:00',
    remark: '定金，微信支付'
  },
  {
    id: 'PAY006',
    paymentNo: 'PAY-20231024-006',
    orderNo: 'WO-20231024-005',
    customerName: '孙小姐',
    amount: 350.00,
    method: 'cash',
    paymentTime: '2023-10-24 16:30:00',
    payee: '前台小李',
    createdAt: '2023-10-24 16:30:00',
    remark: '尾款，现金支付'
  },
  // 官方售后工单客户支付 (可能产生一些额外的非保修项目)
  {
    id: 'PAY007',
    paymentNo: 'PAY-20231024-007',
    orderNo: 'WO-20231024-006',
    customerName: '周先生',
    amount: 50.00,
    method: 'wechat',
    paymentTime: '2023-10-24 17:10:00',
    payee: '店长',
    createdAt: '2023-10-24 17:10:00',
    remark: '官方售后附带的非保修洗车费用'
  },
  // 第三方维修工单客户支付
  {
    id: 'PAY008',
    paymentNo: 'PAY-20231024-008',
    orderNo: 'WO-20231024-007',
    customerName: '吴姐',
    amount: 180.00,
    method: 'alipay',
    paymentTime: '2023-10-25 09:15:00',
    payee: '前台小李',
    createdAt: '2023-10-25 09:15:00',
    remark: '第三方配件改装'
  },
  // 再混合几条别的支付
  {
    id: 'PAY009',
    paymentNo: 'PAY-20231025-001',
    orderNo: 'WO-20231025-001',
    customerName: '郑哥',
    amount: 15.00,
    method: 'cash',
    paymentTime: '2023-10-25 09:40:00',
    payee: '店长',
    createdAt: '2023-10-25 09:40:00'
  },
  {
    id: 'PAY010',
    paymentNo: 'PAY-20231025-002',
    orderNo: 'WO-20231025-002',
    customerName: '冯师傅',
    amount: 300.00,
    method: 'wechat',
    paymentTime: '2023-10-25 10:20:00',
    payee: '前台小李',
    createdAt: '2023-10-25 10:20:00',
    remark: '加装电池箱'
  }
];
