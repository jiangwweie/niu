<template>
  <PageContainer title="试运行数据清理" description="清理试运行数据，执行 Clean Start 状态模型适配">
    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      <span>预检中...</span>
    </div>

    <template v-else>
      <!-- Clean Start 预检状态 -->
      <el-card shadow="never" class="section-card">
        <template #header>
          <span class="section-header">Clean Start 状态预检</span>
        </template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="是否存在旧状态工单">
            <el-tag :type="hasLegacyWorkOrders ? 'danger' : 'success'" size="small">
              {{ hasLegacyWorkOrders ? '是 (检测到旧试运行业务数据)' : '否 (无旧状态数据)' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="是否可以 clean-start">
            <el-tag :type="isCleanStartReady ? 'success' : 'danger'" size="small">
              {{ isCleanStartReady ? '是 (预检通过)' : '否 (需要数据清理)' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="blockReason" label="阻断原因" :span="2">
            <span style="color: #F56C6C; font-weight: bold;">{{ blockReason }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

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
            <p>此操作将<strong>永久删除</strong>所有试运行业务数据，包括工单、支付、退款、报销、官方结算、库存流水、客户、车辆等。</p>
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
          <el-descriptions-item label="收款记录">{{ summary.paymentRecordCount }}</el-descriptions-item>
          <el-descriptions-item label="退款记录">{{ summary.refundRecordCount }}</el-descriptions-item>
          <el-descriptions-item label="官方售后记录">{{ summary.officialAfterSalesCount }}</el-descriptions-item>
          <el-descriptions-item label="报销记录">{{ summary.reimbursementCount }}</el-descriptions-item>
          <el-descriptions-item label="库存流水">{{ summary.inventoryFlowCount }}</el-descriptions-item>
          <el-descriptions-item label="库存记录数">{{ summary.inventoryStockCount }}</el-descriptions-item>
          <el-descriptions-item label="车辆资料">{{ summary.vehicleCount }}</el-descriptions-item>
          <el-descriptions-item label="客户资料">{{ summary.customerCount }}</el-descriptions-item>
          <el-descriptions-item label="旧状态工单数">
            <span :style="{ color: summary.legacyWorkOrderStatusCount > 0 ? '#F56C6C' : 'inherit', fontWeight: summary.legacyWorkOrderStatusCount > 0 ? 'bold' : 'normal' }">
              {{ summary.legacyWorkOrderStatusCount }}
            </span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 数据清理说明 -->
      <el-card shadow="never" class="section-card">
        <template #header>
          <span class="section-header">数据清理及保留规则说明</span>
        </template>
        <div style="margin-bottom: 8px; font-weight: bold; color: #E6A23C; font-size: 14px;">一、 清理后保留的内容：</div>
        <ul class="retain-list" style="margin-bottom: 16px;">
          <li>系统中的员工账号、分配的角色以及配置好的操作权限</li>
          <li>各家门店的配置与门店基本信息</li>
          <li>配件基础资料库（但库存数量将全部清零归零）</li>
          <li>通用字典与系统参数配置</li>
        </ul>
        
        <div style="margin-bottom: 8px; font-weight: bold; color: #F56C6C; font-size: 14px;">二、 将被彻底清理的业务数据：</div>
        <ul class="retain-list">
          <li>所有历史维修工单（包括草稿、维修中、结算、取消等所有工单）</li>
          <li>客户的所有付款、退款等资金流水记录</li>
          <li>官方售后结算单据和记录</li>
          <li>提交的报销审批申请和相关的报销流程</li>
          <li>录入的全部客户个人档案和车辆登记信息</li>
          <li>配件的每一次出库、入库、调整等全部库存变动流水记录</li>
          <li><b>注意：</b>清理后所有的配件实时可用库存及实际库存都将<b>归零</b>，后续门店需通过<b>正式库存盘点记录</b>来重新录入期初的配件库存数量。</li>
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
              style="max-width: 360px;"
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
          <el-descriptions-item label="收款记录已删除">{{ clearResult.paymentsDeleted }}</el-descriptions-item>
          <el-descriptions-item label="退款记录已删除">{{ clearResult.refundsDeleted }}</el-descriptions-item>
          <el-descriptions-item label="官方售后已删除">{{ clearResult.officialAfterSalesDeleted }}</el-descriptions-item>
          <el-descriptions-item label="报销记录已删除">{{ clearResult.reimbursementsDeleted }}</el-descriptions-item>
          <el-descriptions-item label="库存流水已删除">{{ clearResult.inventoryFlowsDeleted }}</el-descriptions-item>
          <el-descriptions-item label="库存已归零">{{ clearResult.inventoryStocksReset }}</el-descriptions-item>
          <el-descriptions-item label="车辆资料已删除">{{ clearResult.vehiclesDeleted }}</el-descriptions-item>
          <el-descriptions-item label="客户资料已删除">{{ clearResult.customersDeleted }}</el-descriptions-item>
        </el-descriptions>
        <el-alert type="success" :closable="false" show-icon style="margin-top: 16px;">
          清理完成。系统状态模型已升级为全新状态机，已可以正式启用！
        </el-alert>
      </el-card>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Loading } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getTrialDataSummary, clearTrialData, cleanStartPreflight } from '@/api/trial-data';
import PageContainer from '@/components/PageContainer.vue';
import type { TrialDataSummaryResponse, ClearTrialDataResponse } from '@/types/trial-data';

const loading = ref(true);
const clearing = ref(false);
const summary = ref<TrialDataSummaryResponse | null>(null);
const clearResult = ref<ClearTrialDataResponse | null>(null);
const confirmInput = ref('');

const isCleanStartReady = ref(true);
const blockReason = ref('');
const hasLegacyWorkOrders = ref(false);

async function loadSummary() {
  loading.value = true;
  isCleanStartReady.value = true;
  blockReason.value = '';
  hasLegacyWorkOrders.value = false;

  try {
    // Try preflight check first
    summary.value = await cleanStartPreflight();
    hasLegacyWorkOrders.value = summary.value.legacyWorkOrderStatusCount > 0;
  } catch (err: any) {
    isCleanStartReady.value = false;
    blockReason.value = err.message || err.response?.data?.message || '检测到旧试运行业务数据，请先执行清理';
    hasLegacyWorkOrders.value = true;

    // Fallback to normal summary so we can display details
    try {
      summary.value = await getTrialDataSummary();
      if (summary.value) {
        hasLegacyWorkOrders.value = summary.value.legacyWorkOrderStatusCount > 0;
      }
    } catch {
      ElMessage.error('加载可清理数据摘要失败');
    }
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
  } catch (err: any) {
    ElMessage.error(err.message || err.response?.data?.message || '清理失败');
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
  line-height: 1.6;
}

.retain-list li {
  margin-bottom: 6px;
  font-size: 13px;
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
