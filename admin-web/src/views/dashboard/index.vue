<template>
  <PageContainer title="Dashboard 首页" description="门店今日经营概览">
    <el-alert
      title="当前 Dashboard 为 mock 展示，真实统计数据 API 尚未实现。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading"><Loading /></el-icon> 加载中...
    </div>
    
    <div v-else-if="dashboardData" class="dashboard-content">
      
      <!-- Top Stats Cards -->
      <el-row :gutter="20" class="stats-row">
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-header text-gray"><el-icon><Document /></el-icon> 今日工单数</div>
            <div class="stat-value">{{ dashboardData.stats.todayOrders }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-header text-primary"><el-icon><Money /></el-icon> 客户支付收入</div>
            <div class="stat-value"><MoneyText :amount="dashboardData.stats.todayCustomerIncome" bold type="primary" /></div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-header text-success"><el-icon><Finished /></el-icon> 官方结算收入</div>
            <div class="stat-value"><MoneyText :amount="dashboardData.stats.todayOfficialIncome" bold type="success" /></div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-header text-info"><el-icon><List /></el-icon> 待确认报销</div>
            <div class="stat-value">{{ dashboardData.stats.pendingReimbursement }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-header text-warning"><el-icon><Warning /></el-icon> 库存预警数量</div>
            <div class="stat-value text-orange">{{ dashboardData.stats.inventoryWarnings }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-header text-danger"><el-icon><WarningFilled /></el-icon> 未结算工单</div>
            <div class="stat-value text-red">{{ dashboardData.stats.unsettledOrders }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="mid-row">
        <!-- Income Split -->
        <el-col :span="12">
          <el-card shadow="hover" class="split-card">
            <template #header>
              <div class="card-header">
                <span>今日收入拆分</span>
              </div>
            </template>
            <div class="income-split-container">
              <div class="income-section">
                <div class="income-title">客户支付收入 ( <MoneyText :amount="dashboardData.stats.todayCustomerIncome" type="primary" /> )</div>
                <div class="income-details">
                  <div class="income-item">
                    <span>配件收入:</span>
                    <MoneyText :amount="dashboardData.incomeSplit.customerIncome.parts" />
                  </div>
                  <div class="income-item">
                    <span>人工费收入:</span>
                    <MoneyText :amount="dashboardData.incomeSplit.customerIncome.labor" />
                  </div>
                  <div class="income-item">
                    <span>其他收入:</span>
                    <MoneyText :amount="dashboardData.incomeSplit.customerIncome.other" />
                  </div>
                </div>
              </div>
              <el-divider direction="vertical" class="split-divider" />
              <div class="income-section">
                <div class="income-title">官方结算收入 ( <MoneyText :amount="dashboardData.stats.todayOfficialIncome" type="success" /> )</div>
                <div class="income-details">
                  <div class="income-item">
                    <span>官方结算收入:</span>
                    <MoneyText :amount="dashboardData.incomeSplit.officialIncome.amount" />
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- Todos -->
        <el-col :span="12">
          <el-card shadow="hover" class="todo-card">
            <template #header>
              <div class="card-header">
                <span>待办提醒</span>
              </div>
            </template>
            <div class="todo-list">
              <div class="todo-item" v-for="todo in dashboardData.todos" :key="todo.id">
                <span class="todo-content">{{ todo.content }}</span>
                <el-badge :value="todo.count" :type="todo.type === 'inventory' ? 'warning' : 'danger'" />
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="bottom-row">
        <!-- Recent Orders -->
        <el-col :span="12">
          <el-card shadow="hover" class="table-card">
            <template #header>
              <div class="card-header">
                <span>最近工单</span>
              </div>
            </template>
            <el-table :data="dashboardData.recentOrders" style="width: 100%" size="small">
              <el-table-column prop="orderNo" label="工单编号" width="140" />
              <el-table-column prop="customerName" label="客户姓名" width="80" />
              <el-table-column prop="scooterModel" label="车型" width="100" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <StatusTag :status="row.status" :label="getStatusLabel(row.status)" />
                </template>
              </el-table-column>
              <el-table-column label="金额详情">
                <template #default="{ row }">
                  <div class="amount-column">
                    <div>应收: <MoneyText :amount="row.receivableAmount" /></div>
                    <div>实收: <MoneyText :amount="row.actualAmount" type="success" /></div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="其他" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.isOfficial ? 'success' : 'info'">{{ row.isOfficial ? '官方售后' : '普通单' }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <!-- Inventory Warnings -->
        <el-col :span="12">
          <el-card shadow="hover" class="table-card">
            <template #header>
              <div class="card-header">
                <span>库存预警</span>
              </div>
            </template>
            <el-table :data="dashboardData.inventoryWarnings" style="width: 100%" size="small">
              <el-table-column prop="partCode" label="配件编码" width="120" />
              <el-table-column prop="partName" label="配件名称" />
              <el-table-column label="可用库存" width="100">
                <template #default="{ row }">
                  <span class="text-red font-bold">{{ row.availableStock }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="warningThreshold" label="预警阈值" width="100" />
              <el-table-column prop="location" label="库存位置" />
            </el-table>
          </el-card>
        </el-col>
      </el-row>

    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import PageContainer from '@/components/PageContainer.vue';
import StatusTag from '@/components/StatusTag.vue';
import MoneyText from '@/components/MoneyText.vue';
import { getDashboardStats } from '@/api/dashboard';
import type { DashboardData } from '@/types/dashboard';

const loading = ref(true);
const dashboardData = ref<DashboardData | null>(null);

const fetchData = async () => {
  try {
    const res = await getDashboardStats();
    if (res.code === 'SUCCESS') {
      dashboardData.value = res.data;
    }
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    pending: '待处理',
    paid: '已支付',
    completed: '已完成',
    closed: '已关闭'
  };
  return map[status] || status;
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #909399;
  font-size: 14px;
}
.is-loading {
  animation: rotating 2s linear infinite;
  margin-right: 8px;
}
@keyframes rotating {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-row .el-col {
  margin-bottom: 20px;
}
.stat-card {
  height: 100px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.stat-header {
  font-size: 13px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}
.text-gray { color: #909399; }
.text-primary { color: #409eff; }
.text-success { color: #67c23a; }
.text-warning { color: #e6a23c; }
.text-danger { color: #f56c6c; }
.text-info { color: #909399; }
.text-orange { color: #ff9900; }
.text-red { color: #f56c6c; }
.font-bold { font-weight: bold; }

.mid-row .el-col, .bottom-row .el-col {
  margin-bottom: 20px;
}

.card-header {
  font-weight: bold;
  color: #303133;
}

.split-card, .todo-card, .table-card {
  height: 100%;
}

.income-split-container {
  display: flex;
  align-items: flex-start;
}

.income-section {
  flex: 1;
  padding: 0 10px;
}

.split-divider {
  height: auto;
  min-height: 100px;
  margin: 0 16px;
}

.income-title {
  font-weight: bold;
  font-size: 14px;
  margin-bottom: 12px;
  color: #303133;
}

.income-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.income-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
  background-color: #f8f9fa;
  padding: 8px 12px;
  border-radius: 4px;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.todo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 4px;
  border-left: 3px solid #df001f;
}

.todo-content {
  font-size: 14px;
  color: #303133;
}

.amount-column {
  font-size: 12px;
  line-height: 1.5;
}
</style>
