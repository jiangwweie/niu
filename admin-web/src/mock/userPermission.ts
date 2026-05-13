import type { SystemUser, RoleInfo, PermissionNode } from '@/types';

export const mockUsers: SystemUser[] = [
  { id: 'U001', userNo: 'EMP-001', name: '老板', phone: '13800138001', store: '天河总店', roleName: '超级管理员', roleCode: 'ADMIN', enabled: true, lastLoginTime: '2023-10-24 10:00:00' },
  { id: 'U002', userNo: 'EMP-002', name: '李经理', phone: '13800138002', store: '天河总店', roleName: '门店管理员', roleCode: 'STORE_MANAGER', enabled: true, lastLoginTime: '2023-10-24 09:30:00' },
  { id: 'U003', userNo: 'EMP-003', name: '王财务', phone: '13800138003', store: '天河总店', roleName: '财务', roleCode: 'FINANCE', enabled: true, lastLoginTime: '2023-10-23 18:00:00' },
  { id: 'U004', userNo: 'EMP-004', name: '张工', phone: '13800138004', store: '天河总店', roleName: '维修员', roleCode: 'REPAIRER', enabled: true, lastLoginTime: '2023-10-24 08:00:00' },
  { id: 'U005', userNo: 'EMP-005', name: '赵前台', phone: '13800138005', store: '天河总店', roleName: '前台', roleCode: 'RECEPTIONIST', enabled: true, lastLoginTime: '2023-10-24 08:30:00' },
  { id: 'U006', userNo: 'EMP-006', name: '陈维修', phone: '13800138006', store: '岗顶分店', roleName: '维修员', roleCode: 'REPAIRER', enabled: true, lastLoginTime: '2023-10-24 08:15:00' },
  { id: 'U007', userNo: 'EMP-007', name: '孙前台', phone: '13800138007', store: '岗顶分店', roleName: '前台', roleCode: 'RECEPTIONIST', enabled: false, remark: '已离职' },
  { id: 'U008', userNo: 'EMP-008', name: '周小工', phone: '13800138008', store: '天河总店', roleName: '维修员', roleCode: 'REPAIRER', enabled: true }
];

export const mockRoles: RoleInfo[] = [
  { id: 'R001', roleCode: 'ADMIN', roleName: '超级管理员', description: '拥有系统所有权限', userCount: 1, enabled: true },
  { id: 'R002', roleCode: 'STORE_MANAGER', roleName: '门店管理员', description: '管理门店日常运营，除了系统设置外的所有权限', userCount: 1, enabled: true },
  { id: 'R003', roleCode: 'FINANCE', roleName: '财务', description: '查看财务报表、成本核算、管理报销请求', userCount: 1, enabled: true },
  { id: 'R004', roleCode: 'REPAIRER', roleName: '维修员', description: '处理工单、领用配件、提交报销', userCount: 3, enabled: true },
  { id: 'R005', roleCode: 'RECEPTIONIST', roleName: '前台', description: '创建工单、协助客户支付、查看简单库存', userCount: 2, enabled: true }
];

export const mockPermissions: PermissionNode[] = [
  { id: 'P01', permCode: 'USER_MANAGE', permName: '用户管理', module: '系统管理', description: '用户列表查看与编辑配置', isCore: false },
  { id: 'P02', permCode: 'ROLE_MANAGE', permName: '角色管理', module: '系统管理', description: '角色分配与权限点维护', isCore: false },
  { id: 'P03', permCode: 'DICT_MANAGE', permName: '字典管理', module: '系统管理', description: '业务字典配置', isCore: false },
  { id: 'P04', permCode: 'PART_MANAGE', permName: '配件管理', module: '库存模块', description: '配件基础资料维护', isCore: true },
  { id: 'P05', permCode: 'INVENTORY_VIEW', permName: '库存查看', module: '库存模块', description: '查看实时库存与预测情况', isCore: true },
  { id: 'P06', permCode: 'INVENTORY_INBOUND', permName: '库存入库', module: '库存模块', description: '新增采购入库单据', isCore: true },
  { id: 'P07', permCode: 'INVENTORY_ADJUST', permName: '库存调整', module: '库存模块', description: '盘点与报损', isCore: true },
  { id: 'P08', permCode: 'WORK_ORDER_CREATE', permName: '新建工单', module: '工单模块', description: '开单', isCore: true },
  { id: 'P09', permCode: 'WORK_ORDER_UPDATE', permName: '编辑工单', module: '工单模块', description: '修改已创建的工单信息', isCore: true },
  { id: 'P10', permCode: 'WORK_ORDER_SUBMIT', permName: '提交工单', module: '工单模块', description: '提交完工操作', isCore: true },
  { id: 'P11', permCode: 'WORK_ORDER_CANCEL', permName: '取消工单', module: '工单模块', description: '作废工单', isCore: true },
  { id: 'P12', permCode: 'WORK_ORDER_SETTLE', permName: '工单结算', module: '工单模块', description: '工单客户支付结算', isCore: true },
  { id: 'P13', permCode: 'PAYMENT_RECORD', permName: '支付记录', module: '财务模块', description: '查看客户支付明细', isCore: true },
  { id: 'P14', permCode: 'REFUND_RECORD', permName: '退款记录', module: '财务模块', description: '查看退款明细', isCore: true },
  { id: 'P15', permCode: 'OFFICIAL_SETTLEMENT_MANAGE', permName: '官方结算管理', module: '财务模块', description: '维护官方售后结算订单', isCore: true },
  { id: 'P16', permCode: 'REIMBURSEMENT_SUBMIT', permName: '报销申请', module: '财务模块', description: '提交门店开支与报销', isCore: false },
  { id: 'P17', permCode: 'REIMBURSEMENT_CONFIRM', permName: '报销确认', module: '财务模块', description: '老板或财务确认报销', isCore: false },
  { id: 'P18', permCode: 'FINANCE_VIEW', permName: '财务报表查看', module: '财务模块', description: '查看综合利润率与收支详情', isCore: true },
  { id: 'P19', permCode: 'EXCEL_EXPORT', permName: 'Excel 导出', module: '通用功能', description: '全量报表数据导出下载', isCore: false }
];
