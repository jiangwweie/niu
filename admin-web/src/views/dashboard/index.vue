<template>
  <PageContainer title="首页" description="欢迎使用小牛维修售后库存管理系统">
    <!-- 欢迎信息 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-content">
        <div class="welcome-icon">👋</div>
        <div class="welcome-text">
          <div class="welcome-title">
            {{ greeting }}，{{ authStore.user?.realName || authStore.user?.username || '管理员' }}
          </div>
          <div class="welcome-sub">当前角色：{{ roleDisplayName }}　｜　统计模块待接入真实接口，请通过左侧菜单查看真实业务数据。</div>
        </div>
      </div>
    </el-card>

    <!-- 快捷入口 -->
    <div class="shortcut-section">
      <div class="section-title">常用功能</div>
      <el-row :gutter="16">
        <el-col
          v-for="item in shortcuts"
          :key="item.path"
          :xs="12" :sm="8" :md="6" :lg="4"
          style="margin-bottom: 16px;"
        >
          <el-card
            shadow="hover"
            class="shortcut-card"
            @click="router.push(item.path)"
          >
            <div class="shortcut-icon">{{ item.icon }}</div>
            <div class="shortcut-label">{{ item.label }}</div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { hasPermission, hasAnyPermission } from '@/utils/permission';
import PageContainer from '@/components/PageContainer.vue';

const router = useRouter();
const authStore = useAuthStore();

const ROLE_LABEL_MAP: Record<string, string> = {
  SUPER_ADMIN: '系统超管',
  STORE_ADMIN: '门店管理员',
  FINANCE: '财务',
  TECHNICIAN_FRONT_DESK: '前台员工',
};

const roleDisplayName = computed(() => {
  const codes = authStore.user?.roleCodes;
  if (!codes || codes.length === 0) return '当前用户';
  return ROLE_LABEL_MAP[codes[0]] || codes[0];
});

const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 12) return '早上好';
  if (hour < 18) return '下午好';
  return '晚上好';
});

// Shortcut entries, only show what user has permission to see
const shortcuts = computed(() => {
  const all = [
    { path: '/work-order', icon: '🔧', label: '工单管理', always: true },
    { path: '/parts', icon: '📦', label: '配件管理', always: true },
    {
      path: '/inventory',
      icon: '🗄️',
      label: '库存管理',
      always: false,
      check: () => hasAnyPermission(['INVENTORY_VIEW', 'INVENTORY_INBOUND', 'INVENTORY_ADJUST']),
    },
    {
      path: '/settlement',
      icon: '🏢',
      label: '官方售后结算',
      always: false,
      check: () => hasAnyPermission(['OFFICIAL_SETTLEMENT_MANAGE', 'FINANCE_VIEW']),
    },
    {
      path: '/reimbursement',
      icon: '💰',
      label: '报销台账',
      always: false,
      check: () => hasAnyPermission(['REIMBURSEMENT_CONFIRM', 'FINANCE_VIEW']),
    },
    {
      path: '/finance',
      icon: '📊',
      label: '财务报表',
      always: false,
      check: () => hasPermission('FINANCE_VIEW'),
    },
  ];

  return all.filter(item => item.always || (item.check && item.check()));
});
</script>

<style scoped>
.welcome-card {
  margin-bottom: 28px;
}

.welcome-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.welcome-icon {
  font-size: 36px;
  flex-shrink: 0;
}

.welcome-title {
  font-size: 18px;
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 6px;
}

.welcome-sub {
  font-size: 13px;
  color: #6b7280;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16px;
}

.shortcut-card {
  cursor: pointer;
  text-align: center;
  padding: 8px 0;
  transition: transform 0.15s;
}

.shortcut-card:hover {
  transform: translateY(-2px);
}

.shortcut-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.shortcut-label {
  font-size: 13px;
  color: #374151;
  font-weight: 500;
}
</style>
