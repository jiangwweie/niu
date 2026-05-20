import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import BasicLayout from '@/layouts/BasicLayout.vue';

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
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: 'Dashboard 首页' }
      },
      {
        path: 'work-order',
        name: 'WorkOrder',
        component: () => import('@/views/work-order/index.vue'),
        meta: { title: '工单管理' }
      },
      {
        path: 'customers',
        name: 'Customers',
        component: () => import('@/views/customer/index.vue'),
        meta: { title: '客户档案' }
      },
      {
        path: 'vehicles',
        name: 'Vehicles',
        component: () => import('@/views/vehicle/index.vue'),
        meta: { title: '车辆档案' }
      },
      {
        path: 'parts',
        name: 'Parts',
        component: () => import('@/views/parts/index.vue'),
        meta: { title: '配件管理' }
      },
      {
        path: 'inventory',
        name: 'Inventory',
        component: () => import('@/views/inventory/index.vue'),
        meta: { title: '库存管理' }
      },
      {
        path: 'payment',
        name: 'Payment',
        component: () => import('@/views/payment/index.vue'),
        meta: { title: '支付记录' }
      },
      {
        path: 'refund',
        name: 'Refund',
        component: () => import('@/views/refund/index.vue'),
        meta: { title: '退款记录' }
      },
      {
        path: 'settlement',
        name: 'Settlement',
        component: () => import('@/views/settlement/index.vue'),
        meta: { title: '官方售后结算' }
      },
      {
        path: 'reimbursement',
        name: 'Reimbursement',
        component: () => import('@/views/reimbursement/index.vue'),
        meta: { title: '报销台账' }
      },
      {
        path: 'finance',
        name: 'Finance',
        component: () => import('@/views/finance/index.vue'),
        meta: { title: '财务报表' }
      },
      {
        path: 'dictionary',
        name: 'Dictionary',
        component: () => import('@/views/dictionary/index.vue'),
        meta: { title: '字典配置' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户与权限' }
      },
      {
        path: 'store',
        name: 'Store',
        component: () => import('@/views/store/index.vue'),
        meta: { title: '门店配置' }
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
        meta: { title: 'Excel 导出中心' }
      },
      {
        path: 'trial-data',
        name: 'TrialData',
        component: () => import('@/views/trial-data/index.vue'),
        meta: { title: '正式启用' }
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
  const permissions = user?.permissionCodes || [];
  const roles = user?.roleCodes || [];
  const hasPermission = (code: string) => permissions.includes(code);
  const hasAnyPermission = (codes: string[]) => codes.some(code => permissions.includes(code));
  const hasRole = (code: string) => roles.includes(code);

  if (hasPermission('FINANCE_VIEW')) return '/dashboard';
  if (hasAnyPermission(['CUSTOMER_VIEW', 'CUSTOMER_MANAGE'])) return '/customers';
  if (hasAnyPermission(['WORK_ORDER_VIEW', 'WORK_ORDER_CREATE', 'WORK_ORDER_SETTLE'])) return '/work-order';
  if (hasAnyPermission(['PART_VIEW', 'PART_MANAGE'])) return '/parts';
  if (hasAnyPermission(['INVENTORY_VIEW', 'INVENTORY_INBOUND', 'INVENTORY_ADJUST'])) return '/inventory';
  if (hasAnyPermission(['USER_MANAGE', 'ROLE_MANAGE'])) return '/user';
  if (hasPermission('STORE_MANAGE')) return '/store';
  if (hasRole('SUPER_ADMIN')) return '/trial-data';
  return '/change-password';
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
      if (user.passwordMustChange && to.path !== '/change-password') {
        return next({ path: '/change-password' });
      }
      if (to.path === '/dashboard' && !user.permissionCodes.includes('FINANCE_VIEW')) {
        return next({ path: firstAccessiblePath(user) });
      }
      next();
    } catch (error) {
      // 401 interceptor handles redirection, but in case it doesn't:
      authStore.clearAuth();
      next({ path: '/login', query: { redirect: to.fullPath } });
    }
  } else {
    if (authStore.user.passwordMustChange && to.path !== '/change-password') {
      return next({ path: '/change-password' });
    }
    if (to.path === '/dashboard' && !authStore.user.permissionCodes.includes('FINANCE_VIEW')) {
      return next({ path: firstAccessiblePath(authStore.user) });
    }
    next();
  }
});

// Update page title automatically (optional enhancement)
router.afterEach((to) => {
  if (to.meta && to.meta.title) {
    document.title = `${to.meta.title} - 小牛售后库存管理系统`;
  }
});

export default router;
