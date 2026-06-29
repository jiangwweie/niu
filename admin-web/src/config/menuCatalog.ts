export interface MenuAuthUser {
  accountType?: string;
  permissionCodes?: string[];
  roleCodes?: string[];
}

export interface MenuItemConfig {
  index: string;
  title: string;
  permissions?: string[];
  roles?: string[];
  platformOnly?: boolean;
  storeOnly?: boolean;
}

export interface MenuGroupConfig {
  index: string;
  title: string;
  permissions?: string[];
  roles?: string[];
  platformOnly?: boolean;
  storeOnly?: boolean;
  children: MenuItemConfig[];
}

export type MenuEntryConfig = MenuItemConfig | MenuGroupConfig;

export const platformMenus: MenuItemConfig[] = [
  {
    index: '/platform/stores',
    title: '门店管理',
    permissions: ['PLATFORM_MANAGE'],
    platformOnly: true,
  },
  {
    index: '/user',
    title: '员工与权限',
    permissions: ['USER_MANAGE', 'ROLE_MANAGE'],
    platformOnly: true,
  },
];

export const storeMenus: MenuEntryConfig[] = [
  {
    index: '/dashboard',
    title: '首页',
    permissions: ['FINANCE_VIEW'],
    storeOnly: true,
  },
  {
    index: 'business',
    title: '业务管理',
    storeOnly: true,
    children: [
      { index: '/work-order', title: '工单管理', permissions: ['WORK_ORDER_VIEW', 'WORK_ORDER_CREATE', 'WORK_ORDER_SETTLE'] },
      { index: '/customers', title: '客户档案', permissions: ['CUSTOMER_VIEW', 'CUSTOMER_MANAGE'] },
      { index: '/vehicles', title: '车辆档案', permissions: ['CUSTOMER_VIEW', 'CUSTOMER_MANAGE'] },
    ],
  },
  {
    index: 'inventory',
    title: '配件库存',
    storeOnly: true,
    children: [
      { index: '/parts', title: '配件管理', permissions: ['PART_VIEW', 'PART_CREATE', 'PART_MANAGE'] },
      { index: '/inventory', title: '库存管理', permissions: ['INVENTORY_VIEW', 'INVENTORY_INBOUND', 'INVENTORY_ADJUST'] },
    ],
  },
  {
    index: 'finance',
    title: '收银财务',
    storeOnly: true,
    children: [
      { index: '/payment', title: '收款记录', permissions: ['PAYMENT_RECORD', 'FINANCE_VIEW'] },
      { index: '/refund', title: '退款记录', permissions: ['REFUND_RECORD', 'FINANCE_VIEW'] },
      { index: '/finance/cashier-report', title: '收银日报', permissions: ['FINANCE_VIEW'] },
      { index: '/finance', title: '财务报表', permissions: ['FINANCE_VIEW'] },
      { index: '/settlement', title: '官方结算', permissions: ['OFFICIAL_SETTLEMENT_MANAGE', 'FINANCE_VIEW'] },
      { index: '/reimbursement', title: '报销台账', permissions: ['REIMBURSEMENT_CONFIRM', 'FINANCE_VIEW'] },
    ],
  },
  {
    index: 'funding',
    title: '资方业务',
    storeOnly: true,
    children: [
      {
        index: '/funding',
        title: '资方台账',
        permissions: [
          'FUNDING_APPLICATION_VIEW',
          'FUNDING_APPLICATION_MANAGE',
          'FUNDING_APPLICATION_AUDIT',
          'FUNDING_CONTRACT_MANAGE',
          'FUNDING_LEDGER_VIEW',
          'FUNDING_LEDGER_MANAGE',
          'FUNDING_PAYMENT_RECORD',
          'FUNDING_EXPORT',
          'FUNDING_IMPORT',
        ],
      },
    ],
  },
  {
    index: 'settings',
    title: '系统设置',
    storeOnly: true,
    children: [
      { index: '/store', title: '门店配置', permissions: ['STORE_MANAGE'] },
      { index: '/user', title: '员工与权限', permissions: ['USER_MANAGE', 'ROLE_MANAGE'] },
      { index: '/dictionary', title: '基础配置', permissions: ['DICT_MANAGE'] },
      { index: '/export', title: '数据导出', permissions: ['EXCEL_EXPORT'] },
      { index: '/trial-data', title: '数据清理', roles: ['SUPER_ADMIN'] },
    ],
  },
];

export function isMenuGroup(entry: MenuEntryConfig): entry is MenuGroupConfig {
  return 'children' in entry;
}

export function canShowMenuEntry(entry: MenuEntryConfig, user: MenuAuthUser | null | undefined): boolean {
  if (!user) return false;
  if (entry.platformOnly && user.accountType !== 'PLATFORM') return false;
  if (entry.storeOnly && user.accountType === 'PLATFORM') return false;

  if (isMenuGroup(entry)) {
    return visibleMenuChildren(entry, user).length > 0;
  }

  const permissions = user.permissionCodes || [];
  const roles = user.roleCodes || [];
  const permissionAllowed = !entry.permissions?.length || entry.permissions.some(code => permissions.includes(code));
  const roleAllowed = !entry.roles?.length || entry.roles.some(code => roles.includes(code));
  return permissionAllowed && roleAllowed;
}

export function visibleMenuChildren(group: MenuGroupConfig, user: MenuAuthUser | null | undefined): MenuItemConfig[] {
  return group.children.filter(child => canShowMenuEntry(child, user));
}

export function visiblePlatformMenus(user: MenuAuthUser | null | undefined): MenuItemConfig[] {
  return platformMenus.filter(item => canShowMenuEntry(item, user));
}

export function visibleStoreMenus(user: MenuAuthUser | null | undefined): MenuEntryConfig[] {
  return storeMenus.filter(entry => canShowMenuEntry(entry, user));
}

export function firstAccessibleMenuPath(user: MenuAuthUser | null | undefined): string {
  if (!user) return '/login';
  if (user.accountType === 'PLATFORM') {
    return visiblePlatformMenus(user)[0]?.index || '/change-password';
  }
  for (const entry of visibleStoreMenus(user)) {
    if (isMenuGroup(entry)) {
      const firstChild = visibleMenuChildren(entry, user)[0];
      if (firstChild) return firstChild.index;
    } else {
      return entry.index;
    }
  }
  return '/change-password';
}

export function defaultOpenMenuGroups(path: string, user: MenuAuthUser | null | undefined): string[] {
  return visibleStoreMenus(user)
    .filter(isMenuGroup)
    .filter(group => group.children.some(child => child.index === path))
    .map(group => group.index);
}
