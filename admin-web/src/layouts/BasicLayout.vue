<template>
  <div class="layout-container">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-logo">
        <div class="logo-icon">NIU</div>
        <span class="logo-text">小牛售后管理</span>
      </div>
      
      <el-menu
        class="custom-menu"
        :default-active="activeMenu"
        router
      >
        <el-menu-item v-if="isPlatform" index="/platform/stores">
          <template #title>门店管理</template>
        </el-menu-item>

        <template v-if="!isPlatform">
          <el-menu-item v-if="hasPermission('FINANCE_VIEW')" index="/dashboard"><template #title>首页</template></el-menu-item>
          <el-menu-item index="/work-order"><template #title>工单管理</template></el-menu-item>
          <el-menu-item
            v-if="hasAnyPermission(['CUSTOMER_VIEW', 'CUSTOMER_MANAGE'])"
            index="/customers"
          ><template #title>客户档案</template></el-menu-item>
          <el-menu-item
            v-if="hasAnyPermission(['CUSTOMER_VIEW', 'CUSTOMER_MANAGE'])"
            index="/vehicles"
          ><template #title>车辆档案</template></el-menu-item>
          <el-menu-item index="/parts"><template #title>配件管理</template></el-menu-item>
          <el-menu-item
            v-if="hasAnyPermission(['INVENTORY_VIEW', 'INVENTORY_INBOUND', 'INVENTORY_ADJUST'])"
            index="/inventory"
          ><template #title>库存管理</template></el-menu-item>
          <el-menu-item v-if="hasPermission('FINANCE_VIEW')" index="/payment"><template #title>支付记录</template></el-menu-item>
          <el-menu-item v-if="hasPermission('FINANCE_VIEW')" index="/refund"><template #title>退款记录</template></el-menu-item>
          <el-menu-item
            v-if="hasAnyPermission(['OFFICIAL_SETTLEMENT_MANAGE', 'FINANCE_VIEW'])"
            index="/settlement"
          ><template #title>官方售后结算</template></el-menu-item>
          <el-menu-item
            v-if="hasAnyPermission(['REIMBURSEMENT_CONFIRM', 'FINANCE_VIEW'])"
            index="/reimbursement"
          ><template #title>报销台账</template></el-menu-item>
          <el-menu-item v-if="hasPermission('FINANCE_VIEW')" index="/finance"><template #title>财务报表</template></el-menu-item>
          <el-menu-item v-if="hasPermission('DICT_MANAGE')" index="/dictionary"><template #title>字典配置</template></el-menu-item>
          <el-menu-item v-if="hasAnyPermission(['USER_MANAGE', 'ROLE_MANAGE'])" index="/user"><template #title>用户与权限</template></el-menu-item>
          <el-menu-item v-if="hasPermission('STORE_MANAGE')" index="/store"><template #title>门店配置</template></el-menu-item>
          <el-menu-item v-if="hasRole('SUPER_ADMIN')" index="/trial-data"><template #title>正式启用</template></el-menu-item>
        </template>
      </el-menu>
      
      <div class="sidebar-footer">
        <div class="version-text">MVP 试运行版</div>
      </div>
    </aside>

    <!-- Main Container -->
    <div class="main-container">
      <!-- Header -->
      <header class="app-header">
        <div class="header-left">
          <h1>小牛维修售后库存管理系统</h1>
          <span class="version-badge">Admin</span>
        </div>
        <div class="header-right">
          <div class="store-info" v-if="!isPlatform">
            <span class="dot"></span>
            <span class="label">当前门店:</span>
            <span class="value">{{ storeDisplayName }}</span>
          </div>
          <div class="store-info" v-else>
            <span class="label">平台管理</span>
          </div>
          <div class="divider"></div>
          <el-dropdown trigger="click">
            <div class="user-profile">
              <div class="avatar">{{ avatarText }}</div>
              <div class="user-meta">
                <span class="username">{{ authStore.user?.realName || authStore.user?.username || '未登录' }}</span>
                <span class="role">{{ roleDisplayName }}</span>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/change-password')">修改密码</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- Main Content -->
      <main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>

      <!-- Footer -->
      <footer class="app-footer">
        <div>小牛维修售后管理系统</div>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { logout } from '@/api/auth';
import { hasPermission, hasAnyPermission, hasRole } from '@/utils/permission';
import { ElMessage } from 'element-plus';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// Highlight current menu item
const activeMenu = computed(() => {
  return route.path;
});

const isPlatform = computed(() => authStore.user?.accountType === 'PLATFORM');

// Role code → Chinese display mapping
const ROLE_LABEL_MAP: Record<string, string> = {
  SUPER_ADMIN: '系统超管',
  STORE_ADMIN: '门店管理员',
  FINANCE: '财务',
  TECHNICIAN_FRONT_DESK: '前台员工',
};

const roleDisplayName = computed(() => {
  if (authStore.user?.accountType === 'PLATFORM') return '平台管理员';
  const codes = authStore.user?.roleCodes;
  if (!codes || codes.length === 0) return '当前用户';
  return ROLE_LABEL_MAP[codes[0]] || codes[0];
});

// Store display: no hardcoded name
const storeDisplayName = computed(() => {
  return authStore.user?.storeName || '未绑定门店';
});

// Avatar: first char of realName or username
const avatarText = computed(() => {
  const name = authStore.user?.realName || authStore.user?.username || '';
  return name.charAt(0).toUpperCase() || 'U';
});

const handleLogout = async () => {
  try {
    await logout();
  } catch (e) {
    // ignore
  } finally {
    authStore.clearAuth();
    ElMessage.success('已退出登录');
    router.push('/login');
  }
};

</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  width: 100%;
  min-width: 0;
  overflow: hidden;
  background-color: #f3f4f6;
  font-family: sans-serif;
}

/* Sidebar */
.sidebar {
  width: 256px;
  background-color: #1a1a1a;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  color: #9ca3af;
}

.sidebar-logo {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  background-color: #000;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background-color: #df001f;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-weight: bold;
  color: white;
  font-size: 12px;
}

.logo-text {
  color: white;
  font-weight: bold;
  font-size: 14px;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.version-text {
  font-size: 11px;
  color: #4b5563;
}

/* Menu Customization */
.custom-menu {
  background-color: transparent !important;
  border-right: none !important;
  flex: 1;
  overflow-y: auto;
  margin-top: 16px;
}

:deep(.custom-menu .el-menu-item) {
  color: #9ca3af !important;
  height: 44px !important;
  line-height: 44px !important;
  margin: 4px 16px;
  border-radius: 6px;
}

:deep(.custom-menu .el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  color: #ffffff !important;
}

:deep(.custom-menu .el-menu-item.is-active) {
  background-color: #df001f !important;
  color: #ffffff !important;
  font-weight: 500;
}

/* Main Container */
.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* Header */
.app-header {
  height: 64px;
  background-color: white;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-left h1 {
  font-size: 18px;
  font-weight: bold;
  color: #1f2937;
  margin: 0;
}

.version-badge {
  padding: 2px 8px;
  background-color: #f3f4f6;
  color: #6b7280;
  font-size: 10px;
  border-radius: 4px;
  border: 1px solid #e5e7eb;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
  font-size: 14px;
}

.store-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.store-info .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #22c55e;
}

.store-info .label {
  color: #6b7280;
}

.store-info .value {
  color: #1f2937;
  font-weight: 500;
}

.divider {
  height: 16px;
  width: 1px;
  background-color: #d1d5db;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  color: #374151;
}

.user-profile .avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  color: #4b5563;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1;
}

.user-meta .username {
  font-weight: bold;
  margin-bottom: 2px;
}

.user-meta .role {
  font-size: 10px;
  color: #9ca3af;
}

/* App Main */
.app-main {
  flex: 1;
  padding: 32px;
  background-color: #f8fafc;
  overflow-y: auto;
  overflow-x: hidden;
  min-width: 0;
}

/* Footer */
.app-footer {
  height: 36px;
  background-color: white;
  border-top: 1px solid #e5e7eb;
  color: #d1d5db;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 32px;
  font-size: 11px;
  flex-shrink: 0;
}

/* Transitions */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.2s;
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateY(5px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}
</style>
