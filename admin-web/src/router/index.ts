import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import BasicLayout from '@/layouts/BasicLayout.vue';
import { firstAccessibleMenuPath } from '@/config/menuCatalog';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: BasicLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'platform/stores',
        name: 'PlatformStores',
        component: () => import('@/views/platform/store/index.vue'),
        meta: { title: '门店管理', requiresPlatform: true, anyPermissions: ['PLATFORM_MANAGE'] }
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', anyPermissions: ['FINANCE_VIEW'] }
      },
      {
        path: 'work-order',
        name: 'WorkOrder',
        component: () => import('@/views/work-order/index.vue'),
        meta: { title: '工单管理', anyPermissions: ['WORK_ORDER_VIEW', 'WORK_ORDER_CREATE', 'WORK_ORDER_SETTLE'] }
      },
      {
        path: 'customers',
        name: 'Customers',
        component: () => import('@/views/customer/index.vue'),
        meta: { title: '客户档案', anyPermissions: ['CUSTOMER_VIEW', 'CUSTOMER_MANAGE'] }
      },
      {
        path: 'vehicles',
        name: 'Vehicles',
        component: () => import('@/views/vehicle/index.vue'),
        meta: { title: '车辆档案', anyPermissions: ['CUSTOMER_VIEW', 'CUSTOMER_MANAGE'] }
      },
      {
        path: 'parts',
        name: 'Parts',
        component: () => import('@/views/parts/index.vue'),
        meta: { title: '配件管理', anyPermissions: ['PART_VIEW', 'PART_CREATE', 'PART_MANAGE'] }
      },
      {
        path: 'inventory',
        name: 'Inventory',
        component: () => import('@/views/inventory/index.vue'),
        meta: { title: '库存管理', anyPermissions: ['INVENTORY_VIEW', 'INVENTORY_INBOUND', 'INVENTORY_ADJUST'] }
      },
      {
        path: 'payment',
        name: 'Payment',
        component: () => import('@/views/payment/index.vue'),
        meta: { title: '收款记录', anyPermissions: ['PAYMENT_RECORD', 'FINANCE_VIEW'] }
      },
      {
        path: 'refund',
        name: 'Refund',
        component: () => import('@/views/refund/index.vue'),
        meta: { title: '退款记录', anyPermissions: ['REFUND_RECORD', 'FINANCE_VIEW'] }
      },
      {
        path: 'settlement',
        name: 'Settlement',
        component: () => import('@/views/settlement/index.vue'),
        meta: { title: '官方结算', anyPermissions: ['OFFICIAL_SETTLEMENT_MANAGE', 'FINANCE_VIEW'] }
      },
      {
        path: 'reimbursement',
        name: 'Reimbursement',
        component: () => import('@/views/reimbursement/index.vue'),
        meta: { title: '报销台账', anyPermissions: ['REIMBURSEMENT_CONFIRM', 'FINANCE_VIEW'] }
      },
      {
        path: 'funding',
        name: 'FundingLedger',
        component: () => import('@/views/funding/index.vue'),
        meta: {
          title: '资方台账',
          anyPermissions: [
            'FUNDING_APPLICATION_VIEW',
            'FUNDING_APPLICATION_MANAGE',
            'FUNDING_APPLICATION_AUDIT',
            'FUNDING_CONTRACT_MANAGE',
            'FUNDING_LEDGER_VIEW',
            'FUNDING_LEDGER_MANAGE',
            'FUNDING_PAYMENT_RECORD',
            'FUNDING_EXPORT',
            'FUNDING_IMPORT',
          ]
        }
      },
      {
        path: 'finance',
        name: 'Finance',
        component: () => import('@/views/finance/index.vue'),
        meta: { title: '财务报表', anyPermissions: ['FINANCE_VIEW'] }
      },
      {
        path: 'finance/cashier-report',
        name: 'CashierReport',
        component: () => import('@/views/finance/cashier-report.vue'),
        meta: { title: '收银日报', anyPermissions: ['FINANCE_VIEW'] }
      },
      {
        path: 'dictionary',
        name: 'Dictionary',
        component: () => import('@/views/dictionary/index.vue'),
        meta: { title: '基础配置', anyPermissions: ['DICT_MANAGE', 'PLATFORM_MANAGE'] }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '员工与权限', anyPermissions: ['USER_MANAGE', 'ROLE_MANAGE'] }
      },
      {
        path: 'store',
        name: 'Store',
        component: () => import('@/views/store/index.vue'),
        meta: { title: '门店配置', anyPermissions: ['STORE_MANAGE'] }
      },
      {
        path: 'change-password',
        name: 'ChangePassword',
        component: () => import('@/views/change-password/index.vue'),
        meta: { title: '修改密码' }
      },
      {
        path: 'export',
        name: 'Export',
        component: () => import('@/views/export/index.vue'),
        meta: { title: '数据导出', anyPermissions: ['EXCEL_EXPORT'] }
      },
      {
        path: 'trial-data',
        name: 'TrialData',
        component: () => import('@/views/trial-data/index.vue'),
        meta: { title: '数据清理', roles: ['SUPER_ADMIN'] }
      },
      {
        path: '403',
        name: 'AccessDenied',
        component: () => import('@/views/access-denied/index.vue'),
        meta: { title: '无权限访问' }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory('/'),
  routes
});

import { useAuthStore } from '@/stores/auth';
import type { AuthUser } from '@/stores/auth';
import { getMe } from '@/api/auth';

function firstAccessiblePath(user: AuthUser | null): string {
  return firstAccessibleMenuPath(user);
}

function canAccessRoute(user: AuthUser | null, to: any): boolean {
  if (!user || to.path === '/403' || to.path === '/change-password') return true;

  const meta = to.meta || {};
  const permissions = user.permissionCodes || [];
  const roles = user.roleCodes || [];
  const requiredRoles = (meta.roles || []) as string[];
  const anyPermissions = (meta.anyPermissions || []) as string[];

  if (requiredRoles.length > 0 && !requiredRoles.some(role => roles.includes(role))) {
    return false;
  }
  if (anyPermissions.length > 0 && !anyPermissions.some(code => permissions.includes(code))) {
    return false;
  }
  return true;
}

function nextAfterAuth(user: AuthUser, to: any, next: any) {
  if (to.path === '/') {
    return next({ path: firstAccessiblePath(user) });
  }
  if (user.passwordMustChange && to.path !== '/change-password') {
    return next({ path: '/change-password' });
  }
  if (user.accountType === 'PLATFORM' && !to.path.startsWith('/platform') && to.path !== '/change-password' && to.path !== '/user' && to.path !== '/dictionary' && to.path !== '/403') {
    return next({ path: '/platform/stores' });
  }
  if (user.accountType === 'STORE' && to.path.startsWith('/platform')) {
    return next({ path: firstAccessiblePath(user) });
  }
  if (!canAccessRoute(user, to)) {
    return next({ path: '/403', query: { from: to.fullPath } });
  }
  return next();
}

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore();
  const token = authStore.accessToken;

  if (to.path === '/login') {
    if (token) {
      return next({ path: '/' });
    }
    return next();
  }

  if (!token) {
    return next({ path: '/login', query: { redirect: to.fullPath } });
  }

  // Token exists, check user
  if (!authStore.user) {
    try {
      const user = await getMe();
      authStore.setUser(user);
      return nextAfterAuth(user, to, next);
    } catch (error) {
      // 401 interceptor handles redirection, but in case it doesn't:
      authStore.clearAuth();
      next({ path: '/login', query: { redirect: to.fullPath } });
    }
  } else {
    return nextAfterAuth(authStore.user, to, next);
  }
});

// Update page title automatically (optional enhancement)
router.afterEach((to) => {
  if (to.meta && to.meta.title) {
    document.title = `${to.meta.title} - 授权店维修售后库存管理系统`;
  }
});

export default router;
