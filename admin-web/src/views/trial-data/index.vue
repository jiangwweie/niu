<template>
  <PageContainer title="正式启用" description="清理试运行数据，正式启用系统">
    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <template v-else>
      <!-- 风险提示 -->
      <el-alert
        type="error"
        :closable="false"
        show-icon
        class="risk-alert"
      >
        <template #title>
          <span style="font-weight: bold;">高风险操作 - 请谨慎操作</span>
        </template>
        <template #default>
          <div class="risk-content">
            <p>此操作将<strong>永久删除</strong>所有试运行业务数据，包括工单、支付、退款、报销、官方结算、库存流水等。</p>
            <p>清理后<strong>不可恢复</strong>，除非有数据库备份。</p>
            <p><strong>强烈建议在操作前备份数据库！</strong></p>
          </div>
        </template>
      </el-alert>

      <!-- 可清理数据摘要 -->
      <el-card shadow="never" class="section-card" v-if="summary">
        <template #header>
          <span class="section-header">当前可清理数据</span>
        </template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="工单">{{ summary.workOrderCount }}</el-descriptions-item>
          <el-descriptions-item label="收费项目">{{ summary.workOrderChargeItemCount }}</el-descriptions-item>
          <el-descriptions-item label="工单状态日志">{{ summary.workOrderStatusLogCount }}</el-descriptions-item>
          <el-descriptions-item label="支付记录">{{ summary.paymentRecordCount }}</el-descriptions-item>
          <el-descriptions-item label="退款记录">{{ summary.refundRecordCount }}</el-descriptions-item>
          <el-descriptions-item label="官方售后记录">{{ summary.officialAfterSalesCount }}</el-descriptions-item>
          <el-descriptions-item label="报销记录">{{ summary.reimbursementCount }}</el-descriptions-item>
          <el-descriptions-item label="库存流水">{{ summary.inventoryFlowCount }}</el-descriptions-item>
          <el-descriptions-item label="库存记录（将归零）">{{ summary.inventoryStockCount }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 保留说明 -->
      <el-card shadow="never" class="section-card">
        <template #header>
          <span class="section-header">清理后保留内容</span>
        </template>
        <ul class="retain-list">
          <li>用户账号、角色、权限</li>
          <li>门店配置</li>
          <li>配件基础资料（库存数量将归零）</li>
          <li>字典配置</li>
        </ul>
      </el-card>

      <!-- 执行清理 -->
      <el-card shadow="never" class="section-card">
        <template #header>
          <span class="section-header">执行清理</span>
        </template>
        <el-form label-width="120px">
          <el-form-item label="确认文本">
            <el-input
              v-model="confirmInput"
              placeholder="请输入 CONFIRM_CLEAR_TRIAL_DATA"
              :disabled="clearing"
            />
            <div class="form-tip">请输入 <code>CONFIRM_CLEAR_TRIAL_DATA</code> 以确认执行清理</div>
          </el-form-item>
          <el-form-item>
            <el-button
              type="danger"
              :disabled="confirmInput !== 'CONFIRM_CLEAR_TRIAL_DATA' || clearing"
              :loading="clearing"
              @click="handleClear"
            >
              执行清理
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 清理结果 -->
      <el-card shadow="never" class="section-card" v-if="clearResult">
        <template #header>
          <span class="section-header">清理结果</span>
        </template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="工单已删除">{{ clearResult.workOrdersDeleted }}</el-descriptions-item>
          <el-descriptions-item label="收费项目已删除">{{ clearResult.chargeItemsDeleted }}</el-descriptions-item>
          <el-descriptions-item label="状态日志已删除">{{ clearResult.statusLogsDeleted }}</el-descriptions-item>
          <el-descriptions-item label="支付记录已删除">{{ clearResult.paymentsDeleted }}</el-descriptions-item>
          <el-descriptions-item label="退款记录已删除">{{ clearResult.refundsDeleted }}</el-descriptions-item>
          <el-descriptions-item label="官方售后已删除">{{ clearResult.officialAfterSalesDeleted }}</el-descriptions-item>
          <el-descriptions-item label="报销记录已删除">{{ clearResult.reimbursementsDeleted }}</el-descriptions-item>
          <el-descriptions-item label="库存流水已删除">{{ clearResult.inventoryFlowsDeleted }}</el-descriptions-item>
          <el-descriptions-item label="库存已归零">{{ clearResult.inventoryStocksReset }}</el-descriptions-item>
        </el-descriptions>
        <el-alert type="success" :closable="false" show-icon style="margin-top: 16px;">
          清理完成。系统已可以正式启用。
        </el-alert>
      </el-card>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Loading } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getTrialDataSummary, clearTrialData } from '@/api/trial-data';
import PageContainer from '@/components/PageContainer.vue';
import type { TrialDataSummaryResponse, ClearTrialDataResponse } from '@/types/trial-data';

const loading = ref(true);
const clearing = ref(false);
const summary = ref<TrialDataSummaryResponse | null>(null);
const clearResult = ref<ClearTrialDataResponse | null>(null);
const confirmInput = ref('');

async function loadSummary() {
  loading.value = true;
  try {
    summary.value = await getTrialDataSummary();
  } catch {
    ElMessage.error('加载数据摘要失败');
  } finally {
    loading.value = false;
  }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm(
      '此操作将永久删除所有试运行业务数据且不可恢复。强烈建议先备份数据库。确认继续？',
      '最终确认',
      { confirmButtonText: '确认清理', cancelButtonText: '取消', type: 'error' }
    );
  } catch {
    return; // user cancelled
  }

  clearing.value = true;
  try {
    clearResult.value = await clearTrialData(confirmInput.value);
    ElMessage.success('试运行数据清理完成');
    confirmInput.value = '';
    await loadSummary();
  } catch {
    ElMessage.error('清理失败');
  } finally {
    clearing.value = false;
  }
}

onMounted(loadSummary);
</script>

<style scoped>
.risk-alert {
  margin-bottom: 20px;
}

.risk-content p {
  margin: 4px 0;
  font-size: 13px;
}

.section-card {
  margin-bottom: 20px;
}

.section-header {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 60px 0;
  color: #909399;
}

.retain-list {
  margin: 0;
  padding-left: 20px;
  color: #606266;
}

.retain-list li {
  margin-bottom: 4px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.form-tip code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  color: #f56c6c;
  font-weight: bold;
}
</style>
