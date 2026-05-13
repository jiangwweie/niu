import { RefundRecord } from '@/types/refund';

export const mockRefunds: RefundRecord[] = [
  // 定金退款
  {
    id: 'REF001',
    refundNo: 'REF-20231024-001',
    orderNo: 'WO-20231024-001',
    customerName: '张先生',
    amount: 50.00,
    method: 'wechat',
    refundTime: '2023-10-24 16:00:00',
    operator: '店长',
    reason: '定金退还',
    createdAt: '2023-10-24 16:00:00',
    remark: '服务完成后原路退回定金'
  },
  // 多收退款
  {
    id: 'REF002',
    refundNo: 'REF-20231024-002',
    orderNo: 'WO-20231024-003',
    customerName: '王女士',
    amount: 20.00,
    method: 'alipay',
    refundTime: '2023-10-25 10:15:00',
    operator: '前台小李',
    reason: '多收配件费退回',
    createdAt: '2023-10-25 10:15:00'
  },
  // 已结算后退款记录
  {
    id: 'REF003',
    refundNo: 'REF-20231025-001',
    orderNo: 'WO-20231020-010', // 假设是很早之前已结算的
    customerName: '林大姐',
    amount: 150.00,
    method: 'wechat',
    refundTime: '2023-10-25 11:30:00',
    operator: '店长',
    reason: '客户售后投诉，协商部分退款',
    createdAt: '2023-10-25 11:30:00',
    remark: '工单已结算，总部特批退款'
  },
  // 未结算取消后退款记录
  {
    id: 'REF004',
    refundNo: 'REF-20231025-002',
    orderNo: 'WO-20231025-003',
    customerName: '李明',
    amount: 500.00,
    method: 'alipay',
    refundTime: '2023-10-25 13:45:00',
    operator: '前台小李',
    reason: '工单取消，金额全退',
    createdAt: '2023-10-25 13:45:00',
    remark: '客户因故取消维修，已全额退款'
  },
  // 微信退款无备注
  {
    id: 'REF005',
    refundNo: 'REF-20231026-001',
    orderNo: 'WO-20231026-001',
    customerName: '老赵',
    amount: 15.00,
    method: 'wechat',
    refundTime: '2023-10-26 09:20:00',
    operator: '前台小李',
    reason: '取消更换机油',
    createdAt: '2023-10-26 09:20:00'
  },
  // 现金退款
  {
    id: 'REF006',
    refundNo: 'REF-20231026-002',
    orderNo: 'WO-20231026-005',
    customerName: '刘爷',
    amount: 30.00,
    method: 'cash',
    refundTime: '2023-10-26 14:10:00',
    operator: '店长',
    reason: '老客户优惠',
    createdAt: '2023-10-26 14:10:00',
    remark: '店长特批，退出现金'
  },
  {
    id: 'REF007',
    refundNo: 'REF-20231027-001',
    orderNo: 'WO-20231027-002',
    customerName: '张小姐',
    amount: 70.00,
    method: 'wechat',
    refundTime: '2023-10-27 10:05:00',
    operator: '前台小李',
    reason: '更换更便宜配件，退差价',
    createdAt: '2023-10-27 10:05:00'
  },
  {
    id: 'REF008',
    refundNo: 'REF-20231027-002',
    orderNo: 'WO-20231027-009',
    customerName: '黄老师',
    amount: 200.00,
    method: 'alipay',
    refundTime: '2023-10-27 16:50:00',
    operator: '店长',
    reason: '工单取消',
    createdAt: '2023-10-27 16:50:00',
    remark: '支付宝原路退回200'
  }
];
