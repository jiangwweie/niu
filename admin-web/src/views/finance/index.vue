<template>
  <PageContainer title="财务报表" description="查看客户支付收入、官方结算收入、成本与利润报表">
    <el-alert
      title="财务口径：净利润 = 总收入 - 总成本；总收入 = 客户实收收入 + 官方结算收入；总成本 = 配件成本 + 已确认报销成本；客户实收收入 = 收款金额 - 退款金额。金额保留两位小数。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />

    <!-- 查询条件区 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="queryParams" class="search-form" size="default">
        <el-form-item label="报表类型">
          <el-radio-group v-model="queryParams.reportType" @change="handleSearch">
            <el-radio-button value="DAILY">日报</el-radio-button>
            <el-radio-button value="MONTHLY">月报</el-radio-button>
            <el-radio-button value="CUSTOM">自定义范围</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="选择日期" v-if="queryParams.reportType === 'DAILY'">
          <el-date-picker
            v-model="queryParams.date"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :clearable="false"
            @change="handleSearch"
          />
        </el-form-item>

        <el-form-item label="选择月份" v-if="queryParams.reportType === 'MONTHLY'">
          <div style="display: flex; gap: 10px;">
            <el-date-picker
              v-model="monthPickerValue"
              type="month"
              placeholder="选择月份"
              format="YYYY-MM"
              value-format="YYYY-MM"
              :clearable="false"
              @change="handleMonthChange"
            />
          </div>
        </el-form-item>

        <el-form-item label="日期范围" v-if="queryParams.reportType === 'CUSTOM'">
          <el-date-picker
            v-model="queryParams.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :clearable="false"
          />
        </el-form-item>

        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button v-if="hasPermission('EXCEL_EXPORT')" type="success" @click="handleExport" :loading="exportLoading">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 核心指标 / 拆分区 -->
    <div class="summary-cards">
      <!-- 收入卡片 -->
      <el-card shadow="never" class="summary-card" v-loading="loading">
        <template #header>
          <div class="card-header">
            <span>总收入</span>
            <span class="total-text"><MoneyText :amount="summaryData?.totalIncome || 0" type="primary" /></span>
          </div>
        </template>
        <div class="split-section">
          <div class="split-group">
            <div class="group-item">
              <span class="item-label">客户实收收入</span>
              <MoneyText :amount="summaryData?.customerIncome || 0" />
            </div>
            <div class="group-desc text-info">收款金额 - 退款金额（仅统计本期）</div>
          </div>
          <el-divider />
          <div class="split-group">
            <div class="group-item">
              <span class="item-label">官方结算收入</span>
              <MoneyText :amount="summaryData?.officialIncome || 0" />
            </div>
            <div class="group-desc text-info">状态为 SETTLED 的官方结算</div>
          </div>
        </div>
      </el-card>

      <!-- 成本卡片 -->
      <el-card shadow="never" class="summary-card" v-loading="loading">
        <template #header>
          <div class="card-header">
            <span>总成本</span>
            <span class="total-text"><MoneyText :amount="summaryData?.totalCost || 0" type="danger" /></span>
          </div>
        </template>
        <div class="split-section">
          <div class="split-group">
            <div class="group-item">
              <span class="item-label">配件成本</span>
              <MoneyText :amount="summaryData?.partsCost || 0" />
            </div>
            <div class="group-desc text-info">SETTLED 工单的配件成本 (line_cost_amount)</div>
          </div>
          <el-divider />
          <div class="split-group">
            <div class="group-item">
              <span class="item-label">已确认报销成本</span>
              <MoneyText :amount="summaryData?.reimbursementCost || 0" />
            </div>
            <div class="group-desc text-info">状态为 CONFIRMED 的报销</div>
          </div>
        </div>
      </el-card>

      <!-- 利润与单据量卡片 -->
      <el-card shadow="never" class="summary-card" v-loading="loading">
        <template #header>
          <div class="card-header">
            <span>净利润</span>
            <span class="total-text"><MoneyText :amount="summaryData?.profit || 0" :type="(summaryData?.profit || 0) >= 0 ? 'success' : 'danger'" /></span>
          </div>
        </template>
        <div class="split-section profit-section">
          <div class="profit-row">
            <span class="item-label">已结算工单数</span>
            <span class="font-bold">{{ summaryData?.settledWorkOrderCount || 0 }}</span>
          </div>
          <el-divider style="margin: 12px 0;" />
          <div class="profit-row">
            <span class="item-label">已确认报销数</span>
            <span class="font-bold">{{ summaryData?.confirmedReimbursementCount || 0 }}</span>
          </div>
        </div>
        <div class="period-info text-info" style="margin-top: 15px; font-size: 12px; text-align: right;">
          统计周期: {{ summaryData?.periodStart || '-' }} 至 {{ summaryData?.periodEnd || '-' }}
        </div>
      </el-card>
    </div>

    <!-- 口径说明区 -->
    <el-card shadow="never" class="notice-card" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>口径说明</span>
        </div>
      </template>
      <div class="notice-content text-info text-sm">
        <ol class="list-decimal pl-4">
          <li><strong>总收入</strong> = 客户实收收入 + 官方结算收入</li>
          <li><strong>总成本</strong> = 配件成本 + 已确认报销成本</li>
          <li><strong>净利润</strong> = 总收入 - 总成本</li>
          <li><strong>客户实收收入</strong> = 收款金额 - 退款金额</li>
          <li>所有金额均保留两位小数，无数据或无流水时展示为 ¥0.00。</li>
        </ol>
      </div>
    </el-card>

  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import PageContainer from '@/components/PageContainer.vue';
import MoneyText from '@/components/MoneyText.vue';
import { getDailyFinance, getMonthlyFinance, getRangeFinance } from '@/api/finance';
import { exportFinance } from '@/api/export';
import { hasPermission } from '@/utils/permission';
import type { FinanceQuery, FinanceReportResponse } from '@/types/finance';
import dayjs from 'dayjs';

const today = dayjs().format('YYYY-MM-DD');
const currentMonthStr = dayjs().format('YYYY-MM');

const queryParams = reactive<FinanceQuery>({
  reportType: 'DAILY',
  date: today,
});

const monthPickerValue = ref(currentMonthStr);

const loading = ref(false);
const exportLoading = ref(false);
const summaryData = ref<FinanceReportResponse | null>(null);

const handleMonthChange = (val: string) => {
  if (val) {
    const parts = val.split('-');
    queryParams.year = parseInt(parts[0], 10);
    queryParams.month = parseInt(parts[1], 10);
  }
  handleSearch();
};

const fetchData = async () => {
  loading.value = true;
  try {
    let res: FinanceReportResponse | null = null;
    
    if (queryParams.reportType === 'DAILY') {
      res = await getDailyFinance({ date: queryParams.date });
    } else if (queryParams.reportType === 'MONTHLY') {
      const y = queryParams.year || dayjs().year();
      const m = queryParams.month || (dayjs().month() + 1);
      res = await getMonthlyFinance({ year: y, month: m });
    } else if (queryParams.reportType === 'CUSTOM') {
      if (!queryParams.dateRange || queryParams.dateRange.length !== 2) {
        ElMessage.warning('请选择日期范围');
        loading.value = false;
        return;
      }
      res = await getRangeFinance({ startDate: queryParams.dateRange[0], endDate: queryParams.dateRange[1] });
    }

    if (res) {
      summaryData.value = res;
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  // auto setup defaults
  if (queryParams.reportType === 'DAILY' && !queryParams.date) {
    queryParams.date = today;
  }
  if (queryParams.reportType === 'MONTHLY' && !queryParams.year) {
    queryParams.year = dayjs().year();
    queryParams.month = dayjs().month() + 1;
    monthPickerValue.value = `${queryParams.year}-${String(queryParams.month).padStart(2, '0')}`;
  }
  if (queryParams.reportType === 'CUSTOM' && (!queryParams.dateRange)) {
    queryParams.dateRange = [
      dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
      today
    ];
  }
  fetchData();
};

const handleExport = async () => {
  exportLoading.value = true;
  try {
    const params: FinanceQuery = { ...queryParams };
    // format month for the api if needed
    if (params.reportType === 'MONTHLY' && !params.year) {
      params.year = dayjs().year();
      params.month = dayjs().month() + 1;
    }
    await exportFinance(params);
    ElMessage.success('导出成功');
  } catch (error: any) {
    ElMessage.error(error.message || '导出失败');
  } finally {
    exportLoading.value = false;
  }
};

onMounted(() => {
  handleSearch();
});
</script>

<style scoped>
.search-card {
  margin-bottom: 20px;
}
.search-actions {
  margin-left: auto;
}
.summary-cards {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}
.summary-card {
  flex: 1;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}
.total-text {
  font-size: 18px;
}
.split-section {
  padding: 8px 0;
}
.split-group {
  margin-bottom: 8px;
}
.group-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}
.item-label {
  color: #606266;
}
.group-desc {
  font-size: 12px;
  margin-top: 8px;
}
.text-info {
  color: var(--el-color-info);
}
.text-danger {
  color: var(--el-color-danger);
}
.text-success {
  color: var(--el-color-success);
}
.font-bold {
  font-weight: bold;
}
.profit-section {
  display: flex;
  flex-direction: column;
}
.profit-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 15px;
}
.notice-card {
  margin-top: 20px;
}
.list-decimal {
  list-style-type: decimal;
}
.pl-4 {
  padding-left: 1rem;
}
.text-sm {
  font-size: 14px;
}
</style>
