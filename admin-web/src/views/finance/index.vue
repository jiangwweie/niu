<template>
  <PageContainer title="财务报表" description="查看客户支付收入、官方结算收入、成本与利润 mock 报表">
    <el-alert
      title="当前财务报表为 mock 展示，真实统计 API 尚未实现。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 10px;"
    />
    <el-alert
      title="财务报表应由后端从工单明细、支付记录、退款记录、官方结算记录、库存成本和已确认报销中汇总生成。当前页面仅展示 mock 数据，不执行真实财务计算。"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 10px;"
    />
    <el-alert
      title="客户支付收入和官方结算收入必须分开统计，不能互相替代。"
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
            <el-radio-button value="CUSTOM">自定义</el-radio-button>
          </el-radio-group>
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
          />
        </el-form-item>
        <el-form-item class="shortcuts" v-if="queryParams.reportType !== 'CUSTOM'">
          快捷选择：
          <el-button link type="primary" disabled title="后续接入">今日</el-button>
          <el-button link type="primary" disabled title="后续接入">本月</el-button>
          <el-button link type="primary" disabled title="后续接入">近 7 天</el-button>
          <el-button link type="primary" disabled title="后续接入">近 30 天</el-button>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" disabled title="后续接入">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 核心指标 / 拆分区 -->
    <div class="summary-cards">
      <!-- 收入卡片 -->
      <el-card shadow="never" class="summary-card" v-loading="summaryLoading">
        <template #header>
          <div class="card-header">
            <span>收入</span>
            <span class="total-text"><MoneyText :amount="summaryData?.totalIncome || 0" /></span>
          </div>
        </template>
        <div class="split-section">
          <div class="split-group">
            <div class="group-title">客户支付收入 ( <MoneyText :amount="summaryData?.customerPaidIncome || 0" type="primary" bold /> )</div>
            <div class="group-item">
              <span class="item-label">配件收入</span>
              <MoneyText :amount="summaryData?.partsIncome || 0" />
            </div>
            <div class="group-item">
              <span class="item-label">人工费收入</span>
              <MoneyText :amount="summaryData?.laborIncome || 0" />
            </div>
            <div class="group-item">
              <span class="item-label">其他收入</span>
              <MoneyText :amount="summaryData?.otherIncome || 0" />
            </div>
            <div class="group-desc text-info">客户支付净收入 = 客户支付来自 payment_record / refund_record 明细，减去退款总额</div>
          </div>
          <el-divider />
          <div class="split-group">
            <div class="group-title">官方结算收入 ( <MoneyText :amount="summaryData?.officialSettlementIncome || 0" type="success" bold /> )</div>
            <div class="group-item">
              <span class="item-label">官方结算收入</span>
              <MoneyText :amount="summaryData?.officialSettlementIncome || 0" />
            </div>
            <div class="group-desc text-info">官方结算收入来自管理端手动录入的官方售后结算记录</div>
          </div>
        </div>
      </el-card>

      <!-- 成本卡片 -->
      <el-card shadow="never" class="summary-card" v-loading="summaryLoading">
        <template #header>
          <div class="card-header">
            <span>成本</span>
            <span class="total-text"><MoneyText :amount="summaryData?.totalCost || 0" type="danger" /></span>
          </div>
        </template>
        <div class="split-section">
          <div class="split-group">
            <div class="group-title">配件成本</div>
            <div class="group-item">
              <span class="item-label">配件成本</span>
              <MoneyText :amount="summaryData?.partsCost || 0" />
            </div>
            <div class="group-desc text-info">配件成本来自工单消耗配件的成本记录</div>
          </div>
          <el-divider />
          <div class="split-group">
            <div class="group-title">报销成本</div>
            <div class="group-item">
              <span class="item-label">报销成本</span>
              <MoneyText :amount="summaryData?.reimbursementCost || 0" />
            </div>
            <div class="group-desc text-warning">报销成本仅统计已确认报销。待确认、已驳回、已取消报销不计入成本。</div>
          </div>
        </div>
      </el-card>

      <!-- 利润卡片 -->
      <el-card shadow="never" class="summary-card" v-loading="summaryLoading">
        <template #header>
          <div class="card-header">
            <span>利润</span>
            <span class="total-text"><MoneyText :amount="summaryData?.profit || 0" type="success" /></span>
          </div>
        </template>
        <div class="split-section profit-section">
          <div class="profit-row">
            <span class="item-label">总收入</span>
            <MoneyText :amount="summaryData?.totalIncome || 0" />
          </div>
          <div class="profit-row">
            <span class="item-label">- 总成本</span>
            <MoneyText :amount="summaryData?.totalCost || 0" type="danger" />
          </div>
          <el-divider style="margin: 12px 0;" />
          <div class="profit-row font-bold">
            <span class="item-label">= 利润</span>
            <MoneyText :amount="summaryData?.profit || 0" type="success" />
          </div>
          <div class="profit-row" style="margin-top: 16px;">
            <span class="item-label">利润率</span>
            <span class="text-primary font-bold">{{ summaryData?.profitMargin || '0%' }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 表格区 -->
    <el-card shadow="never" class="table-card" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>明细表 (Mock)</span>
        </div>
      </template>
      <div class="table-wrapper">
<el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%; min-width: 1000px"
        border
      >
        <el-table-column prop="dateOrMonth" label="日期 / 月份" width="120" fixed="left" />
        <el-table-column label="客户支付净收入" width="130" align="right">
          <template #default="{ row }"><MoneyText :amount="row.customerPaidIncome" /></template>
        </el-table-column>
        <el-table-column label="官方结算收入" width="120" align="right">
          <template #default="{ row }"><MoneyText :amount="row.officialSettlementIncome" /></template>
        </el-table-column>
        <el-table-column label="配件收入" width="100" align="right">
          <template #default="{ row }"><MoneyText :amount="row.partsIncome" /></template>
        </el-table-column>
        <el-table-column label="人工费收入" width="100" align="right">
          <template #default="{ row }"><MoneyText :amount="row.laborIncome" /></template>
        </el-table-column>
        <el-table-column label="其他收入" width="100" align="right">
          <template #default="{ row }"><MoneyText :amount="row.otherIncome" /></template>
        </el-table-column>
        <el-table-column label="配件成本" width="100" align="right">
          <template #default="{ row }"><MoneyText :amount="row.partsCost" /></template>
        </el-table-column>
        <el-table-column label="报销成本" width="100" align="right">
          <template #default="{ row }"><MoneyText :amount="row.reimbursementCost" /></template>
        </el-table-column>
        <el-table-column label="利润" width="120" align="right" fixed="right">
          <template #default="{ row }">
            <MoneyText :amount="row.profit" :type="row.profit >= 0 ? 'success' : 'danger'" bold />
          </template>
        </el-table-column>
      </el-table>
</div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageNoSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSearch"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 口径说明区 -->
    <el-card shadow="never" class="notice-card" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>口径说明</span>
        </div>
      </template>
      <div class="notice-content text-info text-sm">
        <ol class="list-decimal pl-4">
          <li><strong>客户支付净收入</strong> 客户支付来自 payment_record / refund_record 明细，净收入 = 支付总额 - 退款总额</li>
          <li><strong>官方结算收入</strong> 来自管理端手动录入的官方售后结算记录</li>
          <li><strong>配件成本</strong> 来自工单消耗配件的成本记录</li>
          <li><strong>报销成本</strong> 只统计已确认报销</li>
          <li><strong>利润</strong> = 收入 - 成本</li>
          <li>当前页面为 mock UI，真实统计以后端汇总结果为准</li>
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
import { getFinanceSummary, getFinanceList } from '@/api/finance';
import type { FinanceQuery, FinanceSummary, FinanceDetailRecord } from '@/types/finance';

const queryParams = reactive<FinanceQuery>({
  reportType: 'DAILY',
  page: 1,
  pageSize: 10
});

const loading = ref(false);
const summaryLoading = ref(false);
const tableData = ref<FinanceDetailRecord[]>([]);
const total = ref(0);
const summaryData = ref<FinanceSummary | null>(null);

const fetchData = async () => {
  loading.value = true;
  summaryLoading.value = true;
  try {
    const [listRes, sumRes] = await Promise.all([
      getFinanceList(queryParams),
      getFinanceSummary()
    ]);
    if (listRes.code === 'SUCCESS') {
      tableData.value = listRes.data.records;
      total.value = listRes.data.total;
    }
    if (sumRes.code === 'SUCCESS') {
      summaryData.value = sumRes.data;
    }
  } catch (error) {
    ElMessage.error('加载失败');
  } finally {
    loading.value = false;
    summaryLoading.value = false;
  }
};

const handleSearch = () => {
  queryParams.pageNo = 1;
  fetchData();
};

const handleReset = () => {
  queryParams.reportType = 'DAILY';
  queryParams.dateRange = undefined;
  handleSearch();
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.search-card {
  margin-bottom: 20px;
}
.search-actions {
  margin-left: auto;
}
.shortcuts {
  margin-right: 20px;
}
.shortcuts .el-button {
  margin-left: 8px;
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
.group-title {
  font-weight: bold;
  margin-bottom: 12px;
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
.text-warning {
  color: var(--el-color-warning);
}
.text-danger {
  color: var(--el-color-danger);
}
.text-success {
  color: var(--el-color-success);
}
.text-primary {
  color: var(--el-color-primary);
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
.table-card {
  margin-top: 20px;
}
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.notice-card {
  margin-top: 20px;
}
.records-decimal {
  list-style-type: decimal;
}
.pl-4 {
  padding-left: 1rem;
}
.text-sm {
  font-size: 14px;
}
.table-wrapper {
  width: 100%;
  overflow-x: auto;
}
</style>
