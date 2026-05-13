import type { ExportTypeInfo, ExportTask } from '@/types';

export const mockExportTypes: ExportTypeInfo[] = [
  { code: 'WORK_ORDER_LIST', name: '工单列表', description: '导出维修工单基础信息、客户信息、状态、金额与官方售后标记。', targetRole: '老板、门店管理员', dataSourceDesc: '工单导出应以后端工单查询结果为准。' },
  { code: 'FINANCE_REPORT', name: '财务报表', description: '导出客户支付收入、官方结算收入、成本与利润汇总。', targetRole: '老板、财务', dataSourceDesc: '财务与利润导出应以后端从业务明细汇总后的结果为准，不由前端计算。' },
  { code: 'PROFIT_REPORT', name: '利润报表', description: '导出收入、成本、利润、利润率等经营结果。', targetRole: '老板、财务', dataSourceDesc: '财务与利润导出应以后端从业务明细汇总后的结果为准，不由前端计算。' },
  { code: 'INVENTORY_REPORT', name: '库存报表', description: '导出实际库存、可用库存、预占库存与库存预警信息。', targetRole: '老板、门店管理员、维修员', dataSourceDesc: '库存导出应以后端库存数量与库存流水为准。' },
  { code: 'REIMBURSEMENT_LEDGER', name: '报销台账', description: '导出员工报销申请、确认金额、状态与确认人信息。', targetRole: '老板、财务', dataSourceDesc: '报销导出应以后端报销记录为准，只有已确认报销才计入成本。' }
];

export const mockExportTasks: ExportTask[] = [
  { id: '1', taskNo: 'EXP-20231024-001', typeCode: 'WORK_ORDER_LIST', typeName: '工单列表', conditionSummary: '本月工单, 已完成', creator: '老板', createTime: '2023-10-24 10:00:00', status: 'SUCCESS', fileName: '202310_work_orders.xlsx', remark: '每月例行导出' },
  { id: '2', taskNo: 'EXP-20231024-002', typeCode: 'FINANCE_REPORT', typeName: '财务报表', conditionSummary: '近7天', creator: '财务', createTime: '2023-10-24 11:30:00', status: 'SUCCESS', fileName: 'finance_report_last7days.xlsx' },
  { id: '3', taskNo: 'EXP-20231024-003', typeCode: 'INVENTORY_REPORT', typeName: '库存报表', conditionSummary: '全部来源', creator: '李经理', createTime: '2023-10-24 14:00:00', status: 'PROCESSING' },
  { id: '4', taskNo: 'EXP-20231024-004', typeCode: 'REIMBURSEMENT_LEDGER', typeName: '报销台账', conditionSummary: '待确认', creator: '张工', createTime: '2023-10-24 15:00:00', status: 'PENDING' },
  { id: '5', taskNo: 'EXP-20231023-005', typeCode: 'PROFIT_REPORT', typeName: '利润报表', conditionSummary: '2023-09全月', creator: '老板', createTime: '2023-10-23 09:00:00', status: 'FAILED', remark: '数据量过大超时' },
  { id: '6', taskNo: 'EXP-20231022-006', typeCode: 'WORK_ORDER_LIST', typeName: '工单列表', conditionSummary: '官方售后', creator: '李经理', createTime: '2023-10-22 16:20:00', status: 'SUCCESS', fileName: 'official_work_orders.xlsx' },
  { id: '7', taskNo: 'EXP-20231021-007', typeCode: 'FINANCE_REPORT', typeName: '财务报表', conditionSummary: '按月统计', creator: '财务', createTime: '2023-10-21 10:15:00', status: 'SUCCESS', fileName: 'monthly_finance.xlsx' },
  { id: '8', taskNo: 'EXP-20231020-008', typeCode: 'INVENTORY_REPORT', typeName: '库存报表', conditionSummary: '官方商城供货', creator: '老板', createTime: '2023-10-20 17:45:00', status: 'FAILED', remark: '无权限访问官方供货明细' },
];
