<template>
  <PageContainer title="收银日报" description="查看指定日期的收款、退款与方式分组统计">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" class="search-form" size="default">
        <el-form-item label="日期">
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            :clearable="false"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="fetchReport">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="report" shadow="never" style="margin-bottom: 20px;">
      <el-descriptions :column="4" border size="default">
        <el-descriptions-item label="日期">{{ report.date }}</el-descriptions-item>
        <el-descriptions-item label="收款笔数">{{ report.paymentCount }}</el-descriptions-item>
        <el-descriptions-item label="退款笔数">{{ report.refundCount }}</el-descriptions-item>
        <el-descriptions-item label="未结工单数">{{ report.currentUnpaidWorkOrderCount }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions :column="3" border size="default" style="margin-top: 16px;">
        <el-descriptions-item label="收款总额" align="center">
          <MoneyText :amount="report.totalPaymentAmount" bold type="success" />
        </el-descriptions-item>
        <el-descriptions-item label="退款总额" align="center">
          <MoneyText :amount="report.totalRefundAmount" bold type="danger" />
        </el-descriptions-item>
        <el-descriptions-item label="净收款" align="center">
          <MoneyText :amount="report.netAmount" bold :type="report.netAmount >= 0 ? 'primary' : 'danger'" />
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="report && report.byMethod.length > 0" shadow="never">
      <template #header>
        <span style="font-weight: 600;">按支付方式统计</span>
      </template>
      <el-table :data="report.byMethod" border size="default">
        <el-table-column label="支付方式" width="120">
          <template #default="{ row }">{{ getMethodLabel(row.method) }}</template>
        </el-table-column>
        <el-table-column label="收款金额" align="right" width="120">
          <template #default="{ row }"><MoneyText :amount="row.paymentAmount" type="success" /></template>
        </el-table-column>
        <el-table-column label="退款金额" align="right" width="120">
          <template #default="{ row }"><MoneyText :amount="row.refundAmount" type="danger" /></template>
        </el-table-column>
        <el-table-column label="净额" align="right" width="120">
          <template #default="{ row }"><MoneyText :amount="row.netAmount" :type="row.netAmount >= 0 ? 'primary' : 'danger'" /></template>
        </el-table-column>
        <el-table-column label="收款笔数" prop="paymentCount" align="center" width="100" />
        <el-table-column label="退款笔数" prop="refundCount" align="center" width="100" />
      </el-table>
    </el-card>

    <el-empty v-if="!loading && !report" description="请选择日期后点击查询" />
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import PageContainer from '@/components/PageContainer.vue';
import MoneyText from '@/components/MoneyText.vue';
import { getCashierReport } from '@/api/finance';
import type { CashierReportResponse } from '@/types/finance';

const selectedDate = ref(new Date().toISOString().slice(0, 10));
const loading = ref(false);
const report = ref<CashierReportResponse | null>(null);

const getMethodLabel = (method: string) => {
  const map: Record<string, string> = {
    WECHAT: '微信',
    ALIPAY: '支付宝',
    UNIONPAY: '银联',
    CASH: '现金',
    OTHER: '其他',
  };
  return map[method] || method;
};

const fetchReport = async () => {
  if (!selectedDate.value) return;
  loading.value = true;
  report.value = null;
  try {
    report.value = await getCashierReport(selectedDate.value);
  } catch {
    // interceptor shows error
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchReport();
});
</script>

<style scoped>
.search-card {
  margin-bottom: 20px;
}
</style>
