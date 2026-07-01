<template>
  <PageContainer title="首页" description="欢迎使用授权店维修售后库存管理系统">
    <!-- 欢迎信息 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-content">
        <div class="welcome-icon-wrapper">
          <el-icon><Shop /></el-icon>
        </div>
        <div class="welcome-text">
          <div class="welcome-title">
            {{ greeting }}，{{ authStore.user?.realName || authStore.user?.username || '管理员' }}
          </div>
          <div class="welcome-sub">当前角色：{{ roleDisplayName }}　｜　当前门店：{{ authStore.user?.storeName || '默认门店' }}</div>
        </div>
      </div>
    </el-card>

    <!-- 加载中 / 错误状态 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
    <div v-else-if="loadError" class="error-state">
      <el-empty description="数据加载失败，请刷新重试" />
    </div>

    <!-- 统计卡片 -->
    <template v-if="summary">
      <el-row :gutter="16" class="stats-row">
        <el-col :xs="12" :sm="8" :md="6" v-for="card in statCards" :key="card.label">
          <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value" :class="card.colorClass">{{ card.value }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 本月财务概览 -->
      <el-card shadow="never" class="section-card">
        <template #header>
          <span class="section-header">本月财务概览</span>
        </template>
        <el-row :gutter="16">
          <el-col :span="6">
            <div class="finance-item">
              <div class="finance-label">客户支付净收入</div>
              <MoneyText :amount="summary.monthCustomerIncome" bold type="success" />
            </div>
          </el-col>
          <el-col :span="6">
            <div class="finance-item">
              <div class="finance-label">官方结算收入</div>
              <MoneyText :amount="summary.monthOfficialIncome" bold type="primary" />
            </div>
          </el-col>
          <el-col :span="6">
            <div class="finance-item">
              <div class="finance-label">配件成本</div>
              <MoneyText :amount="summary.monthPartsCost" bold type="warning" />
            </div>
          </el-col>
          <el-col :span="6">
            <div class="finance-item">
              <div class="finance-label">报销成本</div>
              <MoneyText :amount="summary.monthReimbursementCost" bold type="warning" />
            </div>
          </el-col>
        </el-row>
        <el-divider />
        <div class="profit-row">
          <span class="profit-label">本月利润</span>
          <MoneyText :amount="summary.monthProfit" bold :type="summary.monthProfit >= 0 ? 'success' : 'danger'" class="profit-value" />
        </div>
      </el-card>

      <!-- 待处理事项 -->
      <el-card shadow="never" class="section-card" v-if="hasPendingActions">
        <template #header>
          <span class="section-header">待处理事项</span>
        </template>
        <el-row :gutter="16">
          <el-col :span="8" v-if="summary.pendingActions.pendingSettleCount > 0">
            <el-alert
              :title="`${summary.pendingActions.pendingSettleCount} 个工单待交付关闭`"
              type="warning"
              :closable="false"
              show-icon
            />
          </el-col>
          <el-col :span="8" v-if="summary.pendingActions.pendingReimbursementCount > 0">
            <el-alert
              :title="`${summary.pendingActions.pendingReimbursementCount} 条报销待确认`"
              type="info"
              :closable="false"
              show-icon
            />
          </el-col>
          <el-col :span="8" v-if="summary.pendingActions.pendingOfficialSettlementCount > 0">
            <el-alert
              :title="`${summary.pendingActions.pendingOfficialSettlementCount} 笔官方结算待处理`"
              type="info"
              :closable="false"
              show-icon
            />
          </el-col>
        </el-row>
      </el-card>

      <!-- 最近工单 -->
      <el-card shadow="never" class="section-card" v-if="summary.recentWorkOrders.length > 0">
        <template #header>
          <div class="section-header-row">
            <span class="section-header">最近工单</span>
            <el-button text type="primary" @click="router.push('/work-order')">查看全部</el-button>
          </div>
        </template>
        <el-table :data="summary.recentWorkOrders" stripe size="small">
          <el-table-column prop="workOrderNo" label="工单号" width="160" />
          <el-table-column prop="customerName" label="客户名" width="120">
            <template #default="{ row }">{{ row.customerName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <StatusTag :status="row.status" :label="getProgressStatusText(row.status)" />
            </template>
          </el-table-column>
          <el-table-column prop="receivableAmount" label="应收" width="120" align="right">
            <template #default="{ row }">
              <MoneyText :amount="row.receivableAmount" />
            </template>
          </el-table-column>
          <el-table-column prop="receivedAmount" label="实收" width="120" align="right">
            <template #default="{ row }">
              <MoneyText :amount="row.receivedAmount" />
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" min-width="160" />
        </el-table>
      </el-card>

      <!-- 快捷入口 -->
      <div class="shortcut-section">
        <div class="section-title">常用功能</div>
        <div class="shortcut-grid">
          <div
            v-for="item in shortcuts"
            :key="item.path"
            class="shortcut-item"
            @click="router.push(item.path)"
          >
            <div class="shortcut-icon-wrapper">
              <el-icon><component :is="item.icon" /></el-icon>
            </div>
            <div class="shortcut-label">{{ item.label }}</div>
          </div>
        </div>
      </div>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Box, DataAnalysis, Loading, Money, OfficeBuilding, Operation, Shop, Tools } from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth';
import { hasPermission, hasAnyPermission } from '@/utils/permission';
import { getDashboardSummary } from '@/api/dashboard';
import PageContainer from '@/components/PageContainer.vue';
import MoneyText from '@/components/MoneyText.vue';
import StatusTag from '@/components/StatusTag.vue';
import { getProgressStatusText } from '@/utils/statusText';
import type { DashboardSummaryResponse } from '@/types/dashboard';

const router = useRouter();
const authStore = useAuthStore();

const loading = ref(true);
const loadError = ref(false);
const summary = ref<DashboardSummaryResponse | null>(null);

const ROLE_LABEL_MAP: Record<string, string> = {
  SUPER_ADMIN: '系统超管',
  STORE_ADMIN: '门店管理员',
  FINANCE: '财务',
  TECHNICIAN_FRONT_DESK: '前台员工',
  INVENTORY_CLERK: '库存员',
  CASHIER: '收银员',
  READONLY_STAFF: '只读员工',
};

const roleDisplayName = computed(() => {
  const codes = authStore.user?.roleCodes;
  if (!codes || codes.length === 0) return '当前账号';
  return ROLE_LABEL_MAP[codes[0]] || codes[0];
});

const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 12) return '早上好';
  if (hour < 18) return '下午好';
  return '晚上好';
});

const hasPendingActions = computed(() => {
  if (!summary.value?.pendingActions) return false;
  const a = summary.value.pendingActions;
  return a.pendingSettleCount > 0 || a.pendingReimbursementCount > 0 || a.pendingOfficialSettlementCount > 0;
});

const statCards = computed(() => {
  if (!summary.value) return [];
  return [
    { label: '今日工单', value: summary.value.todayWorkOrderCount, colorClass: 'text-primary' },
    { label: '待交付关闭工单', value: summary.value.pendingSettleWorkOrderCount, colorClass: 'text-warning' },
    { label: '待确认报销', value: summary.value.pendingReimbursementCount, colorClass: 'text-info' },
    { label: '低库存配件', value: summary.value.lowStockPartCount, colorClass: summary.value.lowStockPartCount > 0 ? 'text-danger' : 'text-success' },
  ];
});

const shortcuts = computed(() => {
  const all = [
    { path: '/work-order', icon: Tools, label: '工单管理', always: true },
    { path: '/parts', icon: Box, label: '配件管理', always: true },
    {
      path: '/inventory',
      icon: Operation,
      label: '库存管理',
      always: false,
      check: () => hasAnyPermission(['INVENTORY_VIEW', 'INVENTORY_INBOUND', 'INVENTORY_ADJUST']),
    },
    {
      path: '/settlement',
      icon: OfficeBuilding,
      label: '官方结算',
      always: false,
      check: () => hasAnyPermission(['OFFICIAL_SETTLEMENT_MANAGE', 'FINANCE_VIEW']),
    },
    {
      path: '/reimbursement',
      icon: Money,
      label: '报销台账',
      always: false,
      check: () => hasAnyPermission(['REIMBURSEMENT_CONFIRM', 'FINANCE_VIEW']),
    },
    {
      path: '/finance',
      icon: DataAnalysis,
      label: '财务报表',
      always: false,
      check: () => hasPermission('FINANCE_VIEW'),
    },
  ];
  return all.filter(item => item.always || (item.check && item.check()));
});

onMounted(async () => {
  try {
    summary.value = await getDashboardSummary();
  } catch {
    loadError.value = true;
    // request interceptor already shows error
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.welcome-card {
  margin-bottom: 20px;
}

.welcome-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.welcome-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: rgba(22, 119, 255, 0.08);
  font-size: 24px;
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

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 60px 0;
  color: #909399;
}

.error-state {
  padding: 40px 0;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  margin-bottom: 16px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
}

.text-primary { color: #409eff; }
.text-success { color: #67c23a; }
.text-warning { color: #e6a23c; }
.text-danger { color: #f56c6c; }
.text-info { color: #909399; }

.section-card {
  margin-bottom: 20px;
}

.section-header {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.finance-item {
  text-align: center;
}

.finance-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.profit-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 8px;
}

.profit-label {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.profit-value {
  font-size: 20px;
}

.shortcut-section {
  margin-top: 8px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.shortcut-item:hover {
  border-color: #1677ff;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.05);
  transform: translateY(-1px);
}

.shortcut-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background-color: rgba(22, 119, 255, 0.08);
  font-size: 18px;
  flex-shrink: 0;
}

.shortcut-label {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}
</style>
