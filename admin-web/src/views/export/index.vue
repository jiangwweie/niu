<template>
  <PageContainer title="数据导出" description="集中进入当前已开放的 Excel 导出功能">
    <el-alert
      title="当前导出按各业务页面的筛选条件生成文件；进入对应页面后设置筛选条件，再点击导出筛选结果。"
      type="info"
      show-icon
      :closable="false"
      class="export-alert"
    />

    <div class="export-grid">
      <el-card shadow="never" class="export-card" v-if="hasPermission('FINANCE_VIEW')">
        <div class="export-card-main">
          <div>
            <h3>财务报表导出</h3>
            <p>导出客户实收、官方结算、配件成本、已确认报销成本和净利润等核心财务指标。</p>
          </div>
          <el-button type="primary" @click="goTo('/finance')">进入财务报表</el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="export-card" v-if="hasAnyPermission(['REIMBURSEMENT_CONFIRM', 'FINANCE_VIEW'])">
        <div class="export-card-main">
          <div>
            <h3>报销台账导出</h3>
            <p>导出报销编号、报销人、申请金额、确认金额、状态和处理时间等台账字段。</p>
          </div>
          <el-button type="primary" @click="goTo('/reimbursement')">进入报销台账</el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="export-card" v-if="hasAnyPermission(['FUNDING_LEDGER_VIEW', 'FUNDING_EXPORT'])">
        <div class="export-card-main">
          <div>
            <h3>资方台账导出</h3>
            <p>导出资方台账号、客户、车型、应收、已收、待收和台账状态等字段。</p>
          </div>
          <el-button type="primary" @click="goTo('/funding')">进入资方台账</el-button>
        </div>
      </el-card>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import PageContainer from '@/components/PageContainer.vue';
import { hasAnyPermission, hasPermission } from '@/utils/permission';

const router = useRouter();

const goTo = (path: string) => {
  router.push(path);
};
</script>

<style scoped>
.export-alert {
  margin-bottom: 20px;
}

.export-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.export-card {
  border-radius: 8px;
}

.export-card-main {
  min-height: 150px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 20px;
}

.export-card-main h3 {
  margin: 0 0 8px;
  font-size: 17px;
  color: #111827;
}

.export-card-main p {
  margin: 0;
  line-height: 1.7;
  color: #4b5563;
  font-size: 14px;
}
</style>
