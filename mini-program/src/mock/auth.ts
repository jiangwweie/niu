import { User } from '../types/auth';

export const mockUsers: User[] = [
  {
    userId: 'user_001',
    userName: '维修员张三',
    roleCode: 'TECHNICIAN',
    roleName: '维修员',
    storeId: 'store_001',
    storeName: '小牛电动北京望京店',
    permissionCodes: ['WORK_ORDER_CREATE', 'WORK_ORDER_UPDATE', 'INVENTORY_VIEW']
  },
  {
    userId: 'user_002',
    userName: '店长李四',
    roleCode: 'MANAGER',
    roleName: '门店管理员',
    storeId: 'store_001',
    storeName: '小牛电动北京望京店',
    permissionCodes: ['WORK_ORDER_ALL', 'INVENTORY_ALL', 'FINANCE_VIEW']
  },
  {
    userId: 'user_003',
    userName: '前台王五',
    roleCode: 'FRONT_DESK',
    roleName: '前台',
    storeId: 'store_001',
    storeName: '小牛电动北京望京店',
    permissionCodes: ['WORK_ORDER_CREATE', 'WORK_ORDER_VIEW']
  }
];

export const defaultMockUser = mockUsers[0];
