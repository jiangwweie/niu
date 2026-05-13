import { ReimbursementRecord } from '@/types/reimbursement';

export const mockReimbursements: ReimbursementRecord[] = [
  {
    id: 'RB001',
    reimbursementNo: 'RB-20231001-001',
    applicant: '张三',
    purpose: '门店耗材采购（绝缘胶带、扎带）',
    amount: 150.00,
    status: 'PENDING',
    createdAt: '2023-10-01 10:00:00',
    remark: '急用耗材'
  },
  {
    id: 'RB002',
    reimbursementNo: 'RB-20231002-001',
    applicant: '李四',
    purpose: '临时维修工具（扭矩扳手）',
    amount: 350.00,
    approvedAmount: 350.00,
    status: 'CONFIRMED',
    createdAt: '2023-10-02 09:30:00',
    confirmer: '老板',
    confirmedAt: '2023-10-02 14:00:00',
    remark: '店里缺这个工具'
  },
  {
    id: 'RB003',
    reimbursementNo: 'RB-20231003-001',
    applicant: '王五',
    purpose: '物流费（客户加急件）',
    amount: 45.00,
    approvedAmount: 45.00,
    status: 'CONFIRMED',
    createdAt: '2023-10-03 11:15:00',
    confirmer: '门店经理',
    confirmedAt: '2023-10-03 12:00:00'
  },
  {
    id: 'RB004',
    reimbursementNo: 'RB-20231004-001',
    applicant: '赵六',
    purpose: '跨店调配件交通费',
    amount: 60.00,
    status: 'REJECTED',
    createdAt: '2023-10-04 08:45:00',
    processor: '老板',
    processedAt: '2023-10-04 10:00:00',
    rejectReason: '交通费未提前报备，不同意报销',
    remark: '打车去西城区调货'
  },
  {
    id: 'RB005',
    reimbursementNo: 'RB-20231005-001',
    applicant: '张三',
    purpose: '清洁用品（洗车液、毛巾）',
    amount: 120.00,
    status: 'CANCELLED',
    createdAt: '2023-10-05 14:20:00',
    processor: '张三',
    processedAt: '2023-10-05 15:00:00',
    cancelReason: '买错品牌了，已退货',
    remark: '洗车房用的'
  },
  {
    id: 'RB006',
    reimbursementNo: 'RB-20231006-001',
    applicant: '李四',
    purpose: '店内饮水机滤芯更换',
    amount: 200.00,
    approvedAmount: 180.00,
    status: 'CONFIRMED',
    createdAt: '2023-10-06 09:10:00',
    confirmer: '老板',
    confirmedAt: '2023-10-06 18:30:00',
    remark: '有20块钱是个人买的水，打回只报180'
  },
  {
    id: 'RB007',
    reimbursementNo: 'RB-20231007-001',
    applicant: '王五',
    purpose: '客户维权安抚小礼品',
    amount: 88.00,
    status: 'PENDING',
    createdAt: '2023-10-07 16:40:00'
  },
  {
    id: 'RB008',
    reimbursementNo: 'RB-20231008-001',
    applicant: '赵六',
    purpose: '电动车道路救援油费',
    amount: 50.00,
    approvedAmount: 50.00,
    status: 'CONFIRMED',
    createdAt: '2023-10-08 10:20:00',
    confirmer: '门店经理',
    confirmedAt: '2023-10-08 11:00:00',
    remark: '去东湖救车'
  },
  {
    id: 'RB009',
    reimbursementNo: 'RB-20231009-001',
    applicant: '张三',
    purpose: '门店宽带费半年',
    amount: 600.00,
    status: 'REJECTED',
    createdAt: '2023-10-09 13:00:00',
    processor: '老板',
    processedAt: '2023-10-09 14:00:00',
    rejectReason: '宽带费由联通直接划扣公司账户，无需提交报销'
  },
  {
    id: 'RB010',
    reimbursementNo: 'RB-20231010-001',
    applicant: '李四',
    purpose: '店庆宣传横幅制作',
    amount: 320.00,
    status: 'PENDING',
    createdAt: '2023-10-10 09:50:00',
    remark: '老板要求的横幅，加急制作费'
  }
];
